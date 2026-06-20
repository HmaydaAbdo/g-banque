package staport.finance.gBanque.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.exception.ErrorResponse;

import java.io.IOException;

/**
 * Point d'entrée d'authentification JSON.
 *
 * <p>Sans ce bean, une requête sans jeton (ou avec un jeton invalide/expiré) sur un
 * endpoint protégé reçoit une page HTML par défaut de Spring Security — ce filtre
 * agit avant que la requête n'atteigne un controller, donc {@code GlobalExceptionHandler}
 * ne peut pas l'intercepter. C'est le seul vrai trou JSON côté authentification.
 */
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(JsonAuthenticationEntryPoint.class);

    private final ObjectMapper objectMapper;

    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        log.warn("Authentification requise pour {} {} : {}", request.getMethod(), request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");

        ErrorResponse body = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "Authentification requise ou jeton invalide/expiré."
        );

        objectMapper.writeValue(response.getWriter(), body);
    }
}
