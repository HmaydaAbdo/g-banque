package staport.finance.gBanque.moyensDePaiements.miseADisposition.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import staport.finance.gBanque.exception.ErrorResponse;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.controller.MiseADispositionController;

/**
 * Gestionnaire d'exceptions du module Mise à Disposition.
 *
 * <p>Scoped uniquement sur {@link MiseADispositionController} via {@code assignableTypes}.
 * {@code GlobalExceptionHandler} reste actif comme fallback pour tout ce qui n'est
 * pas intercepté ici (erreurs imprévues).
 *
 * <p>Mapping HTTP :
 * <ul>
 *   <li>404 — {@link ResourceNotFoundException} (mise à disposition / banque introuvable)</li>
 *   <li>422 — {@link MiseADispositionBusinessException} (violation règle métier)</li>
 * </ul>
 */
@RestControllerAdvice(assignableTypes = MiseADispositionController.class)
public class MiseADispositionExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(MiseADispositionExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        logger.warn("Ressource introuvable [Mise à Disposition module]: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Non trouvé", ex.getMessage(), ex));
    }

    @ExceptionHandler(MiseADispositionBusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(MiseADispositionBusinessException ex) {
        logger.warn("Violation règle métier [Mise à Disposition module]: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(422, "Règle métier violée", ex.getMessage(), ex));
    }
}
