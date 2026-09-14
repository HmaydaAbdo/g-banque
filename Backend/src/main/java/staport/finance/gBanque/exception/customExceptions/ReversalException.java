package staport.finance.gBanque.exception.customExceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Levée lorsqu'une annulation de transaction ({@code ReverseTransactionService.annulerTransaction})
 * échoue — type de {@code TransactionSource} non supporté pour l'annulation, ou remise à l'état
 * initial de l'entité source impossible.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class ReversalException extends RuntimeException {
    public ReversalException(String message) {
        super(message);
    }

    public ReversalException(String message, Throwable cause) {
        super(message, cause);
    }
}
