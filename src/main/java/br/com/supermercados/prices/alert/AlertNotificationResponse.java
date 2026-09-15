package br.com.supermercados.prices.alert;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AlertNotificationResponse(
        UUID id,
        UUID alertId,
        UUID priceRecordId,
        UUID storeId,
        BigDecimal unitPrice,
        Instant createdAt,
        Instant readAt) {

    static AlertNotificationResponse from(AlertNotification notification) {
        return new AlertNotificationResponse(notification.getId(), notification.getAlertId(),
                notification.getPriceRecordId(), notification.getStoreId(), notification.getUnitPrice(),
                notification.getCreatedAt(), notification.getReadAt());
    }
}
