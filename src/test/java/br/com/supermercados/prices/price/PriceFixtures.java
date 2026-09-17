package br.com.supermercados.prices.price;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Synthetic records restricted to automated tests. */
public final class PriceFixtures {

    private PriceFixtures() {
    }

    public static PriceRecord regular(UUID productId, UUID storeId, String amount, Instant collectedAt) {
        return observation(productId, storeId, amount, null, collectedAt, null, null,
                StockAvailability.UNKNOWN);
    }

    public static PriceRecord observation(UUID productId, UUID storeId, String regular, String promotional,
                                          Instant collectedAt, Instant validUntil, Instant promotionValidUntil,
                                          StockAvailability availability) {
        return observation(productId, storeId, regular, promotional, collectedAt,
                validUntil, promotionValidUntil, null, availability);
    }

    public static PriceRecord observation(UUID productId, UUID storeId, String regular, String promotional,
                                          Instant collectedAt, Instant validUntil, Instant promotionValidUntil,
                                          String promotionCondition, StockAvailability availability) {
        return PriceRecord.from(new PriceObservation(productId, storeId, UUID.randomUUID(),
                "synthetic-observation-" + UUID.randomUUID(), new BigDecimal(regular),
                promotional == null ? null : new BigDecimal(promotional), "BRL", collectedAt,
                validUntil, promotionValidUntil, promotionCondition, availability), collectedAt);
    }
}
