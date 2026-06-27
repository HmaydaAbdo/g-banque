package staport.finance.gBanque.department.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import staport.finance.gBanque.exception.ErrorResponse;
import staport.finance.gBanque.exception.customExceptions.DuplicateEntryException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.department.controller.DepartmentController;

/**
 * Gestionnaire d'exceptions du module Department.
 *
 * <p>Scoped uniquement sur {@link DepartmentController} via {@code assignableTypes}.
 * Le {@code GlobalExceptionHandler} reste actif comme fallback pour tout ce qui
 * n'est pas intercepté ici (validation {@code @Valid}, erreurs runtime non prévues).
 *
 * <p>Mapping HTTP :
 * <ul>
 *   <li>404 — {@link ResourceNotFoundException} (département introuvable)</li>
 *   <li>409 — {@link DuplicateEntryException} (nom de département déjà existant)</li>
 *   <li>409 — {@link DepartmentInUseException} (utilisateurs encore assignés)</li>
 * </ul>
 */
@RestControllerAdvice(assignableTypes = DepartmentController.class)
public class DepartmentExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(DepartmentExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Département introuvable : {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(DuplicateEntryException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateEntryException ex) {
        log.warn("Doublon détecté [Department module] : {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(DepartmentInUseException.class)
    public ResponseEntity<ErrorResponse> handleInUse(DepartmentInUseException ex) {
        log.warn("Suppression de département bloquée : {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status.value(), error, message));
    }
}
