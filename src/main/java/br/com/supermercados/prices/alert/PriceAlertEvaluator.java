package br.com.supermercados.prices.alert;

import br.com.supermercados.prices.price.PricePolicy;
import br.com.supermercados.prices.price.PriceRecord;
import br.com.supermercados.prices.price.PriceRecordRepository;
import br.com.supermercados.prices.price.PriceStatus;
import br.com.supermercados.prices.store.Store;
import br.com.supermercados.prices.store.StoreService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PriceAlertEvaluator {

    private final PriceAlertRepository alerts;
    private final AlertNotificationRepository notifications;
    private final PriceRecordRepository prices;
    private final StoreService stores;
    private final PricePolicy pricePolicy;
    private final Clock clock;
    private final Duration repeatInterval;

    public PriceAlertEvaluator(PriceAlertRepository alerts, AlertNotificationRepository notifications,
            PriceRecordRepository prices, StoreService stores, PricePolicy pricePolicy, Clock clock,
            @Value("${app.alerts.repeat-interval:P1D}") Duration repeatInterval) {
        if (repeatInterval.isNegative() || repeatInterval.isZero()) {
            throw new IllegalArgumentException("app.alerts.repeat-interval must be positive");
        }
        this.alerts = alerts;
        this.notifications = notifications;
        this.prices = prices;
        this.stores = stores;
        this.pricePolicy = pricePolicy;
        this.clock = clock;
        this.repeatInterval = repeatInterval;
    }

    @Transactional
    public void evaluate(UUID priceRecordId) {
        PriceRecord record = prices.findById(priceRecordId)
                .orElseThrow(() -> new IllegalStateException("Persisted price record was not found"));
        Store store = stores.requireStore(record.getStoreId());
        Instant now = clock.instant();
        var quote = pricePolicy.quote(record, now);
        if (quote.status() != PriceStatus.KNOWN) {
            return;
        }

        Instant repeatCutoff = now.minus(repeatInterval);
        for (PriceAlert alert : alerts.findActiveForEvaluation(record.getProductId(), store.getCityId())) {
            if (quote.unitPrice().compareTo(alert.getTargetPrice()) > 0
                    || alert.getLastNotifiedAt() != null && alert.getLastNotifiedAt().isAfter(repeatCutoff)) {
                continue;
            }
            int inserted = notifications.insertIfAbsent(UUID.randomUUID(), alert.getUserId(), alert.getId(),
                    record.getId(), record.getStoreId(), quote.unitPrice(), now);
            if (inserted == 1) {
                alert.markNotified(now);
            }
        }
    }
}
