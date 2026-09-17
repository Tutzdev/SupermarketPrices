package br.com.supermercados.prices.collection;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import br.com.supermercados.prices.common.ApiException;

@Service
public class CollectionCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectionCoordinator.class);

    private final List<SupermarketCollector> collectors;
    private final CollectedCatalogIngestionService ingestion;
    private final CollectionRunService runs;
    private final Duration collectorInterval;
    private final AtomicBoolean running = new AtomicBoolean();

    public CollectionCoordinator(
            List<SupermarketCollector> collectors,
            CollectedCatalogIngestionService ingestion,
            CollectionRunService runs,
            @Value("${app.collection.collector-interval:PT2S}") Duration collectorInterval) {
        if (collectorInterval.isNegative()) {
            throw new IllegalArgumentException("Intervalo entre coletores não pode ser negativo");
        }
        this.collectors = List.copyOf(collectors);
        this.ingestion = ingestion;
        this.runs = runs;
        this.collectorInterval = collectorInterval;
    }

    public List<CollectionRunResponse> collectAll() {
        if (!running.compareAndSet(false, true)) {
            throw new ApiException(HttpStatus.CONFLICT, "Já existe uma coleta em andamento");
        }

        try {
            List<CollectionRunResponse> results = new ArrayList<>();
            for (int index = 0; index < collectors.size(); index++) {
                if (index > 0 && !waitBeforeNextCollector()) {
                    break;
                }
                results.add(collect(collectors.get(index)));
            }
            return List.copyOf(results);
        } finally {
            running.set(false);
        }
    }

    private CollectionRunResponse collect(SupermarketCollector collector) {
        CollectorMetadata metadata = collector.metadata();
        CollectionRunResponse run = runs.start(metadata);
        LOGGER.info("Coleta {} iniciada para {}", run.id(), metadata.code());

        try {
            CollectedCatalog catalog = collector.collect();
            CollectionRunResponse completed = runs.finish(
                    run.id(), ingestion.ingest(metadata, catalog));
            LOGGER.info("Coleta {} finalizada para {}: status={}, encontrados={}, criados={}, "
                            + "atualizados={}, ignorados={}, erros={}",
                    completed.id(), metadata.code(), completed.status(), completed.foundCount(),
                    completed.createdCount(), completed.updatedCount(), completed.skippedCount(),
                    completed.errorCount());
            return completed;
        } catch (RuntimeException exception) {
            String message = exception.getMessage() == null
                    ? exception.getClass().getSimpleName() : exception.getMessage();
            CollectionRunResponse failed = runs.fail(run.id(), message);
            LOGGER.error("Coleta {} falhou para {}: {}", failed.id(), metadata.code(), message, exception);
            return failed;
        }
    }

    private boolean waitBeforeNextCollector() {
        try {
            Thread.sleep(collectorInterval.toMillis());
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Execução de coletores interrompida antes da próxima integração");
            return false;
        }
    }
}
