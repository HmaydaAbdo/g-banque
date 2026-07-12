package staport.finance.gBanque.moyensDePaiements.cheques.exceptions;

/**
 * Exception levée lors de la violation d'une règle métier propre au module Chèque
 * (ex. modification d'un chèque déjà réglé, changement de statut d'un chèque annulé).
 */
public class ChequeBusinessException extends RuntimeException {
    public ChequeBusinessException(String message) {
        super(message);
    }
}
