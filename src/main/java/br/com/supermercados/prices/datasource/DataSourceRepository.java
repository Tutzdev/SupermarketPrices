package br.com.supermercados.prices.datasource;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DataSourceRepository extends JpaRepository<DataSource, UUID> {

    boolean existsByCode(String code);

    Optional<DataSource> findByCode(String code);
}
