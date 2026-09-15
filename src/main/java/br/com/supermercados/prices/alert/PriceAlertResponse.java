package br.com.supermercados.prices.alert;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PriceAlertResponse(
        UUID id,
        UUID productId,
        UUID cityId,
        BigDecimal targetPrice,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        Instant lastNotifiedAt) {

    static PriceAlertResponse from(PriceAlert alert) {
        return new PriceAlertResponse(alert.getId(), alert.getProductId(), alert.getCityId(),
                alert.getTargetPrice(), alert.isActive(), alert.getCreatedAt(), alert.getUpdatedAt(),
                alert.getLastNotifiedAt());
    }
}
