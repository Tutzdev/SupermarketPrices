package br.com.supermercados.prices.auth;

import br.com.supermercados.prices.user.UserResponse;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
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
    private final AccountService accounts;
    private final AbuseProtectionService abuseProtection;

    public AuthController(AuthService auth, TokenService tokens, AccountService accounts,
            AbuseProtectionService abuseProtection) {
        this.auth = auth;
        this.tokens = tokens;
        this.accounts = accounts;
        this.abuseProtection = abuseProtection;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        abuseProtection.checkRegistration(httpRequest.getRemoteAddr());
        return ResponseEntity.created(URI.create("/api/v1/users/me")).body(auth.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        abuseProtection.checkLogin(httpRequest.getRemoteAddr(), request.email());
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache").body(auth.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AuthenticatedUser principal,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        tokens.revoke(authorization.substring("Bearer ".length()), principal.id());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/email-verifications")
    public ResponseEntity<Void> requestEmailVerification(
            @AuthenticationPrincipal AuthenticatedUser principal, HttpServletRequest httpRequest) {
        abuseProtection.checkAccountConfirmation(httpRequest.getRemoteAddr());
        accounts.requestEmailVerification(principal.id());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/email-verifications/confirm")
    public ResponseEntity<Void> confirmEmail(
            @Valid @RequestBody EmailVerificationConfirmRequest request,
            HttpServletRequest httpRequest) {
        abuseProtection.checkAccountConfirmation(httpRequest.getRemoteAddr());
        accounts.confirmEmail(request.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-resets")
    public ResponseEntity<Void> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletRequest httpRequest) {
        abuseProtection.checkAccountMessage(httpRequest.getRemoteAddr(), request.email());
        accounts.requestPasswordReset(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password-resets/confirm")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody PasswordResetConfirmRequest request,
            HttpServletRequest httpRequest) {
        abuseProtection.checkAccountConfirmation(httpRequest.getRemoteAddr());
        accounts.resetPassword(request.token(), request.password());
        return ResponseEntity.noContent().build();
    }
}
