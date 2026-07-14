package staport.finance.gBanque.impot_taxes.exceptions;

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
import staport.finance.gBanque.impot_taxes.controller.ImpotEtTaxeController;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Gestionnaire d'exceptions du module Impôts et Taxes.
 *
 * <p>Scoped uniquement sur {@link ImpotEtTaxeController} via {@code assignableTypes}.
 * Le {@code GlobalExceptionHandler} reste actif comme fallback pour tout ce qui n'est pas
 * intercepté ici (notamment {@code ValidationException}, déjà géré globalement en 400).
 *
 * <p>Mapping HTTP :
 * <ul>
 *   <li>404 — {@link ResourceNotFoundException} (impôt/taxe / banque introuvable)</li>
 *   <li>400 — {@link MethodArgumentNotValidException} (validation @Valid)</li>
 * </ul>
 */
@RestControllerAdvice(assignableTypes = ImpotEtTaxeController.class)
public class ImpotEtTaxeExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ImpotEtTaxeExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Ressource introuvable [Impôts/Taxes module]: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Non trouvé", ex.getMessage(), ex));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        String message = "Validation échouée : " + fieldErrors;
        log.warn("Erreur de validation [Impôts/Taxes module]: {}", message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Requête invalide", message, null, Instant.now()));
    }
}
