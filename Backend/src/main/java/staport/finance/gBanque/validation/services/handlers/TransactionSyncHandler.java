package staport.finance.gBanque.validation.services.handlers;

import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.time.LocalDate;

/**
 * Propage la modification d'une transaction déjà validée vers son entité source pour
 * {@code TransactionServiceImpl.updateTransactionById} (architecture.md, Règle 2) — troisième
 * membre de la même famille de registres que {@link BatchValidationHandler} (créer) et
 * {@link staport.finance.gBanque.validation.services.handlers.TransactionReversalHandler}
 * (annuler) : une implémentation par source, découverte automatiquement par Spring, enregistrée
 * via {@link #source()}.
 * <p>
 * Chaque implémentation délègue directement à la méthode {@code syncXxxFromTransaction} de son
 * {@code IXxxValidationService} — c'est cette méthode qui porte la logique métier (Règle 2 :
 * interface obligatoire), le handler n'est qu'un point de routage.
 */
public interface TransactionSyncHandler {

    /** Source gérée par cette implémentation — clé d'enregistrement dans le registre. */
    TransactionSource source();

    /** Propage les champs édités de la transaction vers l'entité source identifiée par {@code referentId}. */
    void sync(Long referentId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation);
}
