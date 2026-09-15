package br.com.supermercados.prices.auth;

import br.com.supermercados.prices.user.UserRole;
import java.util.UUID;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record AuthenticatedUser(UUID id, UserRole role, boolean emailVerified) {

    public AuthenticatedUser(UUID id) {
        this(id, UserRole.USER, false);
    }

    public SimpleGrantedAuthority authority() {
        return new SimpleGrantedAuthority("ROLE_" + role.name());
    }
}
