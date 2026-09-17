package br.com.supermercados.prices.collection;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.supermercados.prices.datasource.DataSource;
import br.com.supermercados.prices.datasource.DataSourceService;
import br.com.supermercados.prices.datasource.SourceObservation;
import br.com.supermercados.prices.datasource.SourceRegistration;
import br.com.supermercados.prices.store.ChainObservation;
import br.com.supermercados.prices.store.ChainResponse;
import br.com.supermercados.prices.store.StoreIngestionService;
import br.com.supermercados.prices.store.StoreObservation;
import br.com.supermercados.prices.store.StoreResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CollectionCatalogService {

    private final DataSourceService sources;
    private final StoreIngestionService stores;

    @Transactional
    public CollectionCatalog ensureCatalog(
            CollectorMetadata metadata, CollectedStore collectedStore, Instant collectedAt) {
        DataSource source = sources.findByCode(metadata.sourceCode())
                .orElseGet(() -> sources.registerVerifiedSource(new SourceRegistration(
                        metadata.sourceCode(), metadata.sourceName(), metadata.sourceBaseUrl(),
                        metadata.sourceVerifiedAt())));
        sources.requireEnabledSource(source.getId());

        SourceObservation chainSource = new SourceObservation(
                source.getId(), metadata.chainSourceReference(), collectedAt);
        ChainResponse chain = stores.ingestChain(new ChainObservation(metadata.chainName(), chainSource));

        SourceObservation storeSource = new SourceObservation(
                source.getId(), metadata.storeSourceReference(), collectedAt);
        StoreResponse store = stores.ingestStore(new StoreObservation(
                chain.id(), metadata.cityId(), collectedStore.name(), collectedStore.address(),
                collectedStore.latitude(), collectedStore.longitude(), collectedStore.active(), storeSource));

        return new CollectionCatalog(source.getId(), store.id());
    }
}
