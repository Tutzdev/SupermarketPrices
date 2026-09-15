package br.com.supermercados.prices.location;

import java.util.UUID;

public record CityResponse(UUID id, UUID stateId, String name) {

    static CityResponse from(City city) {
        return new CityResponse(city.getId(), city.getStateId(), city.getName());
    }
}
