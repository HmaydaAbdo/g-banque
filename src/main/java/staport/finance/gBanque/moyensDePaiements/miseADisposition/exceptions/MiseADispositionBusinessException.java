package staport.finance.gBanque.moyensDePaiements.miseADisposition.exceptions;

/**
 * Exception levée lors de la violation d'une règle métier propre au module Mise à Disposition
 * (ex. modification d'une mise à disposition déjà réglée, changement de statut d'une mise à
 * disposition annulée).
 */
public class MiseADispositionBusinessException extends RuntimeException {
    public MiseADispositionBusinessException(String message) {
        super(message);
    }
}
