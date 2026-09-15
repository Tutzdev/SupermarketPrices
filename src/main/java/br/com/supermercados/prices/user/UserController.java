package br.com.supermercados.prices.user;

import br.com.supermercados.prices.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping
    public UserResponse currentUser(@AuthenticationPrincipal AuthenticatedUser principal) {
        return users.findCurrentUser(principal.id());
    }

    @PatchMapping
    public UserResponse updateCurrentUser(@AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody UpdateUserRequest request) {
        return users.updateCurrentUser(principal.id(), request);
    }
}
