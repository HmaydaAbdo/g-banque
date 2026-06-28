package staport.finance.gBanque.Agios.exceptions;

/**
 * Levée lors d'une violation d'une règle métier propre au module Agios.
 * Interceptée par {@link AgiosExceptionHandler} → HTTP 422.
 */
public class AgiosBusinessException extends RuntimeException {

    public AgiosBusinessException(String message) {
        super(message);
    }

    public AgiosBusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
