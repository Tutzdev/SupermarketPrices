package br.com.supermercados.prices.product;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.supermercados.prices.common.ApiException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repository;

    public Page<ProductResponse> search(ProductSearch search, Pageable pageable) {
        return repository.findAll(search.specification(), pageable).map(ProductResponse::from);
    }

    public ProductResponse findProduct(UUID productId) {
        return ProductResponse.from(requireProduct(productId));
    }

    public Product requireProduct(UUID productId) {
        return repository.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Produto não encontrado"));
    }
}
