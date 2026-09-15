package br.com.supermercados.prices.product;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductSourceReferenceRepository extends JpaRepository<ProductSourceReference, UUID> {

    Optional<ProductSourceReference> findBySourceIdAndSourceReference(UUID sourceId, String sourceReference);
}
