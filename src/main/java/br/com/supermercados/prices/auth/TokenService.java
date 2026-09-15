package br.com.supermercados.prices.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenService {

    private static final int TOKEN_BYTES = 32;
    private static final int TOKEN_LENGTH = 43;

    private final AuthTokenRepository tokens;
    private final Clock clock;
    private final Duration tokenTtl;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenService(AuthTokenRepository tokens, Clock clock,
            @Value("${app.auth.token-ttl:PT12H}") Duration tokenTtl) {
        if (tokenTtl.isZero() || tokenTtl.isNegative()) {
            throw new IllegalArgumentException("app.auth.token-ttl must be positive");
        }
        this.tokens = tokens;
        this.clock = clock;
        this.tokenTtl = tokenTtl;
    }

    @Transactional
    public IssuedToken issue(UUID userId) {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        Instant now = clock.instant();
        Instant expiresAt = now.plus(tokenTtl);
        tokens.deleteExpiredForUser(userId, now);
        tokens.save(new AuthToken(userId, hash(rawToken), now, expiresAt));
        return new IssuedToken(rawToken, expiresAt);
    }

    @Transactional(readOnly = true)
    public Optional<AuthenticatedUser> authenticate(String rawToken) {
        if (rawToken == null || rawToken.length() != TOKEN_LENGTH
                || !rawToken.matches("[A-Za-z0-9_-]{43}")) {
            return Optional.empty();
        }
        return tokens.findByTokenHashAndExpiresAtAfter(hash(rawToken), clock.instant())
                .map(token -> new AuthenticatedUser(token.getUserId()));
    }

    @Transactional
    public void revoke(String rawToken, UUID userId) {
        tokens.revoke(hash(rawToken), userId);
    }

    private static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("The Java runtime must support SHA-256", exception);
        }
    }

    public record IssuedToken(String value, Instant expiresAt) {

        @Override
        public String toString() {
            return "IssuedToken[redacted]";
        }
    }
}
