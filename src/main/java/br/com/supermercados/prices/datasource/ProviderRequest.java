package br.com.supermercados.prices.datasource;

public record ProviderRequest(String cursor, int limit) {

    public ProviderRequest {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Provider batch limit must be between 1 and 100");
        }
    }
}
