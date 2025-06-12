
package staport.finance.gBanque.exception.customExceptions; // Or staport.finance.gBanque.Bank.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND) // This annotation will be picked up by @ControllerAdvice
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}