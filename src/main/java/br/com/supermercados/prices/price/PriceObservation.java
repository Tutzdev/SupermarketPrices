package br.com.supermercados.prices.price;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A source reference identifies one immutable observation, not a mutable product page. */
public record PriceObservation(
        @NotNull UUID productId,
        @NotNull UUID storeId,
        @NotNull UUID sourceId,
        @NotBlank @Size(max = 500) String sourceReference,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal regularPrice,
        @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal promotionalPrice,
        @NotNull @Pattern(regexp = "BRL") String currency,
        @NotNull Instant collectedAt,
        Instant validUntil,
        Instant promotionValidUntil,
        @Size(max = 500) String promotionCondition,
        @NotNull StockAvailability availability
) {
    public PriceObservation(
            UUID productId,
            UUID storeId,
            UUID sourceId,
            String sourceReference,
            BigDecimal regularPrice,
            BigDecimal promotionalPrice,
            String currency,
            Instant collectedAt,
            Instant validUntil,
            Instant promotionValidUntil,
            StockAvailability availability) {
        this(productId, storeId, sourceId, sourceReference, regularPrice, promotionalPrice,
                currency, collectedAt, validUntil, promotionValidUntil, null, availability);
    }
}
