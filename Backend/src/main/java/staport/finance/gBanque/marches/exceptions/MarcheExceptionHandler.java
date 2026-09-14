package staport.finance.gBanque.marches.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import staport.finance.gBanque.exception.ErrorResponse;
import staport.finance.gBanque.exception.customExceptions.DuplicateEntryException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.marches.controller.MarcheController;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Gestionnaire d'exceptions du module Marches.
 *
 * <p>Scoped uniquement sur {@link MarcheController} via {@code assignableTypes}.
 * Retourne des payloads {@link ErrorResponse} typés — jamais de {@code Map} brute.
 *
 * <p>Le {@code GlobalExceptionHandler} reste actif comme fallback pour tout ce
 * qui n'est pas intercepté ici (sécurité, erreurs runtime non prévues).
 *
 * <p>Mapping HTTP :
 * <ul>
 *   <li>404 — {@link ResourceNotFoundException} (marché / banque introuvable)</li>
 *   <li>409 — {@link DuplicateEntryException} (numéro de marché déjà existant)</li>
 *   <li>422 — {@link MarcheBusinessException} (violation règle métier marché)</li>
 *   <li>400 — {@link MethodArgumentNotValidException} (validation @Valid)</li>
 * </ul>
 */
@RestControllerAdvice(assignableTypes = MarcheController.class)
public class MarcheExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(MarcheExceptionHandler.class);

    // ── 404 Not Found ─────────────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Ressource introuvable [Marches module]: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Non trouvé", ex.getMessage(), ex));
    }

    // ── 409 Conflict ──────────────────────────────────────────────────────────

    @ExceptionHandler(DuplicateEntryException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateEntryException ex) {
        log.warn("Doublon détecté [Marches module]: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflit", ex.getMessage(), ex));
    }

    // ── 422 Unprocessable Entity ──────────────────────────────────────────────

    @ExceptionHandler(MarcheBusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(MarcheBusinessException ex) {
        log.warn("Violation règle métier [Marches module]: {}", ex.getMessage());
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
        log.warn("Erreur de validation [Marches module]: {}", message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Requête invalide", message, null, Instant.now()));
    }
}
