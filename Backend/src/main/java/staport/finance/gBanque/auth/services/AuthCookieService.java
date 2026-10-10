package staport.finance.gBanque.auth.services;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

/**
 * Pose et efface les cookies de session. Les jetons ne transitent jamais dans le corps des réponses
 * et ne sont pas lisibles par JavaScript ({@code HttpOnly}) : une faille XSS ne permet pas de les voler.
 *
 * <ul>
 *   <li>{@value #ACCESS_COOKIE} — JWT d'accès, envoyé sur toute l'API ({@code <context-path>}).</li>
 *   <li>{@value #REFRESH_COOKIE} — jeton de rafraîchissement opaque, envoyé uniquement à
 *       {@code <context-path>/auth} (rafraîchissement et déconnexion).</li>
 * </ul>
 * Tous deux : {@code SameSite=Strict} (jamais envoyés depuis un autre site) et {@code Secure} selon
 * {@code AUTH_COOKIE_SECURE} — {@code true} dès que G-Banque est servi en HTTPS ; tant qu'il l'est en
 * HTTP, {@code false} (un navigateur refuse un cookie {@code Secure} reçu en HTTP, hors localhost).
 */
@Service
public class AuthCookieService {

    public static final String ACCESS_COOKIE = "GBANQUE_ACCESS";
    public static final String REFRESH_COOKIE = "GBANQUE_REFRESH";

    private final String apiPath;
    private final String authPath;
    private final long accessTokenMinutes;
    private final boolean secure;

    public AuthCookieService(@Value("${server.servlet.context-path}") String contextPath,
                             @Value("${jwt.access-token-minutes}") long accessTokenMinutes,
                             @Value("${auth.cookie.secure}") boolean secure) {
        this.apiPath = contextPath.isBlank() ? "/" : contextPath;
        this.authPath = (contextPath.endsWith("/") ? contextPath : contextPath + "/") + "auth";
        this.accessTokenMinutes = accessTokenMinutes;
        this.secure = secure;
    }

    public void writeSession(HttpServletResponse response, String accessToken, RefreshTokenService.IssuedRefreshToken refreshToken) {
        writeAccess(response, accessToken);
        Duration remaining = Duration.between(Instant.now(), refreshToken.expiresAt());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(REFRESH_COOKIE, refreshToken.rawToken(), authPath, remaining).toString());
    }

    public void writeAccess(HttpServletResponse response, String accessToken) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookie(ACCESS_COOKIE, accessToken, apiPath, Duration.ofMinutes(accessTokenMinutes)).toString());
    }

    public void clearSession(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(ACCESS_COOKIE, "", apiPath, Duration.ZERO).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(REFRESH_COOKIE, "", authPath, Duration.ZERO).toString());
    }

    public static Optional<String> read(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()) && !c.getValue().isBlank())
                .map(Cookie::getValue)
                .findFirst();
    }

    private ResponseCookie cookie(String name, String value, String path, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(path)
                .maxAge(maxAge)
                .build();
    }
}
