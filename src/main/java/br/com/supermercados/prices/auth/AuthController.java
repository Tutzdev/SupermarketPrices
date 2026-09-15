package br.com.supermercados.prices.auth;

import br.com.supermercados.prices.user.UserResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService auth;
    private final TokenService tokens;

    public AuthController(AuthService auth, TokenService tokens) {
        this.auth = auth;
        this.tokens = tokens;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.created(URI.create("/api/v1/users/me")).body(auth.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache").body(auth.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AuthenticatedUser principal,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        tokens.revoke(authorization.substring("Bearer ".length()), principal.id());
        return ResponseEntity.noContent().build();
    }
}
