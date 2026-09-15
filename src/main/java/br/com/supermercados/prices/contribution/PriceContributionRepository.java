package br.com.supermercados.prices.contribution;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PriceContributionRepository extends JpaRepository<PriceContribution, UUID> {

    Page<PriceContribution> findByContributorId(UUID contributorId, Pageable pageable);

    Optional<PriceContribution> findByIdAndContributorId(UUID id, UUID contributorId);

    Page<PriceContribution> findByStatus(ContributionStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select contribution from PriceContribution contribution where contribution.id = :id")
    Optional<PriceContribution> findByIdForUpdate(@Param("id") UUID id);
}
