package staport.finance.gBanque.moyensDePaiements.virementInternational.exceptions;

/**
 * Exception levée lors de la violation d'une règle métier propre au module Virement International
 * (ex. modification d'un virement international déjà réglé, changement de statut d'un virement
 * international annulé).
 */
public class VirementInternationalBusinessException extends RuntimeException {
    public VirementInternationalBusinessException(String message) {
        super(message);
    }
}
