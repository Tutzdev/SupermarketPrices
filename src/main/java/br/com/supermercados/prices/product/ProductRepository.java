package br.com.supermercados.prices.product;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByGtin(String gtin);

    java.util.List<Product> findByNormalizedBrandAndUnitAndQuantity(String brand, String unit,
            java.math.BigDecimal quantity, org.springframework.data.domain.Pageable pageable);
}
