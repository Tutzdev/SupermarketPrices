package br.com.supermercados.prices.collection.nagumo;

import java.util.List;

import tools.jackson.databind.JsonNode;

record NagumoCatalogResponse(
        JsonNode store,
        String categoryName,
        int foundCount,
        List<JsonNode> products) {

    NagumoCatalogResponse {
        products = List.copyOf(products);
    }
}
