package br.com.supermercados.prices.comparison;

import java.math.BigDecimal;
import java.util.UUID;

import br.com.supermercados.prices.price.PriceQuote;

public record ComparisonItemResponse(
        UUID productId,
        String productName,
        BigDecimal quantity,
        PriceQuote price,
        BigDecimal lineTotal) {
}
