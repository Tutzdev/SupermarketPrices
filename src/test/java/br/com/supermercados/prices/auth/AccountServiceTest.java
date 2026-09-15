package br.com.supermercados.prices.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.supermercados.prices.common.ApiException;
import br.com.supermercados.prices.user.User;
import br.com.supermercados.prices.user.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-15T12:00:00Z");
    private static final String RAW_TOKEN = "a".repeat(43);

    @Mock private UserRepository users;
    @Mock private EmailVerificationTokenRepository verificationTokens;
    @Mock private PasswordResetTokenRepository resetTokens;
    @Mock private TokenService accessTokens;
    @Mock private AccountEmailSender emailSender;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private AccountService accounts;

    @BeforeEach
    void setUp() {
        accounts = new AccountService(users, verificationTokens, resetTokens, passwordEncoder,
                new PasswordPolicy(), accessTokens, emailSender, Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofDays(1), Duration.ofHours(1));
    }

    @Test
    void passwordResetIsSingleUseAndRevokesEveryPreviousSession() {
        User user = new User("Test User", "user@example.test", passwordEncoder.encode("old-password-123"), NOW);
        PasswordResetToken token = new PasswordResetToken(user.getId(), SecureTokenValues.hash(RAW_TOKEN),
                NOW.minusSeconds(60), NOW.plusSeconds(60));
        when(resetTokens.findByTokenHashForUpdate(SecureTokenValues.hash(RAW_TOKEN)))
                .thenReturn(Optional.of(token));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));

        accounts.resetPassword(RAW_TOKEN, "new-password-123");

        assertThat(passwordEncoder.matches("new-password-123", user.getPasswordHash())).isTrue();
        verify(accessTokens).revokeAll(user.getId());
        assertThatThrownBy(() -> accounts.resetPassword(RAW_TOKEN, "another-password-123"))
                .isInstanceOf(ApiException.class);
        verify(accessTokens, times(1)).revokeAll(user.getId());
    }

    @Test
    void expiredPasswordResetTokenIsRejectedWithoutChangingPasswordOrSessions() {
        User user = new User("Test User", "user@example.test", passwordEncoder.encode("old-password-123"), NOW);
        PasswordResetToken token = new PasswordResetToken(user.getId(), SecureTokenValues.hash(RAW_TOKEN),
                NOW.minus(Duration.ofHours(2)), NOW.minusSeconds(1));
        when(resetTokens.findByTokenHashForUpdate(SecureTokenValues.hash(RAW_TOKEN)))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> accounts.resetPassword(RAW_TOKEN, "new-password-123"))
                .isInstanceOf(ApiException.class);

        assertThat(passwordEncoder.matches("old-password-123", user.getPasswordHash())).isTrue();
        org.mockito.Mockito.verifyNoInteractions(accessTokens);
    }
}
