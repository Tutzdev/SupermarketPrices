package br.com.supermercados.prices.shoppinglist;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShoppingListResponse(UUID id, String name, ShoppingType shoppingType,
        List<ShoppingListItemResponse> items, Instant createdAt, Instant updatedAt, long version) {

    public ShoppingListResponse {
        items = List.copyOf(items);
    }
}
