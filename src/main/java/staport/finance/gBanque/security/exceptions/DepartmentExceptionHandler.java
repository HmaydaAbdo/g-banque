package staport.finance.gBanque.security.exceptions;

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
import staport.finance.gBanque.security.controller.DepartmentController;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Gestionnaire d'exceptions du module Department.
 *
 * <p>Scoped uniquement sur {@link DepartmentController} via {@code assignableTypes}.
 * Le {@code GlobalExceptionHandler} reste actif comme fallback pour tout ce qui
 * n'est pas intercepté ici (sécurité, erreurs runtime non prévues).
 *
 * <p>Mapping HTTP :
 * <ul>
 *   <li>404 — {@link ResourceNotFoundException} (département introuvable)</li>
 *   <li>409 — {@link DuplicateEntryException} (nom de département déjà existant)</li>
 *   <li>409 — {@link DepartmentInUseException} (utilisateurs encore assignés)</li>
 *   <li>400 — {@link MethodArgumentNotValidException} (validation {@code @Valid})</li>
 * </ul>
 */
@RestControllerAdvice(assignableTypes = DepartmentController.class)
public class DepartmentExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(DepartmentExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        logger.warn("Département introuvable : {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage(), ex));
    }

    @ExceptionHandler(DuplicateEntryException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateEntryException ex) {
        logger.warn("Doublon détecté [Department module]: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage(), ex));
    }

    @ExceptionHandler(DepartmentInUseException.class)
    public ResponseEntity<ErrorResponse> handleInUse(DepartmentInUseException ex) {
        logger.warn("Suppression de département bloquée : {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        String message = "Validation échouée : " + fieldErrors;
        logger.warn("Erreur de validation [Department module]: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad Request", message, null, Instant.now()));
    }
}
