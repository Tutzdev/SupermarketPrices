package br.com.supermercados.prices.store;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "supermarket_chains")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupermarketChain {

    @Id
    private UUID id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false)
    private UUID sourceId;

    @Column(nullable = false, length = 2048)
    private String sourceReference;

    @Column(nullable = false)
    private Instant collectedAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    SupermarketChain(ChainObservation observation, Instant now) {
        id = UUID.randomUUID();
        sourceId = observation.source().sourceId();
        sourceReference = observation.source().sourceReference();
        update(observation, now);
    }

    void update(ChainObservation observation, Instant now) {
        name = observation.name().strip();
        collectedAt = observation.source().collectedAt();
        updatedAt = now;
    }
}
