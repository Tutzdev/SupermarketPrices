package br.com.supermercados.prices.contribution;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.supermercados.prices.common.ApiException;
import br.com.supermercados.prices.support.PostgresTestDatabase;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContributionConcurrencyTests {

    private static final UUID SOURCE_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID CHAIN_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID STORE_ID = UUID.fromString("10000000-0000-0000-0000-000000000003");
    private static final UUID PRODUCT_ID = UUID.fromString("10000000-0000-0000-0000-000000000004");
    private static final UUID CONTRIBUTOR_ID = UUID.fromString("10000000-0000-0000-0000-000000000005");
    private static final UUID MODERATOR_ID = UUID.fromString("10000000-0000-0000-0000-000000000006");

    @Autowired private PriceContributionService contributions;
    @Autowired private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
        registry.add("app.contributions.source-id", SOURCE_ID::toString);
    }

    @BeforeAll
    void createCatalogAndUsers() {
        UUID cityId = jdbc.queryForObject(
                "select id from cities where name = 'Rio de Janeiro'", UUID.class);
        jdbc.update("""
                insert into data_sources (id, code, name, base_url, enabled, verified_at, created_at)
                values (?, 'concurrency_test', 'Concurrency test source', 'https://source.example.test',
                        true, current_timestamp, current_timestamp)
                """, SOURCE_ID);
        jdbc.update("""
                insert into supermarket_chains
                    (id, name, source_id, source_reference, collected_at, updated_at)
                values (?, 'Concurrency test chain', ?, 'chain', current_timestamp, current_timestamp)
                """, CHAIN_ID, SOURCE_ID);
        jdbc.update("""
                insert into stores
                    (id, supermarket_chain_id, city_id, name, active, source_id,
                     source_reference, collected_at, updated_at)
                values (?, ?, ?, 'Concurrency test store', true, ?, 'store',
                        current_timestamp, current_timestamp)
                """, STORE_ID, CHAIN_ID, cityId, SOURCE_ID);
        jdbc.update("""
                insert into products
                    (id, name, source_id, source_reference, collected_at, updated_at)
                values (?, 'Concurrency test product', ?, 'product', current_timestamp, current_timestamp)
                """, PRODUCT_ID, SOURCE_ID);
        jdbc.update("""
                insert into product_source_references
                    (id, product_id, source_id, source_reference, collected_at)
                values (?, ?, ?, 'product', current_timestamp)
                """, UUID.randomUUID(), PRODUCT_ID, SOURCE_ID);
        insertUser(CONTRIBUTOR_ID, "contributor-concurrency@example.test", "USER");
        insertUser(MODERATOR_ID, "moderator-concurrency@example.test", "ADMIN");
    }

    @AfterAll
    void removeCatalogAndUsers() {
        jdbc.update("delete from admin_audit_entries where actor_user_id = ?", MODERATOR_ID);
        jdbc.update("""
                update price_contributions
                set status = 'PENDING', moderator_id = null, decided_at = null,
                    rejection_reason = null, price_record_id = null
                where contributor_id = ?
                """, CONTRIBUTOR_ID);
        jdbc.update("delete from price_records where contribution_id in "
                + "(select id from price_contributions where contributor_id = ?)", CONTRIBUTOR_ID);
        jdbc.update("delete from price_contributions where contributor_id = ?", CONTRIBUTOR_ID);
        jdbc.update("delete from product_source_references where product_id = ?", PRODUCT_ID);
        jdbc.update("delete from products where id = ?", PRODUCT_ID);
        jdbc.update("delete from stores where id = ?", STORE_ID);
        jdbc.update("delete from supermarket_chains where id = ?", CHAIN_ID);
        jdbc.update("delete from data_sources where id = ?", SOURCE_ID);
        jdbc.update("delete from app_users where id in (?, ?)", CONTRIBUTOR_ID, MODERATOR_ID);
    }

    @Test
    void simultaneousApprovalAndRejectionCannotCreateContradictoryState() throws Exception {
        UUID contributionId = insertContribution();
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<Throwable>> decisions = List.of(
                () -> decide(start, () -> contributions.approve(MODERATOR_ID, contributionId)),
                () -> decide(start, () -> contributions.reject(MODERATOR_ID, contributionId,
                        new RejectContributionRequest("Evidence could not be confirmed"))));

        List<Throwable> outcomes;
        try (var executor = Executors.newFixedThreadPool(2)) {
            var futures = decisions.stream().map(executor::submit).toList();
            start.countDown();
            outcomes = futures.stream().map(future -> {
                try {
                    return future.get();
                } catch (Exception exception) {
                    throw new AssertionError(exception);
                }
            }).toList();
        }

        assertThat(outcomes).filteredOn(java.util.Objects::isNull).hasSize(1);
        assertThat(outcomes).filteredOn(ApiException.class::isInstance).hasSize(1);
        String status = jdbc.queryForObject(
                "select status from price_contributions where id = ?", String.class, contributionId);
        Integer priceCount = jdbc.queryForObject(
                "select count(*) from price_records where contribution_id = ?", Integer.class, contributionId);
        assertThat(status).isIn("APPROVED", "REJECTED");
        assertThat(priceCount).isEqualTo("APPROVED".equals(status) ? 1 : 0);
    }

    @Test
    void repeatedApprovalKeepsSinglePublishedObservation() {
        UUID contributionId = insertContribution();

        PriceContributionResponse first = contributions.approve(MODERATOR_ID, contributionId);
        PriceContributionResponse replay = contributions.approve(MODERATOR_ID, contributionId);

        assertThat(replay.priceRecordId()).isEqualTo(first.priceRecordId());
        Integer priceCount = jdbc.queryForObject(
                "select count(*) from price_records where contribution_id = ?", Integer.class, contributionId);
        assertThat(priceCount).isEqualTo(1);
    }

    private Throwable decide(CountDownLatch start, Runnable decision) throws InterruptedException {
        start.await();
        try {
            decision.run();
            return null;
        } catch (Throwable throwable) {
            return throwable;
        }
    }

    private UUID insertContribution() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into price_contributions (
                    id, contributor_id, product_id, store_id, regular_price, currency,
                    observed_at, submitted_at, availability, status
                ) values (?, ?, ?, ?, 10.00, 'BRL', current_timestamp - interval '1 minute',
                        current_timestamp, 'UNKNOWN', 'PENDING')
                """, id, CONTRIBUTOR_ID, PRODUCT_ID, STORE_ID);
        return id;
    }

    private void insertUser(UUID id, String email, String role) {
        jdbc.update("""
                insert into app_users (
                    id, name, email, password_hash, role, email_verified_at, created_at, updated_at
                ) values (?, 'Concurrency test user', ?, 'not-used-by-test', ?, current_timestamp,
                        current_timestamp, current_timestamp)
                """, id, email, role);
    }
}
