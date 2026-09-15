package br.com.supermercados.prices.location;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CityRepository extends JpaRepository<City, UUID> {

    Page<City> findByStateId(UUID stateId, Pageable pageable);
}
