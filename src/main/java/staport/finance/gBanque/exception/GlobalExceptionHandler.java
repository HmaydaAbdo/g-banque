package staport.finance.gBanque.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import staport.finance.gBanque.exception.customExceptions.DuplicateEntryException;
import staport.finance.gBanque.exception.customExceptions.FileStorageException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;

import java.util.HashMap;
import java.util.Map;

import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);



    /**
     * Handles Spring Security's BadCredentialsException (e.g., incorrect username/password during authentication).
     * Returns 401 Unauthorized.
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleSpringSecurityBadCredentialsException(BadCredentialsException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Unauthorized");
        errorResponse.put("message", ex.getMessage() != null ? ex.getMessage() : "Invalid username or password.");
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles AuthorizationException (e.g., authenticated user lacks permissions).
     * Returns 403 Forbidden.
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleAuthorizationException(AuthorizationDeniedException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Forbidden");
        errorResponse.put("message", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    /**
     * Handles validation errors from @Valid annotation.
     * Returns 400 Bad Request with field-specific error details.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Bad Request");
        errorResponse.put("message", "Validation failed for one or more fields.");

        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value"
                ));

        errorResponse.put("fieldErrors", fieldErrors);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles custom ResourceNotFoundException.
     * Returns 404 Not Found.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Not Found");
        errorResponse.put("message", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles custom DuplicateEntryException.
     * Returns 409 Conflict.
     */
    @ExceptionHandler(DuplicateEntryException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleDuplicateEntryException(DuplicateEntryException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Conflict");
        errorResponse.put("message", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Handles custom FileStorageException.
     * Returns 500 Internal Server Error.
     */
    @ExceptionHandler(FileStorageException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleFileStorageException(FileStorageException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "File Processing Error");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("detail", ex.getCause() != null ? ex.getCause().getMessage() : null);
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }



    /**
     * Handles IllegalArgumentException, typically thrown for invalid method arguments or business rule violations.
     * Returns 400 Bad Request.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Bad Request");
        errorResponse.put("message", ex.getMessage() != null ? ex.getMessage() : "Invalid argument provided.");
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles custom ValidationException.
     * Returns 400 Bad Request.
     */
    @ExceptionHandler(ValidationException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleValidationException(ValidationException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Bad Request");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("detail", ex.getMessage());
        errorResponse.put("exception Type", ex.getClass().getName());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Catches any other unhandled exceptions.
     * Returns 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleGeneralException(Exception ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Internal Server Error");
        errorResponse.put("message", "An unexpected error occurred. Please try again later.");
        errorResponse.put("detail", ex.getMessage());
        errorResponse.put("exception Type", ex.getClass().getName());
        this.log.error("An unhandled exception occurred: ", ex);
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}