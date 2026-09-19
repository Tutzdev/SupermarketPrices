package br.com.supermercados.prices.collection.mercafacil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import br.com.supermercados.prices.price.StockAvailability;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class MercafacilPageTest {

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final MercafacilStoreDefinition store = new MercafacilStoreDefinition("pame", "Pame", "Pame",
            URI.create("https://www.pamesupermercados.com.br"), "loja-01", "6647c2d35aa7f3f538ab875b", "26185052000173", "Volta Redonda");

    @Test
    void readsActualPublicProductsPaginationAndStoreIdentity() throws Exception {
        MercafacilPage page = page("pame-department");
        assertThat(page.objects("EcommerceProduct")).hasSize(30);
        assertThat(page.pagination().path("records").asInt()).isEqualTo(96);
        assertThat(page.objects("Store")).singleElement().satisfies(identity ->
                assertThat(identity.path("cnpj").asString()).isEqualTo(store.cnpj()));
        var product = new MercafacilProductParser().parse(page.objects("EcommerceProduct").getFirst(), page, store);
        assertThat(product.name()).endsWith("(preço de 1 kg)");
        assertThat(product.regularPrice()).isEqualByComparingTo("25.99");
        assertThat(product.gtin()).isNull();
        assertThat(product.availability()).isEqualTo(StockAvailability.AVAILABLE);
        assertThat(product.imageUrl()).startsWith("https://");
        assertThat(product.originUrl()).endsWith("/produto/m/toucinho-barriga-kg-4178");
    }

    @Test
    void retainsRealPackagingAndDoesNotInventPromotionValidity() throws Exception {
        MercafacilPage page = page("ville-products");
        var product = new MercafacilProductParser().parse(page.objects("EcommerceProduct").get(1), page, store);
        assertThat(product.name()).endsWith("200ml");
        assertThat(product.brand()).isEqualTo("Poliflor");
        assertThat(product.promotionalPrice()).isEqualByComparingTo("8.98");
        assertThat(product.promotionValidUntil()).isNull();
        assertThat(new MercafacilProductParser().parse(page.objects("EcommerceProduct").getFirst(), page, store)
                .imageUrl()).isNull();
    }

    @Test
    void refusesInvalidPagePriceAndUnidentifiedVariants() throws Exception {
        assertThatThrownBy(() -> new MercafacilPage("<html>Indisponível</html>", mapper))
                .hasMessageContaining("reconhecido");
        MercafacilPage page = page("pame-department");
        ObjectNode product = (ObjectNode) page.objects("EcommerceProduct").getFirst().deepCopy();
        product.put("price", 0);
        assertThatThrownBy(() -> new MercafacilProductParser().parse(product, page, store))
                .hasMessageContaining("positivo");
        product.put("price", 10);
        product.putArray("variants").addObject().put("name", "sabores variados");
        assertThatThrownBy(() -> new MercafacilProductParser().parse(product, page, store))
                .hasMessageContaining("individual");
    }

    private MercafacilPage page(String name) throws Exception {
        try (var input = getClass().getResourceAsStream("/collectors/regional/" + name + ".html")) {
            return new MercafacilPage(new String(input.readAllBytes(), StandardCharsets.UTF_8), mapper);
        }
    }
}
