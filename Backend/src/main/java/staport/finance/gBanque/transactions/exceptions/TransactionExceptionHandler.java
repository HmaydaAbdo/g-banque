package staport.finance.gBanque.transactions.exceptions;

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
import staport.finance.gBanque.transactions.controller.TransactionController;

import java.util.stream.Collectors;

/**
 * Gestionnaire d'exceptions du module Transactions.
 *
 * <p>Scoped uniquement sur {@link TransactionController} via {@code assignableTypes}. Le
 * {@code GlobalExceptionHandler} reste actif comme fallback pour tout ce qui n'est pas
 * intercepté ici.
 *
 * <p>Mapping HTTP :
 * <ul>
 *   <li>404 — {@link ResourceNotFoundException} (transaction introuvable)</li>
 *   <li>422 — {@link ValidationException} (rapprochement définitivement clôturé)</li>
 *   <li>400 — {@link MethodArgumentNotValidException} (validation {@code @Valid})</li>
 * </ul>
 */
@RestControllerAdvice(assignableTypes = TransactionController.class)
public class TransactionExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TransactionExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Ressource introuvable [Transactions module]: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Non trouvé", ex.getMessage(), ex));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(ValidationException ex) {
        log.warn("Violation règle métier [Transactions module]: {}", ex.getMessage());
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
        log.warn("Erreur de validation [Transactions module]: {}", message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Requête invalide", message, ex));
    }
}
