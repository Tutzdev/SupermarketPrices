package br.com.supermercados.prices.comparison;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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
}
