package staport.finance.gBanque.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.exception.ErrorResponse;

import java.io.IOException;

/**
 * Gestionnaire d'accès refusé JSON — niveau filtre de sécurité.
 *
 * <p>Filet de sécurité défensif : avec la configuration actuelle (autorisation
 * uniquement via {@code @PreAuthorize}, déjà couverte par
 * {@code GlobalExceptionHandler.handleAuthorizationDenied}), ce handler n'est en
 * principe jamais déclenché. Il devient nécessaire si des règles d'autorisation
 * sont un jour ajoutées directement dans {@code authorizeHttpRequests()}
 * (ex. {@code .hasAuthority(...)}), qui sont évaluées avant d'atteindre un controller.
 */
@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(JsonAccessDeniedHandler.class);

    private final ObjectMapper objectMapper;

    public JsonAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException {
        log.warn("Accès refusé (niveau filtre) pour {} {} : {}", request.getMethod(), request.getRequestURI(), accessDeniedException.getMessage());

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json;charset=UTF-8");

        ErrorResponse body = ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                "Vous n'avez pas les droits nécessaires pour effectuer cette action."
        );

        objectMapper.writeValue(response.getWriter(), body);
    }
}
