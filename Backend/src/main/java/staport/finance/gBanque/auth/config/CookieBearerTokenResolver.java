package staport.finance.gBanque.auth.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import staport.finance.gBanque.auth.services.AuthCookieService;

import java.util.Set;

/**
 * Lit le JWT d'accès dans le cookie {@link AuthCookieService#ACCESS_COOKIE} (navigateur), ou à défaut
 * dans l'en-tête {@code Authorization: Bearer} (Swagger, scripts, outils).
 *
 * <p>Sur les endpoints de session (connexion, rafraîchissement, déconnexion), aucun jeton n'est lu :
 * un JWT d'accès expiré encore présent ne doit pas faire échouer ces appels en 401 — c'est
 * précisément quand il a expiré qu'on rafraîchit ou qu'on se déconnecte.
 */
public class CookieBearerTokenResolver implements BearerTokenResolver {

    private static final Set<String> SESSION_ENDPOINTS = Set.of("/auth/login", "/auth/refresh", "/auth/logout");

    private final DefaultBearerTokenResolver headerResolver = new DefaultBearerTokenResolver();

    @Override
    public String resolve(HttpServletRequest request) {
        if (isSessionEndpoint(request)) {
            return null;
        }
        String fromHeader = headerResolver.resolve(request);
        if (fromHeader != null) {
            return fromHeader;
        }
        return AuthCookieService.read(request, AuthCookieService.ACCESS_COOKIE).orElse(null);
    }

    static boolean isSessionEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return SESSION_ENDPOINTS.contains(path);
    }
}
