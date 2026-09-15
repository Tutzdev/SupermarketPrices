package br.com.supermercados.prices.store;

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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @GetMapping("/chains")
    public PageResponse<ChainResponse> findChains(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(storeService.findChains(
                PageRequests.create(page, size, Sort.by("name", "id"))));
    }

    @GetMapping("/stores")
    public PageResponse<StoreResponse> findStores(
            @RequestParam(required = false) UUID cityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(storeService.findActiveStores(
                cityId, PageRequests.create(page, size, Sort.by("name", "id"))));
    }

    @GetMapping("/stores/{id}")
    public StoreResponse findStore(@PathVariable UUID id) {
        return storeService.findStore(id);
    }
}
