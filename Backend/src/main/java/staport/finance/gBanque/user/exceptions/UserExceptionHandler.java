package staport.finance.gBanque.user.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import staport.finance.gBanque.exception.ErrorResponse;
import staport.finance.gBanque.exception.customExceptions.DuplicateEntryException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.user.controller.UserController;

/**
 * Gestionnaire d'exceptions du module User.
 *
 * <p>Scoped uniquement sur {@link UserController} via {@code assignableTypes}.
 * Le {@code GlobalExceptionHandler} reste actif comme fallback pour tout ce qui
 * n'est pas intercepté ici (validation {@code @Valid}, erreurs runtime non prévues).
 *
 * <p>Mapping HTTP :
 * <ul>
 *   <li>404 — {@link ResourceNotFoundException} (utilisateur/département introuvable)</li>
 *   <li>409 — {@link DuplicateEntryException} (nom d'utilisateur déjà existant)</li>
 * </ul>
 */
@RestControllerAdvice(assignableTypes = UserController.class)
public class UserExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(UserExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Ressource introuvable [User module] : {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(DuplicateEntryException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateEntryException ex) {
        log.warn("Doublon détecté [User module] : {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status.value(), error, message));
    }
}
