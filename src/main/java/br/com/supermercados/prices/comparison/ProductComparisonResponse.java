package br.com.supermercados.prices.comparison;

import java.time.Instant;
import java.util.UUID;

import br.com.supermercados.prices.common.PageResponse;

public record ProductComparisonResponse(
        UUID productId,
        String productName,
        UUID cityId,
        String currency,
        Instant comparedAt,
        PageResponse<ProductStoreComparison> stores) {
}
