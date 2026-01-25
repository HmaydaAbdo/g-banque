package staport.finance.gBanque.exception.customExceptions;

public class HistoriqueAlreadyExistsException extends RuntimeException {
    public HistoriqueAlreadyExistsException(String message) {
        super(message);
    }
}