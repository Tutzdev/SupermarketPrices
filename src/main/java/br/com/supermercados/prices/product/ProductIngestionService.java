package br.com.supermercados.prices.product;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.supermercados.prices.common.ApiException;
import br.com.supermercados.prices.datasource.DataSourceService;
import br.com.supermercados.prices.datasource.ObservationValidator;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class ProductIngestionService {

    private final ProductRepository productRepository;
    private final ProductSourceReferenceRepository referenceRepository;
    private final DataSourceService dataSourceService;
    private final ObservationValidator validator;
    private final Clock clock;

    @Transactional
    public ProductResponse ingest(ProductObservation observation) {
        return ingestWithOutcome(observation).product();
    }

    @Transactional
    public ProductIngestionResult ingestWithOutcome(ProductObservation observation) {
        validator.validate(observation);
        dataSourceService.verifyObservation(observation.source());

        String gtin = Gtin.normalize(observation.gtin());
        Optional<ProductSourceReference> existingReference = referenceRepository.findBySourceIdAndSourceReference(
                observation.source().sourceId(),
                observation.source().sourceReference());

        if (existingReference.isPresent()) {
            return updateReferencedProduct(existingReference.orElseThrow(), observation, gtin);
        }

        Optional<Product> identifiedProduct = findByGtin(gtin);
        Product product = identifiedProduct.orElseGet(
                () -> productRepository.save(new Product(observation, gtin, clock.instant())));
        referenceRepository.save(new ProductSourceReference(product.getId(), observation.source()));
        
        ProductIngestionOutcome outcome = identifiedProduct.isPresent()
                ? ProductIngestionOutcome.LINKED : ProductIngestionOutcome.CREATED;
        return new ProductIngestionResult(ProductResponse.from(product), outcome);
    }

    private ProductIngestionResult updateReferencedProduct(
            ProductSourceReference reference, ProductObservation observation, String gtin) {
        Product product = productRepository.findById(reference.getProductId())
                .orElseThrow(() -> new IllegalStateException("Product source reference has no product"));
        validateIdentity(product, gtin);

        if (!observation.source().collectedAt().isAfter(reference.getCollectedAt())) {
            return new ProductIngestionResult(ProductResponse.from(product), ProductIngestionOutcome.UNCHANGED);
        }

        product.assignGtin(gtin);

        if (product.getSourceId().equals(observation.source().sourceId())
                && product.getSourceReference().equals(observation.source().sourceReference())) {
            product.updateDetails(observation, clock.instant());
        }
        reference.recordCollection(observation.source().collectedAt());

        return new ProductIngestionResult(ProductResponse.from(product), ProductIngestionOutcome.UPDATED);
    }

    private void validateIdentity(Product product, String gtin) {
        if (gtin == null) { return; }

        boolean changedGtin = product.getGtin() != null && !Objects.equals(product.getGtin(), gtin);
        boolean anotherProductOwnsGtin = findByGtin(gtin)
                .filter(existing -> !existing.getId()
                .equals(product.getId()))
                .isPresent(
                );
                
        if (changedGtin || anotherProductOwnsGtin) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Identidade de produto conflitante; associação exige revisão da fonte");
        }
    }

    private Optional<Product> findByGtin(String gtin) {
        return gtin == null ? Optional.empty() : productRepository.findByGtin(gtin);
    }
}
