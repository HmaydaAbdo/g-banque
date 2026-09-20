package staport.finance.gBanque.validation.services.handlers;

import staport.finance.gBanque.transactions.enums.TransactionSource;

/**
 * Gère l'annulation (reset) d'une source pour {@code ReverseTransactionService} : remet l'entité
 * source à son état initial (statut non-payé, dates d'opération/valeur effacées) — miroir exact de
 * {@link BatchValidationHandler} côté annulation plutôt que validation.
 * <p>
 * Une implémentation par source, découverte automatiquement par Spring (chaque implémentation est
 * un {@code @Component}) et enregistrée dans {@code ReverseTransactionService} via
 * {@link #source()} — ajouter une source à l'annulation ne demande plus de modifier
 * {@code ReverseTransactionService}, seulement d'ajouter une implémentation dans le module de
 * cette source.
 * <p>
 * Ajouté le 19/09/2026 (architecture.md, Règle 1) en remplacement du switch de 20 cas que portait
 * auparavant {@code ReverseTransactionService.resetEntiteSource} (désormais supprimé) — même
 * moteur que celui déjà utilisé pour la validation ({@link BatchValidationHandler}), appliqué au
 * domaine symétrique de l'annulation.
 */
public interface TransactionReversalHandler {

    /** Source gérée par cette implémentation — clé d'enregistrement dans le registre. */
    TransactionSource source();

    /** Remet l'entité source (identifiée par {@code referentId}) à son état initial. */
    void reset(Long referentId);
}
