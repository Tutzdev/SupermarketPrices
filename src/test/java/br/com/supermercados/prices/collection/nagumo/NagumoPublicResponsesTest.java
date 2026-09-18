package br.com.supermercados.prices.collection.nagumo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.com.supermercados.prices.price.StockAvailability;
import br.com.supermercados.prices.support.CollectorFixtureServer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class NagumoPublicResponsesTest {

    private static final String STORES = "/on/demandware.store/Sites-Nagumo-Site/pt_BR/Stores-AvailableStores";
    private CollectorFixtureServer server;
    private NagumoProperties properties;

    @BeforeEach
    void setUp() throws Exception {
        server = new CollectorFixtureServer();
        properties = new NagumoProperties();
        properties.setBaseUrl(server.baseUrl());
        properties.setRequestDelay(Duration.ZERO);
    }

    @AfterEach
    void close() {
        server.close();
    }

    @Test
    void continuesPastAShortPageWhenTheSourceStillAdvertisesMoreProducts() {
        server.shortNagumoFirstPage = true;

        var catalog = collector().collect();

        assertThat(server.catalogRequests).hasSize(3);
        assertThat(catalog.products()).hasSize(128);
    }

    @Test
    void publicSessionSelectsExactStorePaginatesAndPreservesMemberPrices() {
        var catalog = collector().collect();
        assertThat(server.receivedSession).isTrue();
        assertThat(server.catalogRequests).hasSize(3);
        assertThat(catalog.foundCount()).isEqualTo(129);
        assertThat(catalog.products()).hasSize(129).allSatisfy(product -> assertThat(product.gtin()).isNull());
        assertThat(catalog.products()).anySatisfy(product -> {
            assertThat(product.sourceReference()).isEqualTo("nagumo:product:266016");
            assertThat(product.regularPrice()).isEqualByComparingTo("29.98");
            assertThat(product.promotionalPrice()).isEqualByComparingTo("19.98");
            assertThat(product.promotionCondition()).contains("Meu Nagumo");
            assertThat(product.promotionValidUntil()).isNull();
        });
        assertThat(catalog.products()).anySatisfy(product ->
                assertThat(product.availability()).isEqualTo(StockAvailability.UNAVAILABLE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"storeName", "address", "city", "district"})
    void rejectsChangedBranchBeforeCatalog(String field) {
        ((ObjectNode) server.nagumoStores.path("stores").get(0)).put(field, "alterado");
        assertThatThrownBy(() -> collector().collect()).hasMessageContaining("Identidade inesperada");
        assertThat(server.catalogRequests).isEmpty();
    }

    @Test
    void rejectsMissingStoreAndUnconfirmedSelection() {
        server.rejectSelection = true;
        assertThatThrownBy(() -> collector().collect()).hasMessageContaining("seleção");
        server.nagumoStores.putArray("stores");
        assertThatThrownBy(() -> collector().collect()).hasMessageContaining("não está disponível");
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 403, 404, 500, 503})
    void doesNotHideHttpFailuresAsAnEmptyCatalog(int status) {
        server.failurePath = STORES;
        server.failureStatus = status;
        server.failuresRemaining = 10;
        assertThatThrownBy(() -> collector().collect()).hasMessageContaining("HTTP " + status);
    }

    @Test
    void retriesTransientFailureRejectsMalformedJsonAndTimesOut() {
        server.failurePath = STORES;
        server.failuresRemaining = 1;
        assertThat(collector().collect().products()).hasSize(129);
        server.invalidJson = true;
        assertThatThrownBy(() -> collector().collect()).hasMessageContaining("JSON inválido");
        server.invalidJson = false;
        server.delayMillis = 300;
        properties.setRequestTimeout(Duration.ofMillis(50));
        assertThatThrownBy(() -> collector().collect()).hasMessageContaining("comunicação");
    }

    @Test
    void successfulZeroCountIsAnEmptyCatalog() {
        server.emptyCatalog = true;
        var catalog = collector().collect();
        assertThat(catalog.products()).isEmpty();
        assertThat(catalog.warnings()).isEmpty();
    }

    @Test
    void weighedProductsExposePricePerKilogramOnlyWhenPortionCalculationAgrees() throws Exception {
        ObjectNode product = (ObjectNode) server.fixture("nagumo-MP-GERAL-0.json")
                .path("productsSearchResult").get(0);
        product.put("weighable", true).put("averageWeightNumber", 0.2);
        product.remove("flagtypes");
        ((ObjectNode) product.path("price")).putNull("list");
        ((ObjectNode) product.path("price").path("sales")).put("value", 6.98);
        ((ObjectNode) product.path("price")).put("quantityValue", 1.40);
        var parser = new NagumoProductParser(properties);
        var catalog = new NagumoCatalogResponse(null, "Hortifruti", 1, List.of(product));

        var parsed = parser.parse(catalog, null);

        assertThat(parsed.products()).singleElement().satisfies(item -> {
            assertThat(item.name()).endsWith("(preço de 1 kg)");
            assertThat(item.regularPrice()).isEqualByComparingTo("6.98");
        });
        ((ObjectNode) product.path("price")).put("quantityValue", 9.99);
        assertThat(parser.parse(catalog, null).products()).isEmpty();
        assertThat(parser.parse(catalog, null).warnings()).singleElement().asString().contains("peso");
        product.putNull("averageWeightNumber");
        assertThat(parser.parse(catalog, null).warnings()).singleElement().asString().contains("peso médio");
    }

    @Test
    void parserReportsMissingPriceDeduplicatesAndPreservesUnknownAvailability() throws Exception {
        ObjectNode product = (ObjectNode) server.fixture("nagumo-MP-GERAL-0.json")
                .path("productsSearchResult").get(0);
        product.remove("available");
        var parser = new NagumoProductParser(properties);
        var parsed = parser.parse(new NagumoCatalogResponse(null, "Produtos Nagumo", 2,
                List.of(product, product.deepCopy())), null);
        assertThat(parsed.products()).singleElement().satisfies(item ->
                assertThat(item.availability()).isEqualTo(StockAvailability.UNKNOWN));
        assertThat(parsed.warnings()).singleElement().asString().contains("duplicado");
        product.remove("price");
        assertThat(parser.parse(new NagumoCatalogResponse(null, "Produtos Nagumo", 1,
                List.of(product)), null).warnings()).singleElement().asString().contains("preço normal");
    }

    private NagumoCollector collector() {
        return new NagumoCollector(properties, new NagumoClient(properties, JsonMapper.builder().build()),
                new NagumoProductParser(properties), Clock.systemUTC());
    }
}
