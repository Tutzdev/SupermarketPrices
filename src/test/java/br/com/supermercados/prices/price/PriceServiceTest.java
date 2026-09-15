package br.com.supermercados.prices.price;

import br.com.supermercados.prices.common.ApiException;
import br.com.supermercados.prices.datasource.DataSourceService;
import br.com.supermercados.prices.product.ProductService;
import br.com.supermercados.prices.store.StoreRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceServiceTest {

    private static final ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();
    private final Instant now = Instant.parse("2026-09-10T12:00:00Z");
    private final UUID productId = UUID.randomUUID();
    private final UUID storeId = UUID.randomUUID();
    private final UUID sourceId = UUID.randomUUID();

    @Mock private PriceRecordRepository prices;
    @Mock private ProductService products;
    @Mock private StoreRepository stores;
    @Mock private DataSourceService sources;

    private PriceService service;

    @BeforeEach
    void setUp() {
        service = new PriceService(prices, products, stores, sources, VALIDATORS.getValidator(),
                Clock.fixed(now, ZoneOffset.UTC));
    }

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATORS.close();
    }

    @Test
    void observationReplayReturnsOriginalRecordWithoutChangingItsHistory() {
        PriceObservation observation = observation("12.30", null, now.minusSeconds(60));
        PriceRecord persisted = PriceRecord.from(observation, now.minusSeconds(30));
        when(stores.existsById(storeId)).thenReturn(true);
        when(prices.findBySourceIdAndSourceReference(sourceId, "synthetic-reference"))
                .thenReturn(Optional.of(persisted));

        PriceRecordResponse response = service.appendObservation(observation);

        assertThat(response.id()).isEqualTo(persisted.getId());
        assertThat(response.recordedAt()).isEqualTo(now.minusSeconds(30));
        verify(prices).insertIfAbsent(any(PriceRecord.class));
        verify(sources).requireEnabledSource(sourceId);
    }

    @Test
    void reusedSourceReferenceWithChangedPriceIsRejected() {
        PriceRecord persisted = PriceRecord.from(observation("12.30", null, now.minusSeconds(60)), now);
        when(stores.existsById(storeId)).thenReturn(true);
        when(prices.findBySourceIdAndSourceReference(sourceId, "synthetic-reference"))
                .thenReturn(Optional.of(persisted));

        assertThatThrownBy(() -> service.appendObservation(observation("12.31", null, now.minusSeconds(60))))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("outra observação");
    }

    @Test
    void invalidPromotionAndFutureCollectionAreRejectedBeforePersistence() {
        assertThatThrownBy(() -> service.appendObservation(observation("10.00", "10.00", now)))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.appendObservation(observation("10.00", null, now.plusNanos(1))))
                .isInstanceOf(ApiException.class);

        verifyNoInteractions(prices, products, stores, sources);
    }

    @Test
    void monetaryPrecisionAndPositiveValuesAreValidatedBeforePersistence() {
        assertThatThrownBy(() -> service.appendObservation(observation("1.001", null, now)))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> service.appendObservation(observation("0.00", null, now)))
                .isInstanceOf(ConstraintViolationException.class);

        verifyNoInteractions(prices, products, stores, sources);
    }

    @Test
    void observationIdentityUsesPostgresqlMicrosecondPrecision() {
        PriceRecord first = PriceRecord.from(observation("12.3", null,
                now.minusSeconds(60).plusNanos(123_456_789)), now);
        PriceRecord replay = PriceRecord.from(observation("12.30", null,
                now.minusSeconds(60).plusNanos(123_456_000)), now.plusSeconds(1));

        assertThat(first.hasSameObservation(replay)).isTrue();
    }

    private PriceObservation observation(String regular, String promotion, Instant collectedAt) {
        return new PriceObservation(productId, storeId, sourceId, "synthetic-reference",
                new BigDecimal(regular), promotion == null ? null : new BigDecimal(promotion), "BRL",
                collectedAt, null, null, StockAvailability.UNKNOWN);
    }
}
