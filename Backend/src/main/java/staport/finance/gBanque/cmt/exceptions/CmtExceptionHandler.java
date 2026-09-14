package staport.finance.gBanque.cmt.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import staport.finance.gBanque.cmt.batch.controller.CmtBatchController;
import staport.finance.gBanque.cmt.config.controllers.CmtConfigController;
import staport.finance.gBanque.cmt.controllers.CreditMoyenTermeController;
import staport.finance.gBanque.cmt.echeances.controllers.EcheanceCMTController;
import staport.finance.gBanque.exception.ErrorResponse;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Exception handler pour le module CMT (Crédit Moyen Terme).
 * <p>
 * Couvre les quatre controllers CMT : CreditMoyenTerme, EcheanceCMT, batch et config.
 * Le {@code GlobalExceptionHandler} sert de filet pour toute exception non
 * traitée ici (sécurité, erreurs techniques imprévues, etc.).
 */
@RestControllerAdvice(assignableTypes = {
        CreditMoyenTermeController.class,
        EcheanceCMTController.class,
        CmtBatchController.class,
        CmtConfigController.class
})
public class CmtExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CmtExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        log.warn("CMT — resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage(), ex));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        log.warn("CMT — validation error: {}", ex.getMessage());
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
        log.warn("CMT — bean validation error: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad Request", message, null, Instant.now()));
    }
}
