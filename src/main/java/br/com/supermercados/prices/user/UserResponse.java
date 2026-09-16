package br.com.supermercados.prices.user;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        UserRole role,
        boolean emailVerified,
        boolean subscriber,
        Instant createdAt,
        Instant updatedAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.isEmailVerified(),
                user.isSubscriber(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
