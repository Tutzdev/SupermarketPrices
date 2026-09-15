package br.com.supermercados.prices.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

class BearerTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(BearerTokenFilter.class);

    private final TokenService tokens;
    private final SecurityProblemWriter problems;

    BearerTokenFilter(TokenService tokens, SecurityProblemWriter problems) {
        this.tokens = tokens;
        this.problems = problems;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        List<String> authorization = Collections.list(request.getHeaders(HttpHeaders.AUTHORIZATION));
        if (authorization.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }
        if (authorization.size() != 1 || !authorization.getFirst().regionMatches(true, 0, "Bearer ", 0, 7)) {
            reject(request, response);
            return;
        }
        Optional<AuthenticatedUser> principal;
        try {
            principal = tokens.authenticate(authorization.getFirst().substring(7));
        } catch (DataAccessException exception) {
            log.error("Falha de persistência durante autenticação: {}", exception.getClass().getSimpleName());
            problems.write(request, response, HttpStatus.SERVICE_UNAVAILABLE,
                    "Autenticação temporariamente indisponível.");
            return;
        }
        if (principal.isEmpty()) {
            reject(request, response);
            return;
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                principal.orElseThrow(), null, List.of(principal.orElseThrow().authority())));
        SecurityContextHolder.setContext(context);
        chain.doFilter(request, response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        problems.write(request, response, HttpStatus.UNAUTHORIZED, "Token de acesso inválido ou expirado.");
    }
}
