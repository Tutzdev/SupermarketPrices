package br.com.supermercados.prices.comparison;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

import br.com.supermercados.prices.price.PricePolicy;
import br.com.supermercados.prices.price.PriceQuote;
import br.com.supermercados.prices.price.PriceRecord;
import br.com.supermercados.prices.shoppinglist.ShoppingListItemResponse;

@Component
public class ShoppingPriceCalculator {

    private final PricePolicy pricePolicy;

    public ShoppingPriceCalculator(PricePolicy pricePolicy) {
        this.pricePolicy = pricePolicy;
    }

    public ShoppingStoreComparison calculate(
            UUID storeId, String storeName, List<ShoppingListItemResponse> requestedItems,
            Map<UUID, PriceRecord> productPrices, Instant comparedAt) {
        List<ComparisonItemResponse> items = requestedItems.stream()
                .map(item -> calculateItem(item, productPrices.get(item.productId()), comparedAt))
                .toList();
        int pricedItems = (int) items.stream().filter(item -> item.lineTotal() != null).count();
        BigDecimal subtotal = pricedItems == 0 ? null : items.stream()
                .map(ComparisonItemResponse::lineTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        return new ShoppingStoreComparison(storeId, storeName, items.size(), pricedItems,
                items.size() - pricedItems, subtotal, !items.isEmpty() && pricedItems == items.size(), items);
    }

    private ComparisonItemResponse calculateItem(
            ShoppingListItemResponse item, PriceRecord record, Instant comparedAt) {
        PriceQuote quote = pricePolicy.quote(record, comparedAt);
        // Round each priced line to cents before summing, including fractional requested quantities.
        BigDecimal lineTotal = quote.unitPrice() == null ? null
                : quote.unitPrice().multiply(item.quantity()).setScale(2, RoundingMode.HALF_UP);
        return new ComparisonItemResponse(item.productId(), item.productName(), item.quantity(),
                quote, lineTotal);
    }

    public ShoppingCombinationResponse combine(List<ShoppingListItemResponse> requestedItems,
            List<ShoppingStoreComparison> comparisons, StoreRecommendationCandidate completeStore) {
        Map<UUID, StoreItem> cheapestItems = new LinkedHashMap<>();
        for (ShoppingStoreComparison store : comparisons) {
            for (ComparisonItemResponse item : store.items()) {
                if (item.lineTotal() == null) {
                    continue;
                }
                StoreItem previous = cheapestItems.get(item.productId());
                boolean preferCompleteStore = completeStore != null && store.storeId().equals(completeStore.storeId());
                if (previous == null || item.lineTotal().compareTo(previous.item().lineTotal()) < 0
                        || (item.lineTotal().compareTo(previous.item().lineTotal()) == 0 && preferCompleteStore)) {
                    cheapestItems.put(item.productId(), new StoreItem(store.storeId(), store.storeName(), item));
                }
            }
        }

        Map<UUID, List<ComparisonItemResponse>> itemsByStore = new LinkedHashMap<>();
        Map<UUID, String> storeNames = new LinkedHashMap<>();
        List<UUID> missing = new ArrayList<>();
        for (ShoppingListItemResponse requested : requestedItems) {
            StoreItem chosen = cheapestItems.get(requested.productId());
            if (chosen == null) {
                missing.add(requested.productId());
            } else {
                itemsByStore.computeIfAbsent(chosen.storeId(), ignored -> new ArrayList<>()).add(chosen.item());
                storeNames.put(chosen.storeId(), chosen.storeName());
            }
        }
        List<ShoppingCombinationResponse.StorePurchase> purchases = itemsByStore.entrySet().stream()
                .map(entry -> new ShoppingCombinationResponse.StorePurchase(entry.getKey(), storeNames.get(entry.getKey()),
                        entry.getValue().stream().map(ComparisonItemResponse::lineTotal)
                                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add), entry.getValue()))
                .toList();
        BigDecimal total = purchases.isEmpty() ? null : purchases.stream()
                .map(ShoppingCombinationResponse.StorePurchase::subtotal)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        boolean complete = !requestedItems.isEmpty() && missing.isEmpty();
        BigDecimal savings = complete && completeStore != null
                ? completeStore.total().subtract(total) : null;
        return new ShoppingCombinationResponse(requestedItems.size(), cheapestItems.size(), missing,
                total, complete, savings, purchases);
    }

    private record StoreItem(UUID storeId, String storeName, ComparisonItemResponse item) {
    }
}
