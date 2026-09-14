package staport.finance.gBanque.prelevements.exceptions;

/**
 * Exception levée lors de la violation d'une règle métier propre au module Prélèvements
 * (ex. modification ou suppression d'un prélèvement déjà payé).
 */
public class PrelevementBusinessException extends RuntimeException {
    public PrelevementBusinessException(String message) {
        super(message);
    }
}
