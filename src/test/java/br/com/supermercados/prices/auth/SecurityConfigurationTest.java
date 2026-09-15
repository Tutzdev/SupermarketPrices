package br.com.supermercados.prices.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class SecurityConfigurationTest {

    private final SecurityConfiguration security = new SecurityConfiguration();

    @Test
    void corsDefaultsToNoAllowedOriginsAndNoCookieCredentials() {
        var source = security.corsConfigurationSource("");
        var configuration = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/v1/products"));

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins()).isEmpty();
        assertThat(configuration.getAllowCredentials()).isFalse();
    }

    @Test
    void corsAcceptsExplicitOriginsAndBearerAuthorization() {
        var source = security.corsConfigurationSource("https://example.test, http://localhost:3000");
        var configuration = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/v1/products"));

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins()).containsExactly("https://example.test", "http://localhost:3000");
        assertThat(configuration.getAllowedHeaders()).contains("Authorization");
    }

    @Test
    void corsRejectsWildcardsAndPaths() {
        assertThatThrownBy(() -> security.corsConfigurationSource("*"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> security.corsConfigurationSource("https://example.test/app"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> security.corsConfigurationSource("https://*.example.test"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
