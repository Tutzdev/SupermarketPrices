package br.com.supermercados.prices.comparison;

import br.com.supermercados.prices.common.PageResponse;
import br.com.supermercados.prices.location.LocationService;
import br.com.supermercados.prices.price.PricePolicy;
import br.com.supermercados.prices.price.PriceRecord;
import br.com.supermercados.prices.price.PriceRecordRepository;
import br.com.supermercados.prices.product.ProductService;
import br.com.supermercados.prices.shoppinglist.ShoppingListItemResponse;
import br.com.supermercados.prices.shoppinglist.ShoppingListService;
import br.com.supermercados.prices.store.StoreResponse;
import br.com.supermercados.prices.store.StoreService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ShoppingComparisonService {

    private final LocationService locations;
    private final StoreService stores;
    private final ProductService products;
    private final ShoppingListService shoppingLists;
    private final PriceRecordRepository prices;
    private final PricePolicy pricePolicy;
    private final ShoppingPriceCalculator calculator;
    private final Clock clock;
    private final int maximumRecommendationStores;

    public ShoppingComparisonService(LocationService locations, StoreService stores, ProductService products,
            ShoppingListService shoppingLists, PriceRecordRepository prices,
            PricePolicy pricePolicy, ShoppingPriceCalculator calculator, Clock clock,
            @Value("${app.recommendations.max-stores:500}") int maximumRecommendationStores) {
        if (maximumRecommendationStores < 1 || maximumRecommendationStores > 5000) {
            throw new IllegalArgumentException("app.recommendations.max-stores must be between 1 and 5000");
        }
        this.locations = locations;
        this.stores = stores;
        this.products = products;
        this.shoppingLists = shoppingLists;
        this.prices = prices;
        this.pricePolicy = pricePolicy;
        this.calculator = calculator;
        this.clock = clock;
        this.maximumRecommendationStores = maximumRecommendationStores;
    }

    public ProductComparisonResponse compareProduct(UUID productId, UUID cityId, Pageable pageable) {
        var product = products.requireProduct(productId);
        locations.requireCity(cityId);
        Instant comparedAt = clock.instant();
        Page<StoreResponse> availableStores = stores.findActiveStores(cityId, pageable);
        Map<UUID, Map<UUID, PriceRecord>> latest = latestPrices(availableStores, List.of(productId));
        Page<ProductStoreComparison> comparisons = availableStores.map(store ->
                new ProductStoreComparison(store.id(), store.name(), pricePolicy.quote(
                        latest.getOrDefault(store.id(), Map.of()).get(productId), comparedAt)));
        return new ProductComparisonResponse(productId, product.getName(), cityId, "BRL", comparedAt,
                PageResponse.from(comparisons));
    }

    public ShoppingListComparisonResponse compareShoppingList(
            UUID userId, UUID listId, UUID cityId, Pageable pageable) {
        var shoppingList = shoppingLists.getOwnedList(userId, listId);
        locations.requireCity(cityId);
        Instant comparedAt = clock.instant();
        Page<StoreResponse> availableStores = stores.findActiveStores(cityId, pageable);
        List<UUID> productIds = shoppingList.items().stream()
                .map(ShoppingListItemResponse::productId).toList();
        Map<UUID, Map<UUID, PriceRecord>> latest = latestPrices(availableStores, productIds);
        Page<ShoppingStoreComparison> comparisons = availableStores.map(store -> calculator.calculate(
                store.id(), store.name(), shoppingList.items(),
                latest.getOrDefault(store.id(), Map.of()), comparedAt));
        return new ShoppingListComparisonResponse(listId, cityId, "BRL", comparedAt,
                PageResponse.from(comparisons));
    }

    public ShoppingRecommendationResponse recommendShoppingList(UUID userId, UUID listId, UUID cityId) {
        var shoppingList = shoppingLists.getOwnedList(userId, listId);
        Instant comparedAt = clock.instant();
        List<StoreResponse> availableStores = stores.findAllActiveStores(cityId, maximumRecommendationStores);
        List<UUID> productIds = shoppingList.items().stream()
                .map(ShoppingListItemResponse::productId).toList();
        Map<UUID, Map<UUID, PriceRecord>> latest = latestPrices(availableStores, productIds);
        List<StoreRecommendationCandidate> candidates = availableStores.stream()
                .map(store -> calculator.calculate(store.id(), store.name(), shoppingList.items(),
                        latest.getOrDefault(store.id(), Map.of()), comparedAt))
                .map(StoreRecommendationCandidate::from)
                .toList();

        Comparator<StoreRecommendationCandidate> completeOrder = Comparator
                .comparing(StoreRecommendationCandidate::total)
                .thenComparing(StoreRecommendationCandidate::storeName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(StoreRecommendationCandidate::storeId);
        StoreRecommendationCandidate recommendation = candidates.stream()
                .filter(StoreRecommendationCandidate::completeShoppingList)
                .min(completeOrder)
                .orElse(null);
        if (recommendation != null) {
            return new ShoppingRecommendationResponse(listId, cityId, "BRL", comparedAt, candidates.size(),
                    RecommendationStatus.COMPLETE_STORE_FOUND, recommendation, List.of());
        }

        int bestCoverage = candidates.stream().mapToInt(StoreRecommendationCandidate::pricedItems).max().orElse(0);
        Comparator<StoreRecommendationCandidate> coverageOrder = Comparator
                .comparing(StoreRecommendationCandidate::total,
                        Comparator.nullsLast(BigDecimal::compareTo))
                .thenComparing(StoreRecommendationCandidate::storeName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(StoreRecommendationCandidate::storeId);
        List<StoreRecommendationCandidate> closestMatches = candidates.stream()
                .filter(candidate -> candidate.pricedItems() == bestCoverage)
                .sorted(coverageOrder)
                .limit(10)
                .toList();
        return new ShoppingRecommendationResponse(listId, cityId, "BRL", comparedAt, candidates.size(),
                RecommendationStatus.NO_COMPLETE_STORE, null, closestMatches);
    }

    private Map<UUID, Map<UUID, PriceRecord>> latestPrices(Page<StoreResponse> availableStores,
            List<UUID> productIds) {
        return latestPrices(availableStores.getContent(), productIds);
    }

    private Map<UUID, Map<UUID, PriceRecord>> latestPrices(
            List<StoreResponse> availableStores, List<UUID> productIds) {
        if (availableStores.isEmpty() || productIds.isEmpty()) {
            return Map.of();
        }
        List<UUID> storeIds = availableStores.stream().map(StoreResponse::id).toList();
        return prices.findLatestForStoresAndProducts(storeIds, productIds).stream()
                .collect(Collectors.groupingBy(PriceRecord::getStoreId,
                        Collectors.toMap(PriceRecord::getProductId, Function.identity())));
    }
}
