package staport.finance.gBanque.exception.customExceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST) // This makes Spring return a 400 Bad Request status
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}