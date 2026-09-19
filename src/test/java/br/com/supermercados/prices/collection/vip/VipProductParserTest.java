package br.com.supermercados.prices.collection.vip;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class VipProductParserTest {

    @ParameterizedTest
    @ValueSource(strings = {"bramil-24-coca", "perola-1-coca", "spani-6-coca"})
    void parsesObservedRegionalCatalogsWithoutReusingAnotherStoresIdentity(String fixture) throws Exception {
        try (var input = getClass().getResourceAsStream("/collectors/regional/" + fixture + ".json")) {
            JsonNode response = JsonMapper.builder().build().readTree(input);
            var entries = new ArrayList<JsonNode>();
            response.path("data").forEach(entries::add);
            var parsed = new VipProductParser().parse(entries, fixture, fixture);
            assertThat(parsed.warnings()).isEmpty();
            assertThat(parsed.products()).hasSize(3).allSatisfy(product -> {
                assertThat(product.sourceReference()).startsWith(fixture + ":product:");
                assertThat(product.regularPrice()).isPositive();
                assertThat(product.name()).isNotBlank();
                assertThat(product.imageUrl()).startsWith("https://produto-assets-vipcommerce-com-br.br-se1.magaluobjects.com/250x250/");
                assertThat(product.promotionValidUntil()).isNull();
            });
        }
    }
}
