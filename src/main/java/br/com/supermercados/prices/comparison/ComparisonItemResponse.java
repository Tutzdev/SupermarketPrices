package br.com.supermercados.prices.comparison;

import br.com.supermercados.prices.price.PriceQuote;

import java.math.BigDecimal;
import java.util.UUID;

public record ComparisonItemResponse(
        UUID productId,
        String productName,
        BigDecimal quantity,
        PriceQuote price,
        BigDecimal lineTotal
) {
}
