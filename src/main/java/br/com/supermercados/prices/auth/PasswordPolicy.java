package br.com.supermercados.prices.auth;

import br.com.supermercados.prices.common.ApiException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

    static final int BCRYPT_MAX_BYTES = 72;

    public void validate(String password) {
        if (password == null || password.isBlank() || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "A senha deve ter ao menos 12 caracteres e no máximo 72 bytes em UTF-8.");
        }
    }
}
