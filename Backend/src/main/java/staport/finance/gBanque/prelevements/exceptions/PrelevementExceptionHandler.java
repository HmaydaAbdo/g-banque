package staport.finance.gBanque.prelevements.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import staport.finance.gBanque.exception.ErrorResponse;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.prelevements.controller.PrelevementController;

/**
 * Gestionnaire d'exceptions du module Prélèvements.
 *
 * <p>Scoped uniquement sur {@link PrelevementController} via {@code assignableTypes}.
 * {@code GlobalExceptionHandler} reste actif comme fallback pour tout ce qui n'est
 * pas intercepté ici (erreurs imprévues).
 *
 * <p>Mapping HTTP :
 * <ul>
 *   <li>404 — {@link ResourceNotFoundException} (prélèvement / banque introuvable)</li>
 *   <li>422 — {@link PrelevementBusinessException} (violation règle métier)</li>
 * </ul>
 */
@RestControllerAdvice(assignableTypes = PrelevementController.class)
public class PrelevementExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(PrelevementExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        logger.warn("Ressource introuvable [Prélèvement module]: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Non trouvé", ex.getMessage(), ex));
    }

    @ExceptionHandler(PrelevementBusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(PrelevementBusinessException ex) {
        logger.warn("Violation règle métier [Prélèvement module]: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(422, "Règle métier violée", ex.getMessage(), ex));
    }
}
