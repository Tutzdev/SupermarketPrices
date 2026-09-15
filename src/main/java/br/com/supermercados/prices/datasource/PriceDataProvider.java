package br.com.supermercados.prices.datasource;

import br.com.supermercados.prices.price.PriceObservation;

import java.util.UUID;

/**
 * Contract for verified, legally accessible price sources. Adapters must enforce timeouts,
 * validate upstream responses and propagate failures instead of returning fabricated prices.
 * Every observation must use this provider's source ID and a stable observation reference.
 */
public interface PriceDataProvider {

    UUID sourceId();

    ProviderResult<PriceObservation> fetchPrices(ProviderRequest request);
}
