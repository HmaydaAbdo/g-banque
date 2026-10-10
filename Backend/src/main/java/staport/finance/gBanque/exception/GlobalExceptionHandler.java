package staport.finance.gBanque.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import staport.finance.gBanque.exception.customExceptions.DuplicateEntryException;
import staport.finance.gBanque.exception.customExceptions.FileStorageException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;

import java.util.stream.Collectors;

/**
 * Gestionnaire d'exceptions global — retourne toujours un {@link ErrorResponse} unifié.
 *
 * <p>Tous les handlers produisent le même shape JSON :
 * <pre>
 * {
 *   "status"    : 400,
 *   "error"     : "Bad Request",
 *   "message"   : "...",
 *   "traces"    : null,        ← null en production
 *   "timestamp" : "2025-09-17T10:30:00Z"
 * }
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── 400 — Règle métier / argument invalide ────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Règle métier violée : {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Bad Request",
                ex.getMessage() != null ? ex.getMessage() : "Argument invalide.");
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        log.warn("Erreur de validation : {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    /**
     * Erreurs de validation des DTOs annotés {@code @Valid} / {@code @Validated}.
     * Les messages de chaque champ invalide sont concaténés en un seul message lisible.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(" | "));
        log.warn("Validation DTO échouée : {}", message);
        return build(HttpStatus.BAD_REQUEST, "Bad Request",
                message.isBlank() ? "Validation failed." : message);
    }

    // ── 403 — Autorisation ────────────────────────────────────────────────────

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        log.warn("Accès refusé : {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "Forbidden",
                "Vous n'avez pas les droits nécessaires pour effectuer cette action.");
    }

    // ── 404 — Ressource introuvable ───────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Ressource introuvable : {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    /** URL qui ne correspond à aucun endpoint (sans cela, le filet de sécurité la renverrait en 500). */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("URL inexistante : {}", ex.getResourcePath());
        return build(HttpStatus.NOT_FOUND, "Not Found", "Ressource introuvable.");
    }

    // ── 409 — Doublon ─────────────────────────────────────────────────────────

    @ExceptionHandler(DuplicateEntryException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEntry(DuplicateEntryException ex) {
        log.warn("Entrée dupliquée : {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    // ── 500 — Erreurs techniques ──────────────────────────────────────────────

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorResponse> handleFileStorage(FileStorageException ex) {
        log.error("Erreur stockage fichier : {}", ex.getMessage(), ex);
        return buildWithTraces(HttpStatus.INTERNAL_SERVER_ERROR, "File Processing Error",
                ex.getMessage(), ex);
    }

    /**
     * Filet de sécurité — toute exception non gérée explicitement.
     * Le message interne n'est pas exposé au client pour éviter les fuites d'informations.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Exception non gérée : {}", ex.getMessage(), ex);
        return buildWithTraces(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "Une erreur inattendue s'est produite. Veuillez réessayer plus tard.", ex);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status.value(), error, message));
    }

    private ResponseEntity<ErrorResponse> buildWithTraces(HttpStatus status, String error,
                                                           String message, Exception ex) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status.value(), error, message, ex));
    }
}
