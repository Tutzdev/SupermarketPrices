package br.com.supermercados.prices.price;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PriceRecordRepository extends Repository<PriceRecord, UUID> {

    Page<PriceRecord> findByProductIdAndStoreId(UUID productId, UUID storeId, Pageable pageable);

    Optional<PriceRecord> findBySourceIdAndSourceReference(UUID sourceId, String sourceReference);

    @Query(value = """
            SELECT DISTINCT ON (store_id, product_id) *
            FROM price_records
            WHERE store_id IN (:storeIds) AND product_id IN (:productIds)
            ORDER BY store_id, product_id, collected_at DESC, recorded_at DESC, id DESC
            """, nativeQuery = true)
    List<PriceRecord> findLatestForStoresAndProducts(
            @Param("storeIds") Collection<UUID> storeIds,
            @Param("productIds") Collection<UUID> productIds);

    @Modifying
    @Query(value = """
            INSERT INTO price_records (
                id, product_id, store_id, source_id, source_reference,
                regular_price, promotional_price, currency, collected_at, recorded_at,
                valid_until, promotion_valid_until, availability
            ) VALUES (
                :#{#record.id}, :#{#record.productId}, :#{#record.storeId},
                :#{#record.sourceId}, :#{#record.sourceReference},
                :#{#record.regularPrice}, :#{#record.promotionalPrice}, :#{#record.currency},
                :#{#record.collectedAt}, :#{#record.recordedAt}, :#{#record.validUntil},
                :#{#record.promotionValidUntil}, :#{#record.availability.name()}
            ) ON CONFLICT (source_id, source_reference) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("record") PriceRecord record);
}
