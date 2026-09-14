package staport.finance.gBanque.exception.customExceptions;

public class InvalidChequeDataException extends RuntimeException {
    public InvalidChequeDataException(String message) {
        super(message);
    }
}