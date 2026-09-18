package br.com.supermercados.prices.collection.royal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.com.supermercados.prices.price.StockAvailability;
import br.com.supermercados.prices.support.CollectorFixtureServer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class RoyalCollectorTest {

    private CollectorFixtureServer server;
    private RoyalProperties properties;

    @BeforeEach
    void setUp() throws Exception {
        server = new CollectorFixtureServer();
        properties = new RoyalProperties();
        properties.setBaseUrl(server.baseUrl());
        properties.setApiUrl(server.baseUrl());
        properties.setPublicConfigurationPath("/public-config.js");
        properties.setSearchTerms(List.of("coca"));
        properties.setRequestDelay(Duration.ZERO);
    }

    @AfterEach
    void close() {
        server.close();
    }

    @Test
    void validatesPublicSessionExactRetiroAndEveryPageFromObservedResponses() {
        var catalog = collector().collect();
        assertThat(server.receivedPublicAuthorization).isTrue();
        assertThat(server.catalogRequests).hasSize(2);
        assertThat(catalog.products()).hasSize(28);
        assertThat(catalog.store().name()).isEqualTo("Royal Retiro");
        assertThat(catalog.products()).anySatisfy(product -> {
            assertThat(product.sourceReference()).isEqualTo("royal:product:7310");
            assertThat(product.gtin()).isEqualTo("07894900701524");
            assertThat(product.regularPrice()).isPositive();
        });
        assertThat(catalog.products()).anySatisfy(product -> {
            assertThat(product.sourceReference()).isEqualTo("royal:product:3400");
            assertThat(product.gtin()).isNull();
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"nome", "cnpj", "id", "vipcommerce_centro_distribuicao_id"})
    void refusesChangedStoreIdentityBeforeRequestingCatalog(String field) {
        ((ObjectNode) server.royalStore.path("data")).put(field, "alterado");
        assertThatThrownBy(() -> collector().collect()).hasMessageContaining("Identidade inesperada");
        assertThat(server.catalogRequests).isEmpty();
    }

    @Test
    void fullCatalogVisitsDepartmentPagesInsteadOfRestrictingSearchTerms() {
        properties.setFullCatalog(true);
        properties.setSearchTerms(List.of());

        var catalog = collector().collect();

        assertThat(catalog.products()).hasSize(28);
        assertThat(server.catalogRequests).hasSize(2).allSatisfy(path ->
                assertThat(path).contains("/departamentos/5/produtos?page="));
    }

    @Test
    void refusesWrongAddressAndMissingPickup() {
        ((ObjectNode) server.royalStore.path("data").path("endereco")).put("bairro", "Aterrado");
        assertThatThrownBy(() -> collector().collect()).hasMessageContaining("bairro");
        server.royalPickups.putArray("data");
        assertThatThrownBy(() -> collector().collect()).hasMessageContaining("ausente");
        assertThat(server.catalogRequests).isEmpty();
    }

    @Test
    void handlesSuccessfulEmptyCatalogSeparatelyFromExternalFailure() {
        server.emptyCatalog = true;
        var catalog = collector().collect();
        assertThat(catalog.products()).isEmpty();
        assertThat(catalog.warnings()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 403, 404, 500, 503})
    void propagatesHttpFailures(int status) {
        server.failurePath = "/api-admin/v1/org/255/loja/centros_distribuicoes/1";
        server.failureStatus = status;
        server.failuresRemaining = 10;
        assertThatThrownBy(() -> collector().collect()).hasMessageContaining("HTTP " + status);
    }

    @Test
    void retriesTransientFailureAndRejectsInvalidJsonAndTimeout() {
        server.failurePath = "/api-admin/v1/org/255/loja/centros_distribuicoes/1";
        server.failuresRemaining = 1;
        assertThat(collector().collect().products()).hasSize(28);
        server.invalidJson = true;
        assertThatThrownBy(() -> collector().collect()).hasMessageContaining("JSON inválido");
        server.invalidJson = false;
        server.delayMillis = 300;
        properties.setRequestTimeout(Duration.ofMillis(50));
        assertThatThrownBy(() -> collector().collect()).hasMessageContaining("comunicação");
    }

    @Test
    void parserPreservesUnknownStockMissingGtinConditionalPromotionAndDuplicate() throws Exception {
        ObjectNode product = (ObjectNode) server.fixture("royal-yoki-1.json").path("data").path("produtos").get(0);
        product.put("disponivel", false);
        var parser = new RoyalProductParser();
        var unavailable = parser.parse(List.of(product)).products().getFirst();
        assertThat(unavailable.availability()).isEqualTo(StockAvailability.UNAVAILABLE);
        product.remove("disponivel");
        assertThat(parser.parse(List.of(product)).products().getFirst().availability()).isEqualTo(StockAvailability.UNKNOWN);
        product.putNull("codigo_barras");
        assertThat(parser.parse(List.of(product)).products().getFirst().gtin()).isNull();
        List<JsonNode> duplicates = List.of(product, product.deepCopy());
        assertThat(parser.parse(duplicates).products()).hasSize(1);
        product.remove("preco");
        assertThat(parser.parse(List.of(product)).warnings()).singleElement().asString().contains("preço");

        List<JsonNode> entries = new ArrayList<>();
        server.fixture("royal-yoki-1.json").path("data").path("produtos").forEach(entries::add);
        ObjectNode offered = (ObjectNode) entries.stream().filter(node -> node.path("em_oferta").asBoolean())
                .findFirst().orElseThrow();
        assertThat(parser.parse(List.of(offered)).products().getFirst().promotionalPrice()).isPositive();
        ((ObjectNode) offered.path("oferta")).put("quantidade_minima", 2);
        var conditional = parser.parse(List.of(offered)).products().getFirst();
        assertThat(conditional.promotionCondition()).contains("quantidade mínima 2");
        assertThat(conditional.promotionValidUntil()).isNull();
    }

    private RoyalCollector collector() {
        return new RoyalCollector(properties, new RoyalClient(properties, JsonMapper.builder().build()),
                new RoyalProductParser(), Clock.systemUTC());
    }
}
