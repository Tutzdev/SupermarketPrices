package br.com.supermercados.prices.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.supermercados.prices.common.ApiException;
import br.com.supermercados.prices.datasource.DataSourceService;
import br.com.supermercados.prices.datasource.ObservationValidator;
import br.com.supermercados.prices.datasource.SourceObservation;
import br.com.supermercados.prices.location.LocationService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

class StoreIngestionServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-10T12:00:00Z");
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();

    private final StoreRepository stores = mock(StoreRepository.class);
    private final ChainRepository chains = mock(ChainRepository.class);
    private final LocationService locations = mock(LocationService.class);
    private final DataSourceService sources = mock(DataSourceService.class);
    private final UUID chainId = UUID.randomUUID();
    private final UUID cityId = UUID.randomUUID();
    private final UUID sourceId = UUID.randomUUID();
    private StoreIngestionService service;

    @BeforeEach
    void setUp() {
        service = new StoreIngestionService(stores, chains, locations, sources,
                new ObservationValidator(VALIDATOR_FACTORY.getValidator()), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @AfterAll
    static void closeValidationFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void cityBasedStoreDoesNotRequireCoordinatesOrAddress() {
        when(chains.existsById(chainId)).thenReturn(true);
        when(stores.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StoreResponse result = service.ingestStore(observation(null, null, NOW));

        assertThat(result.cityId()).isEqualTo(cityId);
        assertThat(result.latitude()).isNull();
        assertThat(result.longitude()).isNull();
        assertThat(result.address()).isNull();
        verify(locations).requireCity(cityId);
    }

    @Test
    void rejectsIncompleteCoordinatePair() {
        when(chains.existsById(chainId)).thenReturn(true);

        assertThatThrownBy(() -> service.ingestStore(observation(BigDecimal.ONE, null, NOW)))
                .isInstanceOf(ApiException.class).hasMessageContaining("juntas");
        verify(stores, never()).save(any());
    }

    @Test
    void rejectsCoordinatesOutsideGeographicBounds() {
        assertThatThrownBy(() -> service.ingestStore(observation(new BigDecimal("91"), BigDecimal.ZERO, NOW)))
                .isInstanceOf(ConstraintViolationException.class);
        verify(stores, never()).save(any());
    }

    @Test
    void rejectsMissingChain() {
        assertThatThrownBy(() -> service.ingestStore(observation(null, null, NOW)))
                .isInstanceOf(ApiException.class).hasMessageContaining("Rede");
        verify(stores, never()).save(any());
    }

    @Test
    void oldObservationDoesNotOverwriteNewerLocationData() {
        when(chains.existsById(chainId)).thenReturn(true);
        Store existing = new Store(observation(BigDecimal.ONE, BigDecimal.ONE, NOW), NOW);
        when(stores.findBySourceIdAndSourceReference(sourceId, "synthetic-store"))
                .thenReturn(Optional.of(existing));

        StoreResponse result = service.ingestStore(observation(null, null, NOW.minusSeconds(60)));

        assertThat(result.collectedAt()).isEqualTo(NOW);
        assertThat(result.latitude()).isEqualByComparingTo(BigDecimal.ONE);
    }

    private StoreObservation observation(BigDecimal latitude, BigDecimal longitude, Instant collectedAt) {
        return new StoreObservation(chainId, cityId, "Synthetic test store", null, latitude, longitude, true,
                new SourceObservation(sourceId, "synthetic-store", collectedAt));
    }
}
