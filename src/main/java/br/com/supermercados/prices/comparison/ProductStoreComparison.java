package br.com.supermercados.prices.comparison;

import br.com.supermercados.prices.price.PriceQuote;

import java.util.UUID;

public record ProductStoreComparison(UUID storeId, String storeName, PriceQuote price) {
}
