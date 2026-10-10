package staport.finance.gBanque.auth.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import staport.finance.gBanque.auth.controller.SecurityController;
import staport.finance.gBanque.exception.ErrorResponse;
import staport.finance.gBanque.exception.customExceptions.InvalidRefreshTokenException;
import staport.finance.gBanque.exception.customExceptions.TooManyAttemptsException;

/**
 * Gestionnaire d'exceptions propre au module Auth — scoped à {@link SecurityController}.
 *
 * <p>Couvre les cas d'erreur spécifiques à l'authentification :
 * <ul>
 *   <li>{@link BadCredentialsException} — identifiants incorrects (HTTP 401) ;</li>
 *   <li>{@link InvalidRefreshTokenException} — session terminée au rafraîchissement (HTTP 401) ;</li>
 *   <li>{@link TooManyAttemptsException} — compte verrouillé brute-force (HTTP 429).</li>
 * </ul>
 *
 * <p>Le {@code GlobalExceptionHandler} reste le filet global pour toutes les erreurs
 * imprévues ; ce handler ne traite que ce qui est propre au domaine Auth.
 */
@RestControllerAdvice(assignableTypes = SecurityController.class)
public class SecurityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SecurityExceptionHandler.class);

    // ── 401 — Authentification ────────────────────────────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Echec d'authentification : {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized",
                "Identifiant ou mot de passe incorrect.");
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        log.info("Rafraîchissement de session refusé : {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage());
    }

    // ── 429 — Verrouillage brute-force ────────────────────────────────────────

    @ExceptionHandler(TooManyAttemptsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyAttempts(TooManyAttemptsException ex) {
        log.warn("Verrouillage anti brute-force : {}", ex.getMessage());
        return build(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", ex.getMessage());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status.value(), error, message));
    }
}
