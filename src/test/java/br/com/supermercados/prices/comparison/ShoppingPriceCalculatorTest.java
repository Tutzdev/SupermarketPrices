package br.com.supermercados.prices.comparison;

import br.com.supermercados.prices.price.PriceFixtures;
import br.com.supermercados.prices.price.PricePolicy;
import br.com.supermercados.prices.price.PriceRecord;
import br.com.supermercados.prices.shoppinglist.ShoppingListItemResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ShoppingPriceCalculatorTest {

    private final Instant now = Instant.parse("2026-09-10T12:00:00Z");
    private final UUID storeId = UUID.randomUUID();
    private final ShoppingPriceCalculator calculator = new ShoppingPriceCalculator(
            new PricePolicy(Duration.ofDays(2)));

    @Test
    void multipliesQuantitiesWithDecimalPrecisionAndRoundsEachLineHalfUp() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        List<ShoppingListItemResponse> items = List.of(item(first, "3.000"), item(second, "1.234"),
                item(third, "0.500"));
        Map<UUID, PriceRecord> prices = Map.of(
                first, PriceFixtures.regular(first, storeId, "0.10", now),
                second, PriceFixtures.regular(second, storeId, "1.25", now),
                third, PriceFixtures.regular(third, storeId, "0.01", now));

        ShoppingStoreComparison result = calculator.calculate(storeId, "Synthetic test store", items, prices, now);

        assertThat(result.subtotalKnown()).isEqualByComparingTo("1.85");
        assertThat(result.subtotalKnown().scale()).isEqualTo(2);
        assertThat(result.items()).extracting(ComparisonItemResponse::lineTotal)
                .containsExactly(new BigDecimal("0.30"), new BigDecimal("1.54"), new BigDecimal("0.01"));
        assertThat(result.completeShoppingList()).isTrue();
        assertThat(result.pricedItems()).isEqualTo(3);
    }

    @Test
    void missingProductIsNeverFreeAndMakesTheListIncomplete() {
        UUID known = UUID.randomUUID();
        UUID missing = UUID.randomUUID();
        var items = List.of(item(known, "2.000"), item(missing, "3.000"));
        var prices = Map.of(known, PriceFixtures.regular(known, storeId, "12.35", now));

        ShoppingStoreComparison result = calculator.calculate(storeId, "Synthetic test store", items, prices, now);

        assertThat(result.subtotalKnown()).isEqualByComparingTo("24.70");
        assertThat(result.completeShoppingList()).isFalse();
        assertThat(result.requestedItems()).isEqualTo(2);
        assertThat(result.pricedItems()).isEqualTo(1);
        assertThat(result.missingItems()).isEqualTo(1);
        assertThat(result.items().get(1).lineTotal()).isNull();
        assertThat(result.items().get(1).price().unitPrice()).isNull();
    }

    @Test
    void entirelyMissingPricesProduceNullSubtotal() {
        var items = List.of(item(UUID.randomUUID(), "2.000"));

        ShoppingStoreComparison result = calculator.calculate(
                storeId, "Synthetic test store", items, Map.of(), now);

        assertThat(result.subtotalKnown()).isNull();
        assertThat(result.completeShoppingList()).isFalse();
        assertThat(result.missingItems()).isEqualTo(1);
    }

    @Test
    void emptyListIsNotPresentedAsACompleteFreePurchase() {
        ShoppingStoreComparison result = calculator.calculate(
                storeId, "Synthetic test store", List.of(), Map.of(), now);

        assertThat(result.subtotalKnown()).isNull();
        assertThat(result.completeShoppingList()).isFalse();
        assertThat(result.requestedItems()).isZero();
    }

    @Test
    void expiredPriceDoesNotContributeToSubtotal() {
        UUID productId = UUID.randomUUID();
        var prices = Map.of(productId, PriceFixtures.regular(
                productId, storeId, "10.00", now.minus(Duration.ofDays(3))));

        ShoppingStoreComparison result = calculator.calculate(storeId, "Synthetic test store",
                List.of(item(productId, "1.000")), prices, now);

        assertThat(result.subtotalKnown()).isNull();
        assertThat(result.pricedItems()).isZero();
        assertThat(result.missingItems()).isEqualTo(1);
    }

    private ShoppingListItemResponse item(UUID productId, String quantity) {
        return new ShoppingListItemResponse(UUID.randomUUID(), productId, "Synthetic test product",
                new BigDecimal(quantity));
    }
}
