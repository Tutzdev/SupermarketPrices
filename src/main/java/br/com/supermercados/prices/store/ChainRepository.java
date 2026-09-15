package br.com.supermercados.prices.store;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChainRepository extends JpaRepository<SupermarketChain, UUID> {

    Optional<SupermarketChain> findBySourceIdAndSourceReference(UUID sourceId, String sourceReference);
}
