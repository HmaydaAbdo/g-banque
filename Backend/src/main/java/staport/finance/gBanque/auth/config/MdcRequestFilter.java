package staport.finance.gBanque.auth.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtre MDC — injecte {@code requestId} et {@code userId} dans le contexte
 * de log (SLF4J MDC) pour chaque requête HTTP.
 *
 * <p>Ces deux champs apparaissent automatiquement dans tous les logs émis
 * pendant le traitement de la requête, sans aucune action dans les services
 * ou controllers. Voir {@code logback-spring.xml} pour leur intégration dans
 * le format console (dev) et JSON (prod).
 *
 * <p>Ce filtre est enregistré <em>après</em> le {@code BearerTokenAuthenticationFilter}
 * (via {@code SecurityConfig}) afin que le {@code SecurityContextHolder} soit déjà
 * peuplé au moment où {@code userId} est lu.
 *
 * <p>Règle CLAUDE.md : ne jamais poser {@code MDC.put("requestId", ...)} ou
 * {@code MDC.put("userId", ...)} manuellement dans les services ou controllers.
 */
public class MdcRequestFilter extends OncePerRequestFilter {

    private static final String MDC_REQUEST_ID = "requestId";
    private static final String MDC_USER_ID    = "userId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // requestId : lire depuis l'en-tête entrant si fourni, sinon générer un UUID court
            String requestId = request.getHeader("X-Request-ID");
            if (requestId == null || requestId.isBlank()) {
                requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            }
            MDC.put(MDC_REQUEST_ID, requestId);

            // Propager requestId vers le client pour corrélation dans les logs frontend
            response.setHeader("X-Request-ID", requestId);

            // userId : principal JWT si authentifié, absent sur les routes publiques
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getPrincipal())) {
                MDC.put(MDC_USER_ID, auth.getName());
            }

            filterChain.doFilter(request, response);
        } finally {
            // Obligatoire : les threads sont réutilisés (pool), le MDC doit être vidé
            // après chaque requête pour éviter les fuites de contexte.
            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_USER_ID);
        }
    }
}
