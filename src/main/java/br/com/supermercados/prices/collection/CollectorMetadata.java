package br.com.supermercados.prices.collection;

import java.time.Instant;
import java.util.UUID;

public record CollectorMetadata(
        String code,
        String supermarketName,
        String storeName,
        String sourceCode,
        String sourceName,
        String sourceBaseUrl,
        Instant sourceVerifiedAt,
        String chainName,
        String chainSourceReference,
        String storeSourceReference,
        UUID cityId) {
}
