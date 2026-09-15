package br.com.supermercados.prices.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token,
        @NotBlank @Size(min = 12, max = 72) String password) {

    @Override
    public String toString() {
        return "PasswordResetConfirmRequest[redacted]";
    }
}
