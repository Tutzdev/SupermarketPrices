package br.com.supermercados.prices.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.DefaultCorsProcessor;

class ProblemCorsProcessor extends DefaultCorsProcessor {

    private final SecurityProblemWriter problems;

    ProblemCorsProcessor(SecurityProblemWriter problems) {
        this.problems = problems;
    }

    @Override
    public boolean processRequest(CorsConfiguration configuration, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        boolean allowed = super.processRequest(configuration, request, response);
        if (!allowed) {
            problems.write(request, response, HttpStatus.FORBIDDEN, "Origem ou parâmetros CORS não permitidos.");
        }
        return allowed;
    }

    @Override
    protected void rejectRequest(ServerHttpResponse response) {
        // Defer the body until processRequest can include the original request path.
        response.setStatusCode(HttpStatus.FORBIDDEN);
    }
}
