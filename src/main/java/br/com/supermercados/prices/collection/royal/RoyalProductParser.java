package br.com.supermercados.prices.collection.royal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import br.com.supermercados.prices.collection.CollectedProduct;
import br.com.supermercados.prices.common.ApiException;
import br.com.supermercados.prices.price.StockAvailability;
import br.com.supermercados.prices.product.Gtin;
import tools.jackson.databind.JsonNode;

@Component
class RoyalProductParser {

    ParsedProducts parse(List<JsonNode> entries) {
        Map<String, CollectedProduct> products = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        for (JsonNode entry : entries) {
            try {
                CollectedProduct product = parseProduct(entry);
                CollectedProduct previous = products.putIfAbsent(product.sourceReference(), product);
                if (previous != null && !previous.equals(product)) {
                    throw new IllegalArgumentException("produto repetido com conteúdo divergente");
                }
            } catch (IllegalArgumentException exception) {
                warnings.add("Produto Royal " + entry.path("produto_id").asString("sem identificador")
                        + " ignorado: " + exception.getMessage());
            }
        }
        return new ParsedProducts(List.copyOf(products.values()), List.copyOf(warnings));
    }

    private CollectedProduct parseProduct(JsonNode entry) {
        String id = required(entry, "produto_id");
        String name = required(entry, "descricao");
        if (!"UN".equals(required(entry, "unidade_sigla"))
                || entry.path("possui_unidade_diferente").asBoolean()) {
            throw new IllegalArgumentException("unidade de venda exige revisão antes de comparar embalagens");
        }
        BigDecimal regular = price(entry.path("preco"));
        BigDecimal promotion = null;
        String condition = null;
        JsonNode offer = entry.path("oferta");
        if (entry.path("em_oferta").asBoolean() && offer.isObject()) {
            BigDecimal offered = price(offer.path("preco_oferta"));
            BigDecimal previous = price(offer.path("preco_antigo"));
            if (previous.compareTo(regular) > 0) {
                regular = previous;
            }
            if (offered.compareTo(regular) < 0) {
                promotion = offered;
                if (offer.path("tipo_oferta_id").asInt() != 1
                        || offer.path("quantidade_minima").asInt() > 1
                        || !"G".equals(offer.path("categoria").asString())) {
                    condition = "Oferta Royal: " + offer.path("nome").asString("condição específica")
                            + "; quantidade mínima " + offer.path("quantidade_minima").asString("não informada");
                }
            }
        }
        StockAvailability availability = StockAvailability.UNKNOWN;
        if (entry.path("disponivel").isBoolean()) {
            availability = entry.path("disponivel").asBoolean()
                    ? StockAvailability.AVAILABLE : StockAvailability.UNAVAILABLE;
        }
        String gtin;
        try {
            gtin = Gtin.normalize(entry.path("codigo_barras").asString(null));
        } catch (ApiException exception) {
            // Royal also puts internal ERP codes in codigo_barras; these are not universal identifiers.
            gtin = null;
        }
        String brand = entry.path("marca").isString() ? entry.path("marca").asString(null) : null;
        return new CollectedProduct("royal:product:" + id, name, gtin, brand, null,
                null, regular, promotion, condition, null, null, availability);
    }

    private String required(JsonNode node, String field) {
        String value = node.path(field).asString("").strip();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("campo " + field + " ausente");
        }
        return value;
    }

    private BigDecimal price(JsonNode node) {
        try {
            BigDecimal price = new BigDecimal(node.asString("")).setScale(2, RoundingMode.UNNECESSARY);
            if (price.signum() <= 0) {
                throw new IllegalArgumentException("preço deve ser positivo");
            }
            return price;
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new IllegalArgumentException("preço ausente ou inválido", exception);
        }
    }

    record ParsedProducts(List<CollectedProduct> products, List<String> warnings) {
    }
}
