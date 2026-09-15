package br.com.supermercados.prices.datasource;

import java.util.UUID;

import br.com.supermercados.prices.store.ChainObservation;
import br.com.supermercados.prices.store.StoreObservation;

/** Each observation must retain the adapter's source ID and a verifiable source reference. */
public interface StoreDataProvider {

    UUID sourceId();

    ProviderResult<ChainObservation> fetchChains(ProviderRequest request);

    ProviderResult<StoreObservation> fetchStores(ProviderRequest request);
}
