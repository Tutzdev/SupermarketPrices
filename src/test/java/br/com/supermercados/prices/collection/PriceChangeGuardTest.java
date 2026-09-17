package br.com.supermercados.prices.collection;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.supermercados.prices.price.PriceFixtures;
import br.com.supermercados.prices.price.PriceRecord;

class PriceChangeGuardTest {

    private final PriceChangeGuard guard = new PriceChangeGuard(
            new BigDecimal("0.20"), new BigDecimal("5.00"));
    private final PriceRecord previous = PriceFixtures.regular(
            UUID.randomUUID(), UUID.randomUUID(), "10.00", Instant.EPOCH);

    @Test
    void acceptsPlausibleChangesAndConfiguredBoundaries() {
        assertThat(guard.isSuspicious(new BigDecimal("8.99"), previous)).isFalse();
        assertThat(guard.isSuspicious(new BigDecimal("2.00"), previous)).isFalse();
        assertThat(guard.isSuspicious(new BigDecimal("50.00"), previous)).isFalse();
    }

    @Test
    void rejectsExtremeChangesWithoutRejectingFirstObservation() {
        assertThat(guard.isSuspicious(new BigDecimal("1.99"), previous)).isTrue();
        assertThat(guard.isSuspicious(new BigDecimal("50.01"), previous)).isTrue();
        assertThat(guard.isSuspicious(new BigDecimal("999.00"), null)).isFalse();
    }
}
