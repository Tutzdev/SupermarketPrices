package br.com.supermercados.prices.shoppinglist;

import java.time.Instant;
import java.util.UUID;

public record ShoppingListSummary(UUID id, String name, ShoppingType shoppingType,
        Instant createdAt, Instant updatedAt, long version) {

    static ShoppingListSummary from(ShoppingList list) {
        return new ShoppingListSummary(list.getId(), list.getName(), list.getShoppingType(),
                list.getCreatedAt(), list.getUpdatedAt(), list.getVersion());
    }
}
