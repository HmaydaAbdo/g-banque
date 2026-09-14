package staport.finance.gBanque.transactions.enums;

/**
 * Sens d'une opération de rapprochement pour les types de moyens de paiement qui distinguent
 * décaissement et encaissement (virement, chèque, LCN, mise à disposition, virement international).
 * <p>
 * Utilisé uniquement par le flux de validation en lot ({@code BatchValidationService}) pour savoir
 * quelle méthode unitaire de {@code ValidationService} appeler. Les endpoints unitaires existants
 * encodent déjà le sens dans l'URL (ex. {@code /process/decaissement/virement}) et n'ont donc pas
 * besoin de cet enum.
 */
public enum SensOperation {
    DECAISSEMENT,
    ENCAISSEMENT
}
