package br.com.supermercados.prices.comparison;

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

import br.com.supermercados.prices.common.PageResponse;
import br.com.supermercados.prices.location.LocationService;
import br.com.supermercados.prices.price.PricePolicy;
import br.com.supermercados.prices.price.MeasurementPrice;
import br.com.supermercados.prices.price.PriceRecord;
import br.com.supermercados.prices.price.PriceRecordRepository;
import br.com.supermercados.prices.product.ProductService;
import br.com.supermercados.prices.shoppinglist.ShoppingListItemResponse;
import br.com.supermercados.prices.shoppinglist.ShoppingListService;
import br.com.supermercados.prices.store.StoreResponse;
import br.com.supermercados.prices.store.StoreService;

@Service
@Transactional(readOnly = true)
public class ShoppingComparisonService {

    private static final String CURRENCY = "BRL";
    private static final int MAXIMUM_ALLOWED_RECOMMENDATION_STORES = 5000;
    private static final int MAXIMUM_CLOSEST_MATCHES = 10;

    private final LocationService locations;
    private final StoreService stores;
    private final ProductService products;
    private final ShoppingListService shoppingLists;
    private final PriceRecordRepository prices;
    private final PricePolicy pricePolicy;
    private final ShoppingPriceCalculator calculator;
    private final Clock clock;
    private final int maximumRecommendationStores;

    public ShoppingComparisonService(
            LocationService locations,
            StoreService stores,
            ProductService products,
            ShoppingListService shoppingLists,
            PriceRecordRepository prices,
            PricePolicy pricePolicy,
            ShoppingPriceCalculator calculator,
            Clock clock,
            @Value("${app.recommendations.max-stores:500}") int maximumRecommendationStores) {
        if (maximumRecommendationStores < 1
                || maximumRecommendationStores > MAXIMUM_ALLOWED_RECOMMENDATION_STORES) {
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
        Map<UUID, Map<UUID, PriceRecord>> latestPricesByStore = latestPrices(
                availableStores, List.of(productId));
        Page<ProductStoreComparison> comparisons = availableStores.map(store -> {
            var quote = pricePolicy.quote(latestPricesByStore.getOrDefault(store.id(), Map.of()).get(productId), comparedAt);
            return new ProductStoreComparison(store.id(), store.name(), quote,
                    MeasurementPrice.calculate(quote.unitPrice(), product.getQuantity(), product.getUnit()));
        });

        return new ProductComparisonResponse(productId, product.getName(), cityId, CURRENCY, comparedAt,
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
        Map<UUID, Map<UUID, PriceRecord>> latestPricesByStore = latestPrices(availableStores, productIds);
        Page<ShoppingStoreComparison> comparisons = availableStores.map(store -> calculator.calculate(
                store.id(), store.name(), shoppingList.items(),
                latestPricesByStore.getOrDefault(store.id(), Map.of()), comparedAt));

        return new ShoppingListComparisonResponse(listId, cityId, CURRENCY, comparedAt,
                PageResponse.from(comparisons));
    }

    public ShoppingRecommendationResponse recommendShoppingList(UUID userId, UUID listId, UUID cityId) {
        var shoppingList = shoppingLists.getOwnedList(userId, listId);
        Instant comparedAt = clock.instant();
        List<StoreResponse> availableStores = stores.findAllActiveStores(cityId, maximumRecommendationStores);
        List<UUID> productIds = shoppingList.items().stream()
                .map(ShoppingListItemResponse::productId).toList();
        Map<UUID, Map<UUID, PriceRecord>> latestPricesByStore = latestPrices(availableStores, productIds);
        List<ShoppingStoreComparison> comparisons = availableStores.stream()
                .map(store -> calculator.calculate(store.id(), store.name(), shoppingList.items(),
                        latestPricesByStore.getOrDefault(store.id(), Map.of()), comparedAt))
                .toList();
        List<StoreRecommendationCandidate> candidates = comparisons.stream()
                .map(StoreRecommendationCandidate::from).toList();

        StoreRecommendationCandidate recommendation = findCompleteRecommendation(candidates);
        ShoppingCombinationResponse combination = calculator.combine(
                shoppingList.items(), comparisons, recommendation);
        if (recommendation != null) {
            return new ShoppingRecommendationResponse(listId, cityId, CURRENCY, comparedAt, candidates.size(),
                    RecommendationStatus.COMPLETE_STORE_FOUND, recommendation, List.of(), combination);
        }

        List<StoreRecommendationCandidate> closestMatches = findClosestMatches(candidates);
        return new ShoppingRecommendationResponse(listId, cityId, CURRENCY, comparedAt, candidates.size(),
                RecommendationStatus.NO_COMPLETE_STORE, null, closestMatches, combination);
    }

    private StoreRecommendationCandidate findCompleteRecommendation(
            List<StoreRecommendationCandidate> candidates) {
        Comparator<StoreRecommendationCandidate> completeOrder = Comparator
                .comparing(StoreRecommendationCandidate::total)
                .thenComparing(StoreRecommendationCandidate::storeName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(StoreRecommendationCandidate::storeId);
        return candidates.stream()
                .filter(StoreRecommendationCandidate::completeShoppingList)
                .min(completeOrder)
                .orElse(null);
    }

    private List<StoreRecommendationCandidate> findClosestMatches(
            List<StoreRecommendationCandidate> candidates) {
        int bestCoverage = candidates.stream().mapToInt(StoreRecommendationCandidate::pricedItems).max().orElse(0);
        if (bestCoverage == 0) {
            return List.of();
        }
        Comparator<StoreRecommendationCandidate> coverageOrder = Comparator
                .comparing(StoreRecommendationCandidate::total,
                        Comparator.nullsLast(BigDecimal::compareTo))
                .thenComparing(StoreRecommendationCandidate::storeName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(StoreRecommendationCandidate::storeId);
        return candidates.stream()
                .filter(candidate -> candidate.pricedItems() == bestCoverage)
                .sorted(coverageOrder)
                .limit(MAXIMUM_CLOSEST_MATCHES)
                .toList();
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
