package br.com.supermercados.prices.product;

import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.supermercados.prices.common.PageRequests;
import br.com.supermercados.prices.common.PageResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public PageResponse<ProductResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String gtin,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(productService.search(new ProductSearch(query, brand, gtin, category),
                PageRequests.create(page, size, Sort.by("name", "id"))));
    }

    @GetMapping("/{id}")
    public ProductResponse findProduct(@PathVariable UUID id) {
        return productService.findProduct(id);
    }
}
