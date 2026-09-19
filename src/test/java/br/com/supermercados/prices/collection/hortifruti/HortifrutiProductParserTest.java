package br.com.supermercados.prices.collection.hortifruti;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class HortifrutiProductParserTest {

    @Test
    void readsRealOfferFromTheVerifiedLocalSeller() throws Exception {
        var result = new HortifrutiProductParser().parse(product());
        assertThat(result.gtin()).isEqualTo("07894900701753");
        assertThat(result.regularPrice()).isEqualByComparingTo("8.39");
        assertThat(result.imageUrl()).startsWith("https://hortifrutibr.vtexassets.com/");
        assertThat(result.originUrl()).contains("hortifruti.com.br/");
        assertThat(result.description()).doesNotContain("<span>");
        assertThat(result.promotionalPrice()).isNull();
        assertThat(result.promotionValidUntil()).isNull();
    }

    @Test
    void rejectsDefaultSellerAndUnverifiedWeightMultiplier() throws Exception {
        JsonNode product = product();
        ((ObjectNode) product.path("offers").path("offers").path(0).path("seller")).put("identifier", "1");
        assertThatThrownBy(() -> new HortifrutiProductParser().parse(product)).hasMessageContaining("vendedor local");
        JsonNode weighted = product();
        ((ObjectNode) weighted).put("unitMultiplier", 0.2);
        assertThatThrownBy(() -> new HortifrutiProductParser().parse(weighted)).hasMessageContaining("Peso variável");
    }

    private JsonNode product() throws Exception {
        try (var input = getClass().getResourceAsStream("/collectors/regional/hortifruti-aterrado.json")) {
            return JsonMapper.builder().build().readTree(input).path("data").path("search").path("products").path("edges").path(0).path("node");
        }
    }

    @Test
    void keepsMissingExpiryUnknownAndRejectsMalformedDates() throws Exception {
        JsonNode product = product();
        ObjectNode offer = (ObjectNode) product.path("offers").path("offers").path(0);
        offer.put("priceValidUntil", "");
        assertThat(new HortifrutiProductParser().parse(product).validUntil()).isNull();
        offer.putNull("priceValidUntil");
        assertThat(new HortifrutiProductParser().parse(product).validUntil()).isNull();
        offer.put("priceValidUntil", "invalid-date");
        assertThatThrownBy(() -> new HortifrutiProductParser().parse(product))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Validade");
    }
}
