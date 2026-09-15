package br.com.supermercados.prices.preference;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface FavoriteStoreRepository extends JpaRepository<FavoriteStore, UUID> {

    List<FavoriteStore> findByUserIdOrderByCreatedAtAscIdAsc(UUID userId);

    boolean existsByUserIdAndStoreId(UUID userId, UUID storeId);

    void deleteByUserIdAndStoreId(UUID userId, UUID storeId);
}
