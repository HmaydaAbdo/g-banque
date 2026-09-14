package staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.controller.NatureLigneFonctionnelleController;
import staport.finance.gBanque.exception.ErrorResponse;
import staport.finance.gBanque.exception.customExceptions.DuplicateEntryException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Module-level exception handler scoped to {@link NatureLigneFonctionnelleController}.
 * Returns typed {@link ErrorResponse} payloads for all NatureLF domain exceptions.
 */
@RestControllerAdvice(assignableTypes = NatureLigneFonctionnelleController.class)
public class NatureLFExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(NatureLFExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        log.warn("NatureLF not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage(), ex));
    }

    @ExceptionHandler(DuplicateEntryException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateEntryException ex) {
        log.warn("Duplicate NatureLF entry: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage(), ex));
    }

    @ExceptionHandler(NatureLFDeletionException.class)
    public ResponseEntity<ErrorResponse> handleDeletionGuard(NatureLFDeletionException ex) {
        log.warn("NatureLF deletion blocked: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage(), ex));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        String message = "Validation échouée : " + fieldErrors;
        log.warn("Validation error on NatureLFController: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad Request", message, null, Instant.now()));
    }
}
