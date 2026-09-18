package br.com.supermercados.prices.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.supermercados.prices.datasource.DataSource;
import br.com.supermercados.prices.datasource.DataSourceService;
import br.com.supermercados.prices.datasource.SourceObservation;
import tools.jackson.databind.json.JsonMapper;

class VerifiedProductMappingsTest {

    private final DataSourceService sources = mock(DataSourceService.class);
    private final ProductSourceReferenceRepository references = mock(ProductSourceReferenceRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final UUID royalId = UUID.randomUUID();
    private final UUID nagumoId = UUID.randomUUID();
    private VerifiedProductMappings mappings;

    @BeforeEach
    void setUp() throws Exception {
        mappings = new VerifiedProductMappings(JsonMapper.builder().build(), sources, references, products);
        DataSource royal = mock(DataSource.class);
        when(royal.getCode()).thenReturn("royal_delivery");
        when(sources.requireSource(royalId)).thenReturn(royal);
        DataSource nagumo = mock(DataSource.class);
        when(nagumo.getCode()).thenReturn("nagumo_delivery");
        when(nagumo.getId()).thenReturn(nagumoId);
        when(sources.requireSource(nagumoId)).thenReturn(nagumo);
        when(sources.findByCode("nagumo_delivery")).thenReturn(Optional.of(nagumo));
    }

    @Test
    void linksOnlyTheReviewedSourceIdentifiersAndPackage() {
        ProductObservation nagumo = observation(nagumoId, "nagumo:product:257207", "Lentilha Yoki 400G", null);
        Product product = new Product(nagumo, null, Instant.now());
        when(references.findBySourceIdAndSourceReference(nagumoId, nagumo.source().sourceReference()))
                .thenReturn(Optional.of(new ProductSourceReference(product.getId(), nagumo.source())));
        when(products.findById(product.getId())).thenReturn(Optional.of(product));

        assertThat(mappings.findVerifiedProduct(observation(royalId, "royal:product:7947",
                "Lentilha Yoki 400g", "7891095911318"))).contains(product);
    }

    @Test
    void rejectsChangedPackageEvenWithThePreviouslyVerifiedSourceId() {
        assertThatThrownBy(() -> mappings.findVerifiedProduct(observation(royalId, "royal:product:7947",
                "Lentilha Yoki 500g", "7891095911318"))).hasMessageContaining("nova revisão");
    }

    @Test
    void rejectsChangedOrMissingGtinOnTheVerifiedRoyalProduct() {
        assertThatThrownBy(() -> mappings.findVerifiedProduct(observation(royalId, "royal:product:7947",
                "Lentilha Yoki 400g", "7894900027013"))).hasMessageContaining("nova revisão");
        assertThatThrownBy(() -> mappings.findVerifiedProduct(observation(royalId, "royal:product:7947",
                "Lentilha Yoki 400g", null))).hasMessageContaining("nova revisão");
    }

    @Test
    void doesNotLinkAnUnreviewedIdentifierByTheSameName() {
        assertThat(mappings.findVerifiedProduct(observation(royalId, "royal:product:unreviewed",
                "Lentilha Yoki 400g", null))).isEmpty();
    }

    private ProductObservation observation(UUID sourceId, String reference, String name, String gtin) {
        return new ProductObservation(gtin, name, null, null, null, null, null,
                new SourceObservation(sourceId, reference, Instant.now()));
    }
}
