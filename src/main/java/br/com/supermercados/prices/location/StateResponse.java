package br.com.supermercados.prices.location;

import java.util.UUID;

public record StateResponse(UUID id, String name, String code) {

    static StateResponse from(State state) {
        return new StateResponse(state.getId(), state.getName(), state.getCode());
    }
}
