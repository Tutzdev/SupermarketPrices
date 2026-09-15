package br.com.supermercados.prices.price;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Historical values describe the observation time and are not a current-price assertion. */
public record PriceRecordResponse(
        UUID id,
        UUID productId,
        UUID storeId,
        BigDecimal regularPrice,
        BigDecimal promotionalPrice,
        String currency,
        StockAvailability availability,
        Instant collectedAt,
        Instant recordedAt,
        Instant validUntil,
        Instant promotionValidUntil,
        UUID sourceId,
        String sourceReference,
        PriceOriginType originType,
        UUID contributionId
) {
    public static PriceRecordResponse from(PriceRecord record) {
        return new PriceRecordResponse(
            record.getId(), 
            record.getProductId(), 
            record.getStoreId(),
            record.getRegularPrice(), 
            record.getPromotionalPrice(), 
            record.getCurrency(),
            record.getAvailability(), 
            record.getCollectedAt(), 
            record.getRecordedAt(),
            record.getValidUntil(), 
            record.getPromotionValidUntil(), 
            record.getSourceId(),
            record.getSourceReference(),
            record.getOriginType(),
            record.getContributionId());
    }
}
