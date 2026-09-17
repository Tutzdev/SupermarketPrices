package br.com.supermercados.prices.collection;

import java.time.Instant;
import java.util.UUID;

public record CollectionRunResponse(
        UUID id,
        String collectorCode,
        String supermarketName,
        String storeName,
        UUID sourceId,
        UUID storeId,
        Instant startedAt,
        Instant finishedAt,
        CollectionStatus status,
        int foundCount,
        int createdCount,
        int updatedCount,
        int skippedCount,
        int errorCount,
        String errorMessage) {

    static CollectionRunResponse from(CollectionRun run) {
        return new CollectionRunResponse(run.getId(), run.getCollectorCode(),
                run.getSupermarketName(), run.getStoreName(), run.getSourceId(), run.getStoreId(),
                run.getStartedAt(), run.getFinishedAt(), run.getStatus(), run.getFoundCount(),
                run.getCreatedCount(), run.getUpdatedCount(), run.getSkippedCount(),
                run.getErrorCount(), run.getErrorMessage());
    }
}
