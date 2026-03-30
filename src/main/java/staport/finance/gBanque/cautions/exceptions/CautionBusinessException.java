package staport.finance.gBanque.cautions.exceptions;

/**
 * Exception métier spécifique au module Caution.
 *
 * <p>Levée lors de violations de règles métier propres aux cautions, par exemple :
 * <ul>
 *   <li>Tentative de soft-delete d'une caution déjà inactive</li>
 *   <li>Tentative de modification d'une caution inactive</li>
 *   <li>Activation d'une caution déjà active</li>
 * </ul>
 *
 * <p>Interceptée par {@link CautionExceptionHandler} → HTTP 422 Unprocessable Entity.
 */
public class CautionBusinessException extends RuntimeException {

    public CautionBusinessException(String message) {
        super(message);
    }

    public CautionBusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
