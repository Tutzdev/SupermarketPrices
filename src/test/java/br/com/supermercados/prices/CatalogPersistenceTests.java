package br.com.supermercados.prices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import br.com.supermercados.prices.common.ApiException;
import br.com.supermercados.prices.datasource.DataSourceService;
import br.com.supermercados.prices.datasource.SourceObservation;
import br.com.supermercados.prices.datasource.SourceRegistration;
import br.com.supermercados.prices.product.ProductIngestionService;
import br.com.supermercados.prices.product.ProductObservation;
import br.com.supermercados.prices.product.ProductResponse;
import br.com.supermercados.prices.product.ProductSearch;
import br.com.supermercados.prices.product.ProductService;
import br.com.supermercados.prices.support.PostgresTestDatabase;
import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Sql("/fixtures/catalog.sql")
class CatalogPersistenceTests {

    private static final UUID SOURCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final Instant COLLECTED_AT = Instant.parse("2025-01-10T12:00:00Z");
    private static final PageRequest PAGE = PageRequest.of(0, 20, Sort.by("name", "id"));

    @Autowired
    ProductService products;

    @Autowired
    ProductIngestionService ingestion;

    @Autowired
    DataSourceService sources;

    @Autowired
    EntityManager entityManager;

    @Autowired
    JdbcTemplate jdbc;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @Test
    void textSearchIgnoresCaseAndRespectsPagination() {
        var result = products.search(new ProductSearch("aLpHa", null, null, null), PAGE);

        assertThat(result.getContent()).extracting(ProductResponse::name).containsExactly("Produto FICTÍCIO Alpha");
        assertThat(result.getTotalElements()).isEqualTo(1);
        var paginated = products.search(new ProductSearch(null, null, null, null), PageRequest.of(0, 1));
        assertThat(paginated.getNumberOfElements()).isEqualTo(1);
        assertThat(paginated.getTotalElements()).isEqualTo(2);
    }

    @Test
    void treatsSqlWildcardAndEscapeCharactersAsLiteralSearchText() {
        var literal = ingestion.ingest(observation("SYNTHETIC 50%_! item", null, SOURCE_ID, "literal-item", COLLECTED_AT));
        ingestion.ingest(observation("SYNTHETIC 50abc item", null, SOURCE_ID, "wildcard-decoy", COLLECTED_AT));
        flushAndClear();

        for (String query : new String[] {"%", "_", "!", "50%_!"}) {
            var result = products.search(new ProductSearch(query, null, null, null), PAGE);
            assertThat(result.getContent()).extracting(ProductResponse::id).containsExactly(literal.id());
        }
    }

    @Test
    void combinesCaseInsensitiveBrandCategoryAndCanonicalGtinFilters() {
        var created = ingestion.ingest(observation("Synthetic filtered item", "0000000000017",
                SOURCE_ID, "filtered-item", COLLECTED_AT));
        flushAndClear();

        var matching = products.search(
                new ProductSearch("FILTERED", "sYnThEtIc bRaNd", "00000000000017", "synthetic category"), PAGE);
        var differentBrand = products.search(
                new ProductSearch(null, "Another synthetic brand", "0000000000017", null), PAGE);

        assertThat(matching.getContent()).extracting(ProductResponse::id).containsExactly(created.id());
        assertThat(differentBrand).isEmpty();
    }

    @Test
    void sharesIdentityAcrossSourcesOnlyWithTheSameValidatedGtin() {
        UUID otherSource = registerOtherSource();
        var first = ingestion.ingest(observation("Synthetic name at first source", "0000000000017",
                SOURCE_ID, "first-source-item", COLLECTED_AT));
        flushAndClear();

        var second = ingestion.ingest(observation("Different synthetic name at second source", "00000000000017",
                otherSource, "second-source-item", COLLECTED_AT.plusSeconds(60)));
        flushAndClear();

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(jdbc.queryForObject("select count(*) from products where gtin = ?", Integer.class,
                "00000000000017")).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from product_source_references where product_id = ?",
                Integer.class, first.id())).isEqualTo(2);
        assertThat(products.findProduct(first.id()).sourceId()).isEqualTo(SOURCE_ID);
    }

    @Test
    void identicalNamesWithoutGtinRemainSeparateProducts() {
        UUID otherSource = registerOtherSource();
        var first = ingestion.ingest(observation("Same synthetic name", null, SOURCE_ID, "same-name-a", COLLECTED_AT));
        var second = ingestion.ingest(observation("Same synthetic name", null, otherSource, "same-name-b", COLLECTED_AT));
        flushAndClear();

        assertThat(second.id()).isNotEqualTo(first.id());
        assertThat(products.search(new ProductSearch("Same synthetic name", null, null, null), PAGE)
                .getTotalElements()).isEqualTo(2);
    }

    @Test
    void updatesVerifiedReferenceOnlyWhenObservationIsNewer() {
        var first = ingestion.ingest(observation("Synthetic initial name", null, SOURCE_ID, "temporal-item", COLLECTED_AT));
        ingestion.ingest(observation("Synthetic latest name", null, SOURCE_ID,
                "temporal-item", COLLECTED_AT.plusSeconds(60)));
        flushAndClear();
        ingestion.ingest(observation("Synthetic stale name", null, SOURCE_ID,
                "temporal-item", COLLECTED_AT.minusSeconds(60)));
        flushAndClear();

        var persisted = products.findProduct(first.id());
        assertThat(persisted.name()).isEqualTo("Synthetic latest name");
        assertThat(persisted.collectedAt()).isEqualTo(COLLECTED_AT.plusSeconds(60));
    }

    @Test
    void rejectsConflictingGtinForExistingSourceReference() {
        ingestion.ingest(observation("Synthetic identified item", "0000000000017",
                SOURCE_ID, "conflicting-item", COLLECTED_AT));
        flushAndClear();

        assertThatThrownBy(() -> ingestion.ingest(observation("Synthetic conflicting item", "0000000000024",
                SOURCE_ID, "conflicting-item", COLLECTED_AT.plusSeconds(60))))
                .isInstanceOf(ApiException.class).hasMessageContaining("Identidade");
    }

    private UUID registerOtherSource() {
        return sources.registerVerifiedSource(new SourceRegistration("synthetic-secondary", "Synthetic test source",
                "https://secondary.example.test", COLLECTED_AT)).getId();
    }

    private ProductObservation observation(String name, String gtin, UUID sourceId, String reference, Instant collectedAt) {
        return new ProductObservation(gtin, name, "Synthetic Brand", "Synthetic test description", "UN",
                BigDecimal.ONE, "Synthetic Category", new SourceObservation(sourceId, reference, collectedAt));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
