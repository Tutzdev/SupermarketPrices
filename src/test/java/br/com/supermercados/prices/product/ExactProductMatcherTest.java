package br.com.supermercados.prices.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.supermercados.prices.datasource.SourceObservation;

class ExactProductMatcherTest {

    private final ProductRepository products = mock(ProductRepository.class);
    private final ExactProductMatcher matcher = new ExactProductMatcher(products);

    @Test
    void linksOnlyUniqueExactBrandVariantAndEquivalentVolume() {
        Product original = new Product(observation("Coca-Cola Original 1000ml", "Coca-Cola"), "07894900011517", Instant.EPOCH);
        when(products.findByNormalizedBrandAndUnitAndQuantity(any(), any(), any(), any())).thenReturn(List.of(original));
        assertThat(matcher.find(observation("Refrigerante Coca Cola Original 1 litro", "Coca Cola"), null))
                .contains(original);
        assertThat(matcher.find(observation("Coca Cola Zero 1L", "Coca Cola"), null)).isEmpty();
        assertThat(matcher.find(observation("Coca Cola Original PET 1L", "Coca Cola"), null)).isEmpty();
        assertThat(matcher.find(observation("Coca Cola Original 1L", "Coca Cola"), "07894900011500")).isEmpty();
    }

    @Test
    void leavesAmbiguousCandidatesAndIncompleteIdentitySeparate() {
        Product first = new Product(observation("Coca-Cola Original 1L", "Coca-Cola"), null, Instant.EPOCH);
        Product second = new Product(observation("Coca-Cola Original 1L", "Coca-Cola"), null, Instant.EPOCH);
        assertThat(matcher.find(observation("Coca Cola Original 1L", null), null)).isEmpty();
        assertThat(matcher.find(observation("Coca Cola", "Coca Cola"), null)).isEmpty();
        verifyNoInteractions(products);
        when(products.findByNormalizedBrandAndUnitAndQuantity(any(), any(), any(), any())).thenReturn(List.of(first, second));
        assertThat(matcher.find(observation("Coca-Cola Original 1 litro", "Coca-Cola"), null)).isEmpty();
    }

    private ProductObservation observation(String name, String brand) {
        return new ProductObservation(null, name, brand, null, null, null, null,
                new SourceObservation(UUID.randomUUID(), "synthetic-contract-test", Instant.EPOCH));
    }
}
