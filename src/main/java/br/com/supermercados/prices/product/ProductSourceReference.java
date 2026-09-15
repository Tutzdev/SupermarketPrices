package br.com.supermercados.prices.product;

import java.time.Instant;
import java.util.UUID;

import br.com.supermercados.prices.datasource.SourceObservation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_source_references")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductSourceReference {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private UUID sourceId;

    @Column(nullable = false, length = 2048)
    private String sourceReference;

    @Column(nullable = false)
    private Instant collectedAt;

    @Version
    private long version;

    ProductSourceReference(UUID productId, SourceObservation observation) {
        id = UUID.randomUUID();
        this.productId = productId;
        sourceId = observation.sourceId();
        sourceReference = observation.sourceReference();
        collectedAt = observation.collectedAt();
    }

    void recordCollection(Instant collectionTime) {
        collectedAt = collectionTime;
    }
}
