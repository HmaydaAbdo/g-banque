package staport.finance.gBanque.moyensDePaiements.virements.exceptions;

/**
 * Exception levée lors de la violation d'une règle métier propre au module Virement
 * (ex. modification d'un virement déjà réglé, changement de statut d'un virement annulé).
 */
public class VirementBusinessException extends RuntimeException {
    public VirementBusinessException(String message) {
        super(message);
    }
}
