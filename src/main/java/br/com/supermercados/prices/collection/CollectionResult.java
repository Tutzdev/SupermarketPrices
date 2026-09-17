package br.com.supermercados.prices.collection;

import java.util.UUID;

public record CollectionResult(
        UUID sourceId,
        UUID storeId,
        int foundCount,
        int createdCount,
        int updatedCount,
        int skippedCount,
        int errorCount,
        String errorMessage) {
}
