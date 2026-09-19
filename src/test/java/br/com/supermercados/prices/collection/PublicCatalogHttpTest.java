package br.com.supermercados.prices.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class PublicCatalogHttpTest {

    private HttpServer server;
    private ExecutorService executor;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newVirtualThreadPerTaskExecutor();
        server.setExecutor(executor);
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
        executor.shutdownNow();
    }

    @Test
    void retriesTransientFailureOnceButDoesNotRetryAccessDenial() {
        var transientRequests = new AtomicInteger();
        server.createContext("/retry", exchange -> {
            try (exchange) {
                exchange.sendResponseHeaders(transientRequests.incrementAndGet() == 1 ? 503 : 200, 2);
                exchange.getResponseBody().write("{}".getBytes());
            }
        });
        var deniedRequests = new AtomicInteger();
        server.createContext("/denied", exchange -> {
            try (exchange) {
                deniedRequests.incrementAndGet();
                exchange.sendResponseHeaders(403, -1);
            }
        });
        var http = new PublicCatalogHttp(Duration.ZERO);

        assertThat(http.get(uri("/retry"))).isEqualTo("{}");
        assertThat(transientRequests).hasValue(2);
        assertThatThrownBy(() -> http.get(uri("/denied"))).hasMessageContaining("403");
        assertThat(deniedRequests).hasValue(1);
    }

    @Test
    void deadlineIncludesResponseBodyAfterHeadersHaveArrived() {
        server.createContext("/slow", exchange -> {
            try (exchange) {
                exchange.sendResponseHeaders(200, 2);
                try {
                    Thread.sleep(2000);
                    exchange.getResponseBody().write("{}".getBytes());
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        var http = new PublicCatalogHttp(Duration.ZERO, Duration.ofMillis(100));

        assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
                assertThatThrownBy(() -> http.get(uri("/slow"))).isInstanceOf(IllegalStateException.class));
    }

    @Test
    void rejectsOversizedBodies() {
        server.createContext("/large", exchange -> {
            try (exchange) {
                exchange.sendResponseHeaders(200, 8_000_001);
                exchange.getResponseBody().write(new byte[8_000_001]);
            }
        });
        assertThatThrownBy(() -> new PublicCatalogHttp(Duration.ZERO).get(uri("/large")))
                .isInstanceOf(IllegalStateException.class);
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
    }
}
