package br.com.supermercados.prices.product;

import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import br.com.supermercados.prices.common.ApiException;

public record ProductSearch(String query, String brand, String gtin, String category) {

    public ProductSearch {
        query = normalize(query, 200);
        brand = normalize(brand, 120);
        gtin = Gtin.normalize(gtin);
        category = normalize(category, 120);
    }

    Specification<Product> specification() {
        return (root, criteriaQuery, builder) -> {
            var predicate = builder.conjunction();
            if (query != null) {
                String pattern = "%" + escapeLike(query) + "%";
                predicate = builder.and(predicate, builder.or(
                        builder.like(builder.lower(root.get("name")), pattern, '!'),
                        builder.like(builder.lower(root.get("brand")), pattern, '!'),
                        builder.like(builder.lower(root.get("description")), pattern, '!')));
            }
            if (brand != null) {predicate = builder.and(predicate, builder.equal(builder.lower(root.get("brand")), brand));}
            if (gtin != null) {predicate = builder.and(predicate, builder.equal(root.get("gtin"), gtin));}
            if (category != null) {predicate = builder.and(predicate, builder.equal(builder.lower(root.get("category")), category));}

            return predicate;
        };
    }

    private static String normalize(String value, int maxLength) {
        if (value == null || value.isBlank()) {return null;}
        if (value.length() > maxLength) {throw new ApiException(HttpStatus.BAD_REQUEST, "Filtro de busca excede o tamanho permitido");}
        
        return value.strip().toLowerCase(Locale.ROOT);
    }

    private static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
