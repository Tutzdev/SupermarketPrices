package br.com.supermercados.prices.comparison;

import br.com.supermercados.prices.common.PageResponse;

import java.time.Instant;
import java.util.UUID;

public record ShoppingListComparisonResponse(
        UUID shoppingListId,
        UUID cityId,
        String currency,
        Instant comparedAt,
        PageResponse<ShoppingStoreComparison> stores
) {
}
