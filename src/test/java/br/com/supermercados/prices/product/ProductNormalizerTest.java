package br.com.supermercados.prices.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.supermercados.prices.datasource.SourceObservation;

class ProductNormalizerTest {

    @Test
    void normalizesTextAndExtractsMetricPackageDetails() {
        ProductNormalizer.NormalizedProduct product = normalize(
                "Coca-Cola Original Pet 2 Litros", "Coca-Cola");

        assertThat(product.name()).isEqualTo("COCA COLA ORIGINAL PET 2 LITROS");
        assertThat(product.brand()).isEqualTo("COCA COLA");
        assertThat(product.quantity()).isEqualByComparingTo("2000");
        assertThat(product.unit()).isEqualTo("ML");
        assertThat(product.packageDescription()).isEqualTo("2 L");
    }

    @Test
    void preservesVariationTermsAndDifferentiatesPackageSizes() {
        ProductNormalizer.NormalizedProduct regular = normalize("Refrigerante Cola 2L", "Marca");
        ProductNormalizer.NormalizedProduct zero = normalize("Refrigerante Cola Zero 1L", "Marca");

        assertThat(regular.name()).doesNotContain("ZERO");
        assertThat(zero.name()).contains("ZERO");
        assertThat(regular.quantity()).isEqualByComparingTo("2000");
        assertThat(zero.quantity()).isEqualByComparingTo("1000");
    }

    @Test
    void identifiesMultipackWithoutFlatteningItsUnitCountIntoWeight() {
        ProductNormalizer.NormalizedProduct product = normalize(
                "Papel Higiênico Folha Dupla 30M 16Un", "Nagumo");

        assertThat(product.quantity()).isEqualByComparingTo("16");
        assertThat(product.unit()).isEqualTo("UN");
        assertThat(product.packageDescription()).isEqualTo("16 X 30 M");
    }

    private ProductNormalizer.NormalizedProduct normalize(String name, String brand) {
        ProductObservation observation = new ProductObservation(
                null, name, brand, null, null, null, null,
                new SourceObservation(UUID.randomUUID(), "synthetic", Instant.EPOCH));
        return ProductNormalizer.normalize(observation);
    }
}
