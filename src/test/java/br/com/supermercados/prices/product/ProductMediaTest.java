package br.com.supermercados.prices.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.supermercados.prices.datasource.SourceObservation;

class ProductMediaTest {

    @Test
    void encodesSpacesInPublicImagePathsWithoutRejectingTheProduct() {
        var observation = new ProductObservation(null, "Produto 100g", null, null, null, null, null,
                new SourceObservation(UUID.randomUUID(), "synthetic", Instant.EPOCH),
                "https://example.com/images/Product Image.jpg?v=1", "https://example.com/product");
        var product = new Product(observation, null, Instant.EPOCH);
        assertThat(product.getImageUrl()).isEqualTo("https://example.com/images/Product%20Image.jpg?v=1");
        assertThat(product.getOriginUrl()).isEqualTo("https://example.com/product");
    }
}
