package br.com.supermercados.prices.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.supermercados.prices.datasource.DataSourceService;
import br.com.supermercados.prices.datasource.SourceObservation;
import br.com.supermercados.prices.datasource.SourceRegistration;
import br.com.supermercados.prices.support.PostgresTestDatabase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;

@SpringBootTest
@ActiveProfiles("test")
class CatalogConcurrencyTests {

    private static final Instant COLLECTED_AT = Instant.parse("2025-01-10T12:00:00Z");

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    DataSourceService sources;

    @Autowired
    ProductIngestionService ingestion;

    @Autowired
    ProductService products;

    @Autowired
    JdbcTemplate jdbc;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @Test
    void concurrentRefreshCannotOverwriteTheObservationCommittedFirst() {
        var transaction = new TransactionTemplate(transactionManager);
        ProductResponse original = transaction.execute(status -> createSyntheticProduct());
        try (EntityManager first = entityManagerFactory.createEntityManager();
                EntityManager second = entityManagerFactory.createEntityManager()) {
            first.getTransaction().begin();
            second.getTransaction().begin();
            try {
                Product firstSnapshot = first.find(Product.class, original.id());
                Product secondSnapshot = second.find(Product.class, original.id());
                firstSnapshot.updateDetails(observation(original.sourceId(), "Synthetic newest observation",
                        COLLECTED_AT.plusSeconds(120)), COLLECTED_AT.plusSeconds(120));
                secondSnapshot.updateDetails(observation(original.sourceId(), "Synthetic older observation",
                        COLLECTED_AT.plusSeconds(60)), COLLECTED_AT.plusSeconds(60));
                first.getTransaction().commit();

                assertThatThrownBy(second::flush).isInstanceOf(OptimisticLockException.class);
                second.getTransaction().rollback();

                ProductResponse persisted = products.findProduct(original.id());
                assertThat(persisted.name()).isEqualTo("Synthetic newest observation");
                assertThat(persisted.collectedAt()).isEqualTo(COLLECTED_AT.plusSeconds(120));
            } finally {
                rollbackIfActive(first);
                rollbackIfActive(second);
            }
        } finally {
            transaction.executeWithoutResult(status -> deleteSyntheticProduct(original));
        }
    }

    private ProductResponse createSyntheticProduct() {
        UUID sourceId = sources.registerVerifiedSource(new SourceRegistration(
                "synthetic-concurrency-" + UUID.randomUUID(), "Synthetic concurrency test source",
                "https://concurrency.example.test", COLLECTED_AT)).getId();
        return ingestion.ingest(observation(sourceId, "Synthetic initial observation", COLLECTED_AT));
    }

    private ProductObservation observation(UUID sourceId, String name, Instant collectedAt) {
        return new ProductObservation(null, name, null, null, "UN", BigDecimal.ONE, null,
                new SourceObservation(sourceId, "synthetic-concurrency-item", collectedAt));
    }

    private void deleteSyntheticProduct(ProductResponse product) {
        jdbc.update("delete from product_source_references where product_id = ?", product.id());
        jdbc.update("delete from products where id = ?", product.id());
        jdbc.update("delete from data_sources where id = ?", product.sourceId());
    }

    private void rollbackIfActive(EntityManager entityManager) {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
    }
}
