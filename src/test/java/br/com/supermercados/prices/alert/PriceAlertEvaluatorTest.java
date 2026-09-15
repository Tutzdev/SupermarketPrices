package br.com.supermercados.prices.alert;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.supermercados.prices.price.PriceFixtures;
import br.com.supermercados.prices.price.PricePolicy;
import br.com.supermercados.prices.price.PriceRecord;
import br.com.supermercados.prices.price.PriceRecordRepository;
import br.com.supermercados.prices.store.Store;
import br.com.supermercados.prices.store.StoreService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PriceAlertEvaluatorTest {

    private static final Instant NOW = Instant.parse("2026-09-15T12:00:00Z");

    @Mock private PriceAlertRepository alerts;
    @Mock private AlertNotificationRepository notifications;
    @Mock private PriceRecordRepository prices;
    @Mock private StoreService stores;
    @Mock private Store store;

    private PriceAlertEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new PriceAlertEvaluator(alerts, notifications, prices, stores,
                new PricePolicy(Duration.ofDays(2)), Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofDays(1));
    }

    @Test
    void reprocessingTheSameQualifyingPriceDoesNotDuplicateNotification() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID cityId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        PriceRecord record = PriceFixtures.regular(productId, storeId, "8.00", NOW.minusSeconds(30));
        PriceAlert alert = new PriceAlert(userId,
                new CreatePriceAlertRequest(productId, cityId, new BigDecimal("10.00")), NOW.minusSeconds(60));
        when(prices.findById(record.getId())).thenReturn(Optional.of(record));
        when(stores.requireStore(storeId)).thenReturn(store);
        when(store.getCityId()).thenReturn(cityId);
        when(alerts.findActiveForEvaluation(productId, cityId)).thenReturn(List.of(alert));
        when(notifications.insertIfAbsent(any(), eq(userId), eq(alert.getId()), eq(record.getId()),
                eq(storeId), eq(new BigDecimal("8.00")), eq(NOW))).thenReturn(1);

        evaluator.evaluate(record.getId());
        evaluator.evaluate(record.getId());

        verify(notifications, times(1)).insertIfAbsent(any(), eq(userId), eq(alert.getId()),
                eq(record.getId()), eq(storeId), eq(new BigDecimal("8.00")), eq(NOW));
    }
}
