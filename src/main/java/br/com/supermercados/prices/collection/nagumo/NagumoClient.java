package br.com.supermercados.prices.collection.nagumo;

import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
class NagumoClient {

    private static final String STORES_PATH =
            "/on/demandware.store/Sites-Nagumo-Site/pt_BR/Stores-AvailableStores";
    private static final String SELECT_STORE_PATH =
            "/on/demandware.store/Sites-Nagumo-Site/pt_BR/Stores-SelectStore";
    private static final String SEARCH_PATH =
            "/on/demandware.store/Sites-Nagumo-Site/pt_BR/Search-UpdateGrid";
    private static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(502, 503, 504);

    private final NagumoProperties properties;
    private final ObjectMapper objectMapper;

    NagumoClient(NagumoProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        validateProperties();
    }

    NagumoCatalogResponse fetch() {
        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        HttpClient httpClient = HttpClient.newBuilder()
                .cookieHandler(cookies)
                .connectTimeout(properties.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        RequestSession session = new RequestSession(httpClient, properties.getRequestDelay());

        session.get(properties.getBaseUrl(), false);
        requireExpectedStore(session.get(resolve(STORES_PATH), true), false);
        selectStore(session);
        JsonNode store = requireExpectedStore(session.get(resolve(STORES_PATH), true), true);
        return fetchCatalog(session, store);
    }

    private NagumoCatalogResponse fetchCatalog(RequestSession session, JsonNode store) {
        List<JsonNode> products = new ArrayList<>();
        String categoryName = null;
        int sourceCount = -1;
        int maximumPages = Integer.MAX_VALUE;

        for (int pageNumber = 0, start = 0; ; pageNumber++, start += properties.getPageSize()) {
            if (pageNumber >= maximumPages) {
                throw new IllegalStateException("Paginação da Nagumo excedeu o limite esperado");
            }
            URI uri = resolve(SEARCH_PATH + "?cgid=" + encode(properties.getCategoryId())
                    + "&start=" + start + "&sz=" + properties.getPageSize());
            JsonNode response = session.get(uri, true);
            int responseCount = response.path("productSearch").path("count").asInt(-1);
            if (responseCount < 0) {
                throw new IllegalStateException("Resposta da Nagumo sem quantidade total do catálogo");
            }
            if (sourceCount < 0) {
                sourceCount = responseCount;
                maximumPages = Math.max(2,
                        (int) Math.ceil((double) sourceCount / properties.getPageSize()) + 2);
                categoryName = requiredText(response.path("productSearch").path("category"), "name");
            } else if (sourceCount != responseCount) {
                throw new IllegalStateException("Catálogo da Nagumo mudou durante a paginação");
            }

            JsonNode page = response.path("productsSearchResult");
            if (!page.isArray()) {
                throw new IllegalStateException("Resposta da Nagumo sem lista de produtos");
            }
            if (page.isEmpty()) {
                break;
            }
            page.forEach(products::add);
            if (page.size() < properties.getPageSize()) {
                break;
            }
        }

        if (sourceCount <= 0 || products.isEmpty()) {
            throw new IllegalStateException("A Nagumo retornou um catálogo vazio");
        }
        if (products.size() < sourceCount) {
            throw new IllegalStateException("A Nagumo retornou menos produtos que o total declarado");
        }
        return new NagumoCatalogResponse(store, categoryName, products.size(), products);
    }

    private JsonNode requireExpectedStore(JsonNode response, boolean selectionRequired) {
        JsonNode stores = response.path("stores");
        if (!stores.isArray()) {
            throw new IllegalStateException("Resposta da Nagumo sem lista de lojas");
        }

        JsonNode match = null;
        for (JsonNode store : stores) {
            if (properties.getStoreId().equals(store.path("storeId").asString())) {
                if (match != null) {
                    throw new IllegalStateException("A Nagumo retornou a loja configurada mais de uma vez");
                }
                match = store;
            }
        }
        if (match == null) {
            throw new IllegalStateException("A loja Nagumo configurada não está disponível");
        }
        requireEquals(match, "storeName", properties.getStoreName());
        requireEquals(match, "city", properties.getExpectedCity());
        requireEquals(match, "district", properties.getExpectedDistrict());
        requireEquals(match, "address", properties.getExpectedSourceAddress());
        if (!match.path("operation").asBoolean(false)) {
            throw new IllegalStateException("A loja Nagumo de Volta Redonda não está em operação");
        }
        if (selectionRequired && !match.path("active").asBoolean(false)) {
            throw new IllegalStateException("A Nagumo não manteve a loja de Volta Redonda selecionada");
        }
        return match;
    }

    private void selectStore(RequestSession session) {
        URI uri = resolve(SELECT_STORE_PATH + "?storeId=" + encode(properties.getStoreId())
                + "&lat=" + properties.getLatitude().toPlainString()
                + "&lng=" + properties.getLongitude().toPlainString());
        JsonNode response = session.get(uri, true);
        if (!response.path("success").asBoolean(false)) {
            throw new IllegalStateException("A Nagumo não confirmou a seleção da loja de Volta Redonda");
        }
    }

    private void requireEquals(JsonNode object, String field, String expected) {
        String actual = requiredText(object, field);
        if (!expected.equals(actual)) {
            throw new IllegalStateException("Identidade inesperada da loja Nagumo no campo " + field);
        }
    }

    private String requiredText(JsonNode object, String field) {
        String value = object.path(field).asString("").strip();
        if (value.isEmpty()) {
            throw new IllegalStateException("Resposta da Nagumo sem o campo " + field);
        }
        return value;
    }

    private URI resolve(String path) {
        return properties.getBaseUrl().resolve(path);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void validateProperties() {
        URI baseUrl = properties.getBaseUrl();
        if (baseUrl == null || baseUrl.getHost() == null
                || !("http".equalsIgnoreCase(baseUrl.getScheme())
                || "https".equalsIgnoreCase(baseUrl.getScheme()))) {
            throw new IllegalArgumentException("URL base da Nagumo inválida");
        }
        if (properties.getPageSize() < 1 || properties.getPageSize() > 100) {
            throw new IllegalArgumentException("Tamanho de página da Nagumo deve estar entre 1 e 100");
        }
        if (properties.getMaxAttempts() < 1 || properties.getMaxAttempts() > 3) {
            throw new IllegalArgumentException("Quantidade de tentativas da Nagumo deve estar entre 1 e 3");
        }
        if (properties.getMaxResponseBytes() < 1_000) {
            throw new IllegalArgumentException("Limite de resposta da Nagumo é muito pequeno");
        }
        requirePositive(properties.getConnectTimeout(), "Timeout de conexão");
        requirePositive(properties.getRequestTimeout(), "Timeout de requisição");
        if (properties.getRequestDelay().isNegative()) {
            throw new IllegalArgumentException("Intervalo entre requisições não pode ser negativo");
        }
    }

    private void requirePositive(Duration duration, String name) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " da Nagumo deve ser positivo");
        }
    }

    private final class RequestSession {

        private final HttpClient httpClient;
        private final Duration delay;
        private boolean requested;

        private RequestSession(HttpClient httpClient, Duration delay) {
            this.httpClient = httpClient;
            this.delay = delay;
        }

        private JsonNode get(URI uri, boolean jsonExpected) {
            RuntimeException lastFailure = null;
            for (int attempt = 1; attempt <= properties.getMaxAttempts(); attempt++) {
                waitBeforeRequest();
                try {
                    HttpRequest request = HttpRequest.newBuilder(uri)
                            .timeout(properties.getRequestTimeout())
                            .header("Accept", jsonExpected ? "application/json" : "text/html,application/xhtml+xml")
                            .header("User-Agent", "SupermarketPrices/1.0 public-price-collector")
                            .GET()
                            .build();
                    HttpResponse<InputStream> response = httpClient.send(
                            request, HttpResponse.BodyHandlers.ofInputStream());
                    int status = response.statusCode();
                    byte[] body = readLimited(response.body());
                    if (status >= 200 && status < 300) {
                        return jsonExpected ? parseJson(body) : objectMapper.createObjectNode();
                    }
                    lastFailure = new IllegalStateException(
                            "Nagumo respondeu HTTP " + status + " em " + uri.getPath());
                    if (!RETRYABLE_STATUS_CODES.contains(status)) {
                        throw lastFailure;
                    }
                } catch (IOException exception) {
                    lastFailure = new IllegalStateException(
                            "Falha de comunicação com a Nagumo em " + uri.getPath(), exception);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Coleta da Nagumo interrompida", exception);
                }
            }
            throw lastFailure == null
                    ? new IllegalStateException("Falha desconhecida ao consultar a Nagumo")
                    : lastFailure;
        }

        private byte[] readLimited(InputStream body) throws IOException {
            try (body) {
                byte[] bytes = body.readNBytes(properties.getMaxResponseBytes() + 1);
                if (bytes.length > properties.getMaxResponseBytes()) {
                    throw new IOException("Resposta da Nagumo excede o limite configurado");
                }
                return bytes;
            }
        }

        private JsonNode parseJson(byte[] body) {
            if (body.length == 0) {
                throw new IllegalStateException("A Nagumo retornou uma resposta vazia");
            }
            try {
                return objectMapper.readTree(body);
            } catch (RuntimeException exception) {
                throw new IllegalStateException("A Nagumo retornou JSON inválido", exception);
            }
        }

        private void waitBeforeRequest() {
            if (!requested) {
                requested = true;
                return;
            }
            try {
                Thread.sleep(delay.toMillis());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Coleta da Nagumo interrompida", exception);
            }
        }
    }
}
