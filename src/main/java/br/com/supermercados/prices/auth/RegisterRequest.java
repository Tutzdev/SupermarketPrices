package br.com.supermercados.prices.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 12, max = 72) String password) {

    @Override
    public String toString() {
        return "RegisterRequest[redacted]";
    }
}
