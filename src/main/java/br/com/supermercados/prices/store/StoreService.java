package br.com.supermercados.prices.store;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.supermercados.prices.common.ApiException;
import br.com.supermercados.prices.location.LocationService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;
    private final ChainRepository chainRepository;
    private final LocationService locationService;

    public Page<ChainResponse> findChains(Pageable pageable) {
        return chainRepository.findAll(pageable).map(ChainResponse::from);
    }

    public Page<StoreResponse> findActiveStores(UUID cityId, Pageable pageable) {
        if (cityId == null) {
            return storeRepository.findByActiveTrue(pageable).map(StoreResponse::from);
        }
        locationService.requireCity(cityId);
        return storeRepository.findByCityIdAndActiveTrue(cityId, pageable).map(StoreResponse::from);
    }

    public StoreResponse findStore(UUID storeId) {
        return StoreResponse.from(requireStore(storeId));
    }

    public Store requireStore(UUID storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Loja não encontrada"));
    }
}
