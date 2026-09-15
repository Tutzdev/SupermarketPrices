package br.com.supermercados.prices.alert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "price_alerts")
class PriceAlert {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private UUID cityId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal targetPrice;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant lastNotifiedAt;

    @Version
    private long version;

    protected PriceAlert() {
    }

    PriceAlert(UUID userId, CreatePriceAlertRequest request, Instant now) {
        id = UUID.randomUUID();
        this.userId = userId;
        productId = request.productId();
        cityId = request.cityId();
        targetPrice = request.targetPrice().setScale(2, RoundingMode.UNNECESSARY);
        active = true;
        createdAt = now;
        updatedAt = now;
    }

    void deactivate(Instant now) {
        active = false;
        updatedAt = now;
    }

    void markNotified(Instant now) {
        lastNotifiedAt = now;
        updatedAt = now;
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    UUID getProductId() {
        return productId;
    }

    UUID getCityId() {
        return cityId;
    }

    BigDecimal getTargetPrice() {
        return targetPrice;
    }

    boolean isActive() {
        return active;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

    Instant getLastNotifiedAt() {
        return lastNotifiedAt;
    }
}
