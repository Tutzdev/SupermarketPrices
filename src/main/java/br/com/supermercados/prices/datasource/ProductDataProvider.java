package br.com.supermercados.prices.datasource;

import java.util.UUID;

import br.com.supermercados.prices.product.ProductObservation;

//Adapters must validate source responses, honor the batch limit and configure finite I/O timeouts.
public interface ProductDataProvider {

    UUID sourceId();

    ProviderResult<ProductObservation> fetchProducts(ProviderRequest request);
}
