package br.com.supermercados.prices.preference;

import java.util.List;
import java.util.UUID;

public record UserPreferenceResponse(UUID preferredCityId, List<UUID> favoriteStoreIds) {

    public UserPreferenceResponse {
        favoriteStoreIds = List.copyOf(favoriteStoreIds);
    }
}
