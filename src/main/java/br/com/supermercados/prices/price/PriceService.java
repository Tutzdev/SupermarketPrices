package br.com.supermercados.prices.price;

import br.com.supermercados.prices.common.ApiException;
import br.com.supermercados.prices.common.PageResponse;
import br.com.supermercados.prices.datasource.DataSourceService;
import br.com.supermercados.prices.product.ProductService;
import br.com.supermercados.prices.store.StoreRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class PriceService {

    private final PriceRecordRepository prices;
    private final ProductService products;
    private final StoreRepository stores;
    private final DataSourceService sources;
    private final Validator validator;
    private final Clock clock;

    public PriceService(PriceRecordRepository prices, ProductService products, StoreRepository stores,
                        DataSourceService sources, Validator validator, Clock clock) {
        this.prices = prices;
        this.products = products;
        this.stores = stores;
        this.sources = sources;
        this.validator = validator;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<PriceRecordResponse> findHistory(UUID productId, UUID storeId, Pageable pageable) {
        requireProductAndStore(productId, storeId);
        return PageResponse.from(prices.findByProductIdAndStoreId(productId, storeId, pageable)
                .map(PriceRecordResponse::from));
    }

    /** Internal ingestion boundary. Reusing a source reference with changed content is a conflict. */
    @Transactional
    public PriceRecordResponse appendObservation(PriceObservation observation) {
        Instant now = clock.instant();
        validateObservation(observation, now);
        requireProductAndStore(observation.productId(), observation.storeId());
        sources.requireEnabledSource(observation.sourceId());

        PriceRecord candidate = PriceRecord.from(observation, now);
        prices.insertIfAbsent(candidate);
        PriceRecord persisted = prices.findBySourceIdAndSourceReference(
                candidate.getSourceId(), candidate.getSourceReference()).orElseThrow();
        if (!persisted.hasSameObservation(candidate)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "A referência da fonte já identifica outra observação de preço.");
        }
        return PriceRecordResponse.from(persisted);
    }

    private void requireProductAndStore(UUID productId, UUID storeId) {
        products.requireProduct(productId);
        if (!stores.existsById(storeId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Loja não encontrada.");
        }
    }

    private void validateObservation(PriceObservation observation, Instant now) {
        if (observation == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A observação de preço é obrigatória.");
        }
        var violations = validator.validate(observation);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        if (observation.collectedAt().isAfter(now)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A coleta não pode ocorrer no futuro.");
        }
        if (observation.promotionalPrice() != null
                && observation.promotionalPrice().compareTo(observation.regularPrice()) >= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "O preço promocional deve ser menor que o preço regular.");
        }
        if (observation.promotionalPrice() == null && observation.promotionValidUntil() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "A validade promocional exige um preço promocional.");
        }
        requireValidPeriod(observation.collectedAt(), observation.validUntil());
        requireValidPeriod(observation.collectedAt(), observation.promotionValidUntil());
    }

    private void requireValidPeriod(Instant collectedAt, Instant validUntil) {
        if (validUntil != null && !validUntil.truncatedTo(ChronoUnit.MICROS)
                .isAfter(collectedAt.truncatedTo(ChronoUnit.MICROS))) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "A validade deve ser posterior à data de coleta.");
        }
    }
}
