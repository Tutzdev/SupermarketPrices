package br.com.supermercados.prices.collection.nagumo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import br.com.supermercados.prices.collection.CollectedProduct;
import br.com.supermercados.prices.price.StockAvailability;
import tools.jackson.databind.JsonNode;

@Component
class NagumoProductParser {

    private static final String MEMBER_CONDITION = "Preço exclusivo para clientes Meu Nagumo";

    private final NagumoProperties properties;

    NagumoProductParser(NagumoProperties properties) {
        this.properties = properties;
    }

    ParsedProducts parse(NagumoCatalogResponse catalog, Instant validUntil) {
        Map<String, CollectedProduct> products = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();

        for (JsonNode externalProduct : catalog.products()) {
            try {
                CollectedProduct product = parseProduct(externalProduct, catalog.categoryName(), validUntil);
                if (products.putIfAbsent(product.sourceReference(), product) != null) {
                    warnings.add("Produto duplicado na resposta da Nagumo: " + product.sourceReference());
                }
            } catch (RuntimeException exception) {
                String id = externalProduct.path("id").asText("sem identificador");
                warnings.add("Produto Nagumo " + id + " ignorado: " + safeMessage(exception));
            }
        }
        return new ParsedProducts(List.copyOf(products.values()), warnings);
    }

    private CollectedProduct parseProduct(JsonNode product, String category, Instant validUntil) {
        String id = requiredText(product, "id");
        String name = requiredText(product, "productName");
        BigDecimal salesPrice = requiredPrice(product.path("price").path("sales").path("value"));
        BigDecimal listPrice = optionalPrice(product.path("price").path("list").path("value"));
        BigDecimal memberPrice = memberPrice(product.path("flagtypes"));

        BigDecimal regularPrice = salesPrice;
        BigDecimal promotionalPrice = null;
        String promotionCondition = null;
        if (memberPrice != null && memberPrice.compareTo(salesPrice) < 0) {
            promotionalPrice = memberPrice;
            promotionCondition = MEMBER_CONDITION;
        } else if (listPrice != null && listPrice.compareTo(salesPrice) > 0) {
            regularPrice = listPrice;
            promotionalPrice = salesPrice;
        }

        String brand = optionalText(product, "brand");
        String description = firstText(product, "productAdditionalInfo", "shortDescription", "longDescription");
        String gtin = firstText(product, "gtin", "ean");
        StockAvailability availability = product.path("available").asBoolean(false)
                ? StockAvailability.AVAILABLE
                : StockAvailability.UNAVAILABLE;

        return new CollectedProduct(
                "nagumo:product:" + id,
                name,
                gtin,
                brand,
                description,
                category,
                regularPrice,
                promotionalPrice,
                promotionCondition,
                validUntil,
                promotionalPrice == null ? null : validUntil,
                availability);
    }

    private BigDecimal memberPrice(JsonNode flags) {
        if (!flags.isArray()) {
            return null;
        }
        for (JsonNode flag : flags) {
            if (properties.getPromotionFlag().equals(flag.path("flagType").asText())) {
                BigDecimal price = optionalPrice(flag.path("valueFlag"));
                if (price == null) {
                    throw new IllegalArgumentException("preço Meu Nagumo ausente");
                }
                return price;
            }
        }
        return null;
    }

    private BigDecimal requiredPrice(JsonNode value) {
        BigDecimal price;
        try {
            price = optionalPrice(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("preço normal inválido: " + exception.getMessage(), exception);
        }
        if (price == null) {
            throw new IllegalArgumentException("preço normal ausente ou inválido");
        }
        return price;
    }

    private BigDecimal optionalPrice(JsonNode value) {
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (!value.isNumber()) {
            throw new IllegalArgumentException("preço com formato inválido");
        }
        BigDecimal price;
        try {
            price = value.decimalValue().setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("preço com precisão inválida", exception);
        }
        if (price.signum() <= 0) {
            throw new IllegalArgumentException("preço deve ser maior que zero");
        }
        return price;
    }

    private String requiredText(JsonNode product, String field) {
        String value = optionalText(product, field);
        if (value == null) {
            throw new IllegalArgumentException("campo " + field + " ausente");
        }
        return value;
    }

    private String optionalText(JsonNode product, String field) {
        JsonNode value = product.path(field);
        if (!value.isTextual() || value.asText().isBlank()) {
            return null;
        }
        return value.asText().strip();
    }

    private String firstText(JsonNode product, String... fields) {
        for (String field : fields) {
            String value = optionalText(product, field);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }

    record ParsedProducts(List<CollectedProduct> products, List<String> warnings) {

        ParsedProducts {
            products = List.copyOf(products);
            warnings = List.copyOf(warnings);
        }
    }
}
