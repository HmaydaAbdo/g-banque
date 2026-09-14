package staport.finance.gBanque.moyensDePaiements.lcn.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import staport.finance.gBanque.exception.ErrorResponse;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.moyensDePaiements.lcn.controller.LcnController;

/**
 * Gestionnaire d'exceptions du module LCN.
 *
 * <p>Scoped uniquement sur {@link LcnController} via {@code assignableTypes}.
 * {@code GlobalExceptionHandler} reste actif comme fallback pour tout ce qui n'est
 * pas intercepté ici (erreurs imprévues).
 *
 * <p>Mapping HTTP :
 * <ul>
 *   <li>404 — {@link ResourceNotFoundException} (LCN / banque / ligne fonctionnelle introuvable)</li>
 *   <li>422 — {@link LcnBusinessException} (violation règle métier LCN)</li>
 * </ul>
 */
@RestControllerAdvice(assignableTypes = LcnController.class)
public class LcnExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(LcnExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        logger.warn("Ressource introuvable [Lcn module]: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Non trouvé", ex.getMessage(), ex));
    }

    @ExceptionHandler(LcnBusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(LcnBusinessException ex) {
        logger.warn("Violation règle métier [Lcn module]: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(422, "Règle métier violée", ex.getMessage(), ex));
    }
}
