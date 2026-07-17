package staport.finance.gBanque.rapprochement.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import staport.finance.gBanque.exception.ErrorResponse;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.rapprochement.controller.RapprochementController;

import java.util.stream.Collectors;

/**
 * Gestionnaire d'exceptions du module Rapprochement.
 *
 * <p>Scoped uniquement sur {@link RapprochementController} via {@code assignableTypes}. Le
 * {@code GlobalExceptionHandler} reste actif comme fallback pour tout ce qui n'est pas
 * intercepté ici.
 *
 * <p>Mapping HTTP :
 * <ul>
 *   <li>404 — {@link ResourceNotFoundException} (rapprochement ou banque introuvable)</li>
 *   <li>422 — {@link ValidationException} (transition de statut invalide, clôture provisoire déjà existante)</li>
 *   <li>400 — {@link MethodArgumentNotValidException} (validation {@code @Valid})</li>
 * </ul>
 */
@RestControllerAdvice(assignableTypes = RapprochementController.class)
public class RapprochementExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RapprochementExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Ressource introuvable [Rapprochement module]: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Non trouvé", ex.getMessage(), ex));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(ValidationException ex) {
        log.warn("Violation règle métier [Rapprochement module]: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(422, "Règle métier violée", ex.getMessage(), ex));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        String message = "Validation échouée : " + fieldErrors;
        log.warn("Erreur de validation [Rapprochement module]: {}", message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Requête invalide", message, ex));
    }
}
