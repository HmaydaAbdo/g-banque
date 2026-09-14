package staport.finance.gBanque.moyensDePaiements.lcn.exceptions;

/**
 * Exception levée lors de la violation d'une règle métier propre au module LCN
 * (ex. modification d'un LCN déjà réglé, changement de statut d'un LCN annulé,
 * violation de la règle ESCOMPTE ⇒ ENCAISSEMENT).
 */
public class LcnBusinessException extends RuntimeException {
    public LcnBusinessException(String message) {
        super(message);
    }
}
