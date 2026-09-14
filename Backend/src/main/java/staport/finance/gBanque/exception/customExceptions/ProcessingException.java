package staport.finance.gBanque.exception.customExceptions;

/**
 * Exception thrown when an error occurs during the processing of a functional line
 * in the batch job.
 */
public class ProcessingException extends RuntimeException {

    public ProcessingException(String message) {
        super(message);
    }

    public ProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProcessingException(Throwable cause) {
        super(cause);
    }
}