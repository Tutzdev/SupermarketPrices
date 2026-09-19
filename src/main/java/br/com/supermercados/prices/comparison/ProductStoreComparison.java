package br.com.supermercados.prices.comparison;

import java.util.UUID;

import br.com.supermercados.prices.price.PriceQuote;
import br.com.supermercados.prices.price.MeasurementPrice;

public record ProductStoreComparison(UUID storeId, String storeName, PriceQuote price, MeasurementPrice measurementPrice) {
}
