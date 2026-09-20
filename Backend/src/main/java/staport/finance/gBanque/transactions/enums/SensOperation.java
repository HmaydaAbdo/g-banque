package staport.finance.gBanque.transactions.enums;

/**
 * Sens d'une opération de rapprochement pour les types de moyens de paiement qui distinguent
 * décaissement et encaissement (virement, chèque, LCN, mise à disposition, virement international).
 * <p>
 * Utilisé par le flux de validation en lot ({@code BatchValidationService.requireSensIfNeeded})
 * pour imposer un sens homogène à la sélection avant de déléguer au
 * {@code BatchValidationHandler} de la source — depuis la fusion du 19/09/2026 des méthodes de
 * validation unitaires à deux sens en une seule par source (le sens réel est lu directement sur
 * l'entité par le mapper), ce sens n'est plus consommé par le handler lui-même, seulement validé
 * en amont pour la cohérence de la sélection en lot.
 */
public enum SensOperation {
    DECAISSEMENT,
    ENCAISSEMENT
}
