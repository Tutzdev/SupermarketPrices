package br.com.supermercados.prices.collection;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import br.com.supermercados.prices.price.PriceRecord;

@Component
public class PriceObservationReference {

    private final ZoneId collectionZone;

    public PriceObservationReference(
            @Value("${app.collection.zone:America/Sao_Paulo}") ZoneId collectionZone) {
        this.collectionZone = collectionZone;
    }

    public String create(String collectorCode, String storeReference,
            CollectedProduct product, Instant collectedAt) {
        String content = String.join("|",
                amount(product.regularPrice()),
                amount(product.promotionalPrice()),
                value(product.promotionCondition()),
                timestamp(product.validUntil()),
                timestamp(product.promotionValidUntil()),
                product.availability().name());
        String hash = sha256(content).substring(0, 24);
        String collectionDate = collectedAt.atZone(collectionZone).toLocalDate().toString();
        return collectorCode + ":" + storeReference + ":" + product.sourceReference()
                + ":" + collectionDate + ":" + hash;
    }

    public String createAfter(String collectorCode, String storeReference,
            CollectedProduct product, Instant collectedAt, PriceRecord previous) {
        String baseReference = create(collectorCode, storeReference, product, collectedAt);
        return baseReference + ":after-"
                + sha256(previous.getSourceReference()).substring(0, 16);
    }

    public boolean isSameDailyObservation(
            PriceRecord previous, CollectedProduct product, Instant collectedAt) {
        if (previous == null || !previous.getCollectedAt().atZone(collectionZone).toLocalDate()
                .equals(collectedAt.atZone(collectionZone).toLocalDate())) {
            return false;
        }
        return previous.getRegularPrice().compareTo(product.regularPrice()) == 0
                && equalAmount(previous.getPromotionalPrice(), product.promotionalPrice())
                && Objects.equals(previous.getPromotionCondition(), clean(product.promotionCondition()))
                && Objects.equals(previous.getValidUntil(), product.validUntil())
                && Objects.equals(previous.getPromotionValidUntil(), product.promotionValidUntil())
                && previous.getAvailability() == product.availability();
    }

    private String amount(BigDecimal amount) {
        return amount == null ? "" : amount.stripTrailingZeros().toPlainString();
    }

    private String value(String value) {
        return value == null ? "" : value.strip();
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private String timestamp(Instant value) {
        return value == null ? "" : value.toString();
    }

    private boolean equalAmount(BigDecimal first, BigDecimal second) {
        return first == null ? second == null : second != null && first.compareTo(second) == 0;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }
}
