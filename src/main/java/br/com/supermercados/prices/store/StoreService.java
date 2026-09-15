package br.com.supermercados.prices.store;

import br.com.supermercados.prices.common.ApiException;
import br.com.supermercados.prices.location.LocationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public List<StoreResponse> findAllActiveStores(UUID cityId, int maximumStores) {
        locationService.requireCity(cityId);
        PageRequest request = PageRequest.of(0, maximumStores, Sort.by("name", "id"));
        Page<Store> page = storeRepository.findByCityIdAndActiveTrue(cityId, request);
        if (page.getTotalElements() > maximumStores) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "A cidade possui mais lojas elegíveis que o limite seguro para uma recomendação completa.");
        }
        return page.getContent().stream().map(StoreResponse::from).toList();
    }

    public Store requireStore(UUID storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Loja não encontrada"));
    }
}
