package br.com.supermercados.prices.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    private UUID id;

    @Column(unique = true, length = 14)
    private String gtin;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 120)
    private String brand;

    @Column(length = 2000)
    private String description;

    @Column(length = 30)
    private String unit;

    @Column(precision = 14, scale = 4)
    private BigDecimal quantity;

    @Column(length = 120)
    private String category;

    @Column(nullable = false)
    private UUID sourceId;

    @Column(nullable = false, length = 2048)
    private String sourceReference;

    @Column(nullable = false)
    private Instant collectedAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    Product(ProductObservation observation, String normalizedGtin, Instant now) {
        id = UUID.randomUUID();
        gtin = normalizedGtin;
        updateDetails(observation, now);
    }

    void assignGtin(String normalizedGtin) {
        if (gtin == null) {
            gtin = normalizedGtin;
        }
    }

    void updateDetails(ProductObservation observation, Instant now) {
        name = observation.name().strip();
        brand = cleanOptional(observation.brand());
        description = cleanOptional(observation.description());
        unit = cleanOptional(observation.unit());
        quantity = observation.quantity();
        category = cleanOptional(observation.category());
        sourceId = observation.source().sourceId();
        sourceReference = observation.source().sourceReference();
        collectedAt = observation.source().collectedAt();
        updatedAt = now;
    }

    private String cleanOptional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
