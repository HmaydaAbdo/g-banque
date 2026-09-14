package staport.finance.gBanque.moyensDePaiements.virements.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import staport.finance.gBanque.exception.ErrorResponse;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.moyensDePaiements.virements.controller.VirementController;

/**
 * Gestionnaire d'exceptions du module Virement.
 *
 * <p>Scoped uniquement sur {@link VirementController} via {@code assignableTypes}.
 * {@code GlobalExceptionHandler} reste actif comme fallback pour tout ce qui n'est
 * pas intercepté ici (erreurs imprévues).
 *
 * <p>Mapping HTTP :
 * <ul>
 *   <li>404 — {@link ResourceNotFoundException} (virement / banque introuvable)</li>
 *   <li>422 — {@link VirementBusinessException} (violation règle métier virement)</li>
 * </ul>
 */
@RestControllerAdvice(assignableTypes = VirementController.class)
public class VirementExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(VirementExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        logger.warn("Ressource introuvable [Virement module]: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Non trouvé", ex.getMessage(), ex));
    }

    @ExceptionHandler(VirementBusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(VirementBusinessException ex) {
        logger.warn("Violation règle métier [Virement module]: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(422, "Règle métier violée", ex.getMessage(), ex));
    }
}
