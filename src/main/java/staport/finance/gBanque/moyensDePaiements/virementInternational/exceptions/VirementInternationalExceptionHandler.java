package staport.finance.gBanque.moyensDePaiements.virementInternational.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import staport.finance.gBanque.exception.ErrorResponse;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.moyensDePaiements.virementInternational.controller.VirementInternationalController;

/**
 * Gestionnaire d'exceptions du module Virement International.
 *
 * <p>Scoped uniquement sur {@link VirementInternationalController} via {@code assignableTypes}.
 * {@code GlobalExceptionHandler} reste actif comme fallback pour tout ce qui n'est
 * pas intercepté ici (erreurs imprévues).
 *
 * <p>Mapping HTTP :
 * <ul>
 *   <li>404 — {@link ResourceNotFoundException} (virement international / banque introuvable)</li>
 *   <li>422 — {@link VirementInternationalBusinessException} (violation règle métier)</li>
 * </ul>
 */
@RestControllerAdvice(assignableTypes = VirementInternationalController.class)
public class VirementInternationalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(VirementInternationalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        logger.warn("Ressource introuvable [Virement International module]: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Non trouvé", ex.getMessage(), ex));
    }

    @ExceptionHandler(VirementInternationalBusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(VirementInternationalBusinessException ex) {
        logger.warn("Violation règle métier [Virement International module]: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(422, "Règle métier violée", ex.getMessage(), ex));
    }
}
