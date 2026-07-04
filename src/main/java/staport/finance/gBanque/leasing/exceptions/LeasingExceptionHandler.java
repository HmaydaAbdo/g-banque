package staport.finance.gBanque.leasing.exceptions;

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
import staport.finance.gBanque.leasing.batch.controller.LeasingBatchController;
import staport.finance.gBanque.leasing.config.controllers.LeasingConfigController;
import staport.finance.gBanque.leasing.contratLeasing.controller.ContratLeasingController;
import staport.finance.gBanque.leasing.echeancesLeasing.controller.EcheanceLeasingController;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.controllers.OrganismeLeasingController;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Exception handler for the Leasing module.
 *
 * Covers all five leasing controllers: ContratLeasing, EcheancesLeasing,
 * OrganismesLeasing, LeasingBatch et LeasingConfig.
 *
 * The global {@code GlobalExceptionHandler} acts as fallback for any exception
 * not matched here (security, unhandled runtime errors, etc.).
 */
@RestControllerAdvice(assignableTypes = {
        ContratLeasingController.class,
        EcheanceLeasingController.class,
        OrganismeLeasingController.class,
        LeasingBatchController.class,
        LeasingConfigController.class
})
public class LeasingExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(LeasingExceptionHandler.class);

    // ── 404 Not Found ────────────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Leasing — resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage(), ex));
    }

    // ── 400 Bad Request ──────────────────────────────────────────────────────

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        log.warn("Leasing — validation error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", ex.getMessage(), ex));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBeanValidation(MethodArgumentNotValidException ex) {
        String fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        String message = "Validation échouée : " + fieldErrors;
        log.warn("Leasing — bean validation error: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad Request", message, null, Instant.now()));
    }
}
