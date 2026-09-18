package br.com.supermercados.prices.comparison;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ShoppingCombinationResponse(
        int requestedItems,
        int pricedItems,
        List<UUID> missingProductIds,
        BigDecimal subtotalKnown,
        boolean completeShoppingList,
        BigDecimal savingsAgainstCompleteStore,
        List<StorePurchase> stores) {

    public ShoppingCombinationResponse {
        missingProductIds = List.copyOf(missingProductIds);
        stores = List.copyOf(stores);
    }

    public record StorePurchase(UUID storeId, String storeName, BigDecimal subtotal,
            List<ComparisonItemResponse> items) {

        public StorePurchase {
            items = List.copyOf(items);
        }
    }
}
