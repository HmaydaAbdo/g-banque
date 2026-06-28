package staport.finance.gBanque.Agios.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import staport.finance.gBanque.Agios.controller.AgiosController;
import staport.finance.gBanque.exception.ErrorResponse;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Gestionnaire d'exceptions du module Agios.
 *
 * <p>Scoped uniquement sur {@link AgiosController} via {@code assignableTypes}.
 * Le {@code GlobalExceptionHandler} reste actif comme fallback pour les erreurs imprévues.
 *
 * <p>Mapping HTTP :
 * <ul>
 *   <li>404 — {@link AgiosNotFoundException}</li>
 *   <li>422 — {@link AgiosBusinessException} (violation règle métier)</li>
 *   <li>400 — {@link MethodArgumentNotValidException} (validation @Valid)</li>
 * </ul>
 */
@RestControllerAdvice(assignableTypes = AgiosController.class)
public class AgiosExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AgiosExceptionHandler.class);

    // ── 404 Not Found ─────────────────────────────────────────────────────────

    @ExceptionHandler(AgiosNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(AgiosNotFoundException ex) {
        log.warn("Agios introuvable : {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Non trouvé", ex.getMessage(), ex));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Ressource introuvable [Agios module] : {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Non trouvé", ex.getMessage(), ex));
    }

    // ── 422 Unprocessable Entity ──────────────────────────────────────────────

    @ExceptionHandler(AgiosBusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(AgiosBusinessException ex) {
        log.warn("Violation règle métier [Agios module] : {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(422, "Règle métier violée", ex.getMessage(), ex));
    }

    // ── 400 Bad Request ───────────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        String message = "Validation échouée : " + fieldErrors;
        log.warn("Erreur de validation [Agios module] : {}", message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Requête invalide", message, null, Instant.now()));
    }
}
