package br.com.supermercados.prices.datasource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import br.com.supermercados.prices.common.ApiException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

class DataSourceServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-10T12:00:00Z");
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();

    private final DataSourceRepository repository = mock(DataSourceRepository.class);
    private final DataSourceService service = new DataSourceService(repository,
            new ObservationValidator(VALIDATOR_FACTORY.getValidator()), Clock.fixed(NOW, ZoneOffset.UTC));

    @AfterAll
    static void closeValidationFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void rejectsUnknownSource() {
        assertThatThrownBy(() -> service.verifyObservation(new SourceObservation(UUID.randomUUID(), "item", NOW)))
                .isInstanceOf(ApiException.class).hasMessageContaining("não encontrada");
    }

    @Test
    void rejectsObservationFromDisabledSource() {
        UUID sourceId = UUID.randomUUID();
        DataSource source = mock(DataSource.class);
        when(repository.findById(sourceId)).thenReturn(Optional.of(source));

        assertThatThrownBy(() -> service.verifyObservation(new SourceObservation(sourceId, "synthetic-item", NOW)))
                .isInstanceOf(ApiException.class).hasMessageContaining("desabilitada");
    }

    @Test
    void rejectsFutureCollectionTime() {
        UUID sourceId = UUID.randomUUID();
        DataSource source = mock(DataSource.class);
        when(source.isEnabled()).thenReturn(true);
        when(repository.findById(sourceId)).thenReturn(Optional.of(source));

        assertThatThrownBy(() -> service.verifyObservation(
                new SourceObservation(sourceId, "synthetic-item", NOW.plusSeconds(1))))
                .isInstanceOf(ApiException.class).hasMessageContaining("futuro");
    }

    @Test
    void rejectsMissingProvenance() {
        assertThatThrownBy(() -> service.verifyObservation(new SourceObservation(UUID.randomUUID(), " ", NOW)))
                .isInstanceOf(ConstraintViolationException.class);
        verify(repository, never()).findById(any());
    }

    @Test
    void rejectsCredentialsInsideSourceUrl() {
        SourceRegistration registration = new SourceRegistration("synthetic-test", "Synthetic test source",
                "https://user:password@example.invalid", NOW);

        assertThatThrownBy(() -> service.registerVerifiedSource(registration))
                .isInstanceOf(ApiException.class).hasMessageContaining("credenciais");
        verify(repository, never()).save(any());
    }
}
