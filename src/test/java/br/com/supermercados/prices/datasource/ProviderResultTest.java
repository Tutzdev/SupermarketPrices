package br.com.supermercados.prices.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class ProviderResultTest {

    @Test
    void distinguishesUnavailableSourceFromSuccessfulEmptyResult() {
        ProviderResult<String> unavailable = ProviderResult.unavailable("Source has no accessible endpoint");

        assertThat(unavailable.status()).isEqualTo(ProviderResult.Status.UNAVAILABLE);
        assertThat(unavailable.items()).isEmpty();
        assertThat(unavailable.message()).isNotBlank();
        assertThat(ProviderResult.available(List.of(), null).status()).isEqualTo(ProviderResult.Status.AVAILABLE);
    }

    @Test
    void refusesFabricatedRecordsAlongsideUnavailability() {
        assertThatThrownBy(() -> new ProviderResult<>(ProviderResult.Status.UNAVAILABLE,
                List.of("synthetic record"), null, "Unavailable"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void boundsCollectionSize() {
        assertThatThrownBy(() -> ProviderResult.available(IntStream.range(0, 101).boxed().toList(), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProviderRequest(null, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProviderRequest(null, 101)).isInstanceOf(IllegalArgumentException.class);
    }
}
