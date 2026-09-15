package br.com.supermercados.prices.store;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StoreResponse(
        UUID id,
        UUID supermarketChainId,
        UUID cityId,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean active,
        UUID sourceId,
        String sourceReference,
        Instant collectedAt,
        Instant updatedAt) {

    public static StoreResponse from(Store store) {
        return new StoreResponse(store.getId(), store.getSupermarketChainId(), store.getCityId(), store.getName(),
                store.getAddress(), store.getLatitude(), store.getLongitude(), store.isActive(),
                store.getSourceId(), store.getSourceReference(), store.getCollectedAt(), store.getUpdatedAt());
    }
}
