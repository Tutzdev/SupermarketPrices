package br.com.supermercados.prices.support;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** Public response snapshots with explicit mutations for failure scenarios. Never contacts the internet. */
public class CollectorFixtureServer implements AutoCloseable {

    private final HttpServer server;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final JsonMapper mapper = JsonMapper.builder().build();
    public final List<String> catalogRequests = new ArrayList<>();
    public ObjectNode nagumoStores;
    public ObjectNode royalStore;
    public ObjectNode royalPickups;
    public String failurePath;
    public int failureStatus = 503;
    public int failuresRemaining;
    public long delayMillis;
    public boolean invalidJson;
    public boolean emptyCatalog;
    public boolean rejectSelection;
    public boolean receivedSession;
    public boolean receivedPublicAuthorization;
    public boolean cocaOnly;
    public boolean shortNagumoFirstPage;

    public CollectorFixtureServer() throws IOException {
        nagumoStores = (ObjectNode) fixture("nagumo-stores.json");
        royalStore = (ObjectNode) fixture("royal-store.json");
        royalPickups = (ObjectNode) fixture("royal-pickups.json");
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(executor);
        server.createContext("/", this::respond);
        server.start();
    }

    public URI baseUrl() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    public JsonNode fixture(String name) throws IOException {
        try (var input = getClass().getResourceAsStream("/fixtures/collectors/" + name)) {
            if (input == null) {
                throw new IOException("Missing public fixture " + name);
            }
            return mapper.readTree(input);
        }
    }

    private void respond(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        try {
            if (path.equals(failurePath)) {
                if (delayMillis > 0) {
                    Thread.sleep(delayMillis);
                }
                if (failuresRemaining-- > 0) {
                    write(exchange, failureStatus, "{}");
                    return;
                }
                if (invalidJson) {
                    write(exchange, 200, "{invalid-json");
                    return;
                }
            }
            if (path.equals("/")) {
                exchange.getResponseHeaders().add("Set-Cookie", "public-session=fixture; Path=/");
                write(exchange, 200, "<html></html>");
            } else if (path.endsWith("Stores-AvailableStores")) {
                receivedSession = exchange.getRequestHeaders().getFirst("Cookie") != null;
                write(exchange, 200, mapper.writeValueAsString(nagumoStores));
            } else if (path.endsWith("Stores-SelectStore")) {
                ((ObjectNode) nagumoStores.path("stores").get(0)).put("active", !rejectSelection);
                write(exchange, 200, "{\"success\":" + !rejectSelection + "}");
            } else if (path.endsWith("Search-UpdateGrid")) {
                String query = exchange.getRequestURI().getQuery();
                String start = query.contains("start=100") ? "100" : query.contains("start=50") ? "50" : "0";
                ObjectNode page = (ObjectNode) fixture(cocaOnly ? "nagumo-PARCEIRO-COCA-COLA-0.json"
                        : "nagumo-MP-GERAL-" + start + ".json");
                if (emptyCatalog) {
                    page = (ObjectNode) fixture("nagumo-empty.json");
                }
                if (shortNagumoFirstPage && "0".equals(start)) {
                    ((tools.jackson.databind.node.ArrayNode) page.path("productsSearchResult")).remove(49);
                }
                catalogRequests.add(query);
                write(exchange, 200, mapper.writeValueAsString(page));
            } else if (path.contains("/organizacoes/filiais/dominio/")) {
                write(exchange, 200, mapper.writeValueAsString(fixture("royal-domain.json")));
            } else if (path.equals("/public-config.js")) {
                write(exchange, 200, "lojaUser:\"loja\",lojaAuthJWT:\"sanitized-public-bootstrap\"");
            } else if (path.endsWith("/auth/loja/login")) {
                write(exchange, 200, "{\"success\":true,\"data\":\"sanitized-anonymous-session\"}");
            } else if (path.endsWith("/centros_distribuicoes/retiradas")) {
                receivedPublicAuthorization = "Bearer sanitized-anonymous-session"
                        .equals(exchange.getRequestHeaders().getFirst("Authorization"));
                write(exchange, 200, mapper.writeValueAsString(royalPickups));
            } else if (path.endsWith("/centros_distribuicoes/1")) {
                write(exchange, 200, mapper.writeValueAsString(royalStore));
            } else if (path.endsWith("/departamentos/arvore")) {
                write(exchange, 200, "{\"success\":true,\"data\":[{\"classificacao_mercadologica_id\":5}]}");
            } else if (path.endsWith("/buscas/produtos/termo/coca") || path.endsWith("/departamentos/5/produtos")) {
                String pageNumber = exchange.getRequestURI().getQuery().contains("page=2") ? "2" : "1";
                ObjectNode page = (ObjectNode) fixture("royal-coca-" + pageNumber + ".json");
                if (emptyCatalog) {
                    page = (ObjectNode) fixture("royal-empty.json");
                }
                if (path.endsWith("/departamentos/5/produtos")) {
                    page.set("data", page.path("data").path("produtos"));
                }
                catalogRequests.add(exchange.getRequestURI().toString());
                write(exchange, 200, mapper.writeValueAsString(page));
            } else {
                write(exchange, 404, "{}");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void write(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }
}
