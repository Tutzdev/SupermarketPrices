package br.com.supermercados.prices.store;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, UUID> {

    Page<Store> findByActiveTrue(Pageable pageable);

    Page<Store> findByCityIdAndActiveTrue(UUID cityId, Pageable pageable);

    Optional<Store> findBySourceIdAndSourceReference(UUID sourceId, String sourceReference);
}
