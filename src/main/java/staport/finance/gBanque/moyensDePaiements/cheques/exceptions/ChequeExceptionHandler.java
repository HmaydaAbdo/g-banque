package staport.finance.gBanque.moyensDePaiements.cheques.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import staport.finance.gBanque.exception.ErrorResponse;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.moyensDePaiements.cheques.controller.ChequeController;

/**
 * Gestionnaire d'exceptions du module Chèque.
 *
 * <p>Scoped uniquement sur {@link ChequeController} via {@code assignableTypes}.
 * {@code GlobalExceptionHandler} reste actif comme fallback pour tout ce qui n'est
 * pas intercepté ici (erreurs imprévues).
 *
 * <p>Mapping HTTP :
 * <ul>
 *   <li>404 — {@link ResourceNotFoundException} (chèque / banque introuvable)</li>
 *   <li>422 — {@link ChequeBusinessException} (violation règle métier chèque)</li>
 * </ul>
 */
@RestControllerAdvice(assignableTypes = ChequeController.class)
public class ChequeExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChequeExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        logger.warn("Ressource introuvable [Cheque module]: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Non trouvé", ex.getMessage(), ex));
    }

    @ExceptionHandler(ChequeBusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(ChequeBusinessException ex) {
        logger.warn("Violation règle métier [Cheque module]: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(422, "Règle métier violée", ex.getMessage(), ex));
    }
}
