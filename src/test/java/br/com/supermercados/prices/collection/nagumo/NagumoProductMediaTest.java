package br.com.supermercados.prices.collection.nagumo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class NagumoProductMediaTest {

    @Test
    void usesActualPublicImageAndProductUrls() throws Exception {
        var mapper = JsonMapper.builder().build();
        try (var input = getClass().getResourceAsStream("/collectors/regional/nagumo-media.json")) {
            var product = mapper.readTree(input);
            var catalog = new NagumoCatalogResponse(mapper.createObjectNode(), "Bebidas", 1, List.of(product));
            var parsed = new NagumoProductParser(new NagumoProperties()).parse(catalog, Instant.parse("2026-09-20T00:00:00Z"));

            assertThat(parsed.warnings()).isEmpty();
            assertThat(parsed.products().getFirst().imageUrl())
                    .isEqualTo("https://assetsmn.s3.us-east-1.amazonaws.com/assets/ofertas-ecommerce/676519.webp");
            assertThat(parsed.products().getFirst().originUrl()).startsWith("https://www.nagumo.com.br/categoria/");
        }
    }
}
