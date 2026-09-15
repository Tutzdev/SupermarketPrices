package br.com.supermercados.prices.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.supermercados.prices.common.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class AttemptRateLimiterTest {

    @Test
    void rejectsAttemptsBeyondTheConfiguredLimit() {
        AttemptRateLimiter limiter = new AttemptRateLimiter(
                Clock.fixed(Instant.parse("2026-09-15T12:00:00Z"), ZoneOffset.UTC));

        limiter.check("login", "client", 2, Duration.ofMinutes(1));
        limiter.check("login", "client", 2, Duration.ofMinutes(1));

        assertThatThrownBy(() -> limiter.check("login", "client", 2, Duration.ofMinutes(1)))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getStatus())
                                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }
}
