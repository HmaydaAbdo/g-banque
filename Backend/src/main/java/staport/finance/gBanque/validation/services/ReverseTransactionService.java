package staport.finance.gBanque.validation.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ReversalException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
import staport.finance.gBanque.rapprochement.services.SoldeCascadeService;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.transactions.repositories.TransactionRepository;
import staport.finance.gBanque.validation.services.handlers.TransactionReversalHandler;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Annule une transaction en inversant ses effets (remet l'entité source à son état initial,
 * supprime la transaction, recalcule la cascade des soldes si besoin).
 * <p>
 * Toutes les sources sont portées par un registre de {@link TransactionReversalHandler} (migration
 * du 19/09/2026, une implémentation {@code @Component} par source, découverte et injectée
 * automatiquement par Spring, même mécanisme que {@code BatchValidationHandler} côté validation —
 * voir Javadoc de {@link TransactionReversalHandler}) — {@link #resetEntiteSource} délègue
 * systématiquement au handler enregistré pour la {@link TransactionSource} de la transaction ; il
 * n'y a plus de switch. Ajouter une nouvelle source ne demande plus de modifier cette classe,
 * seulement d'ajouter une implémentation de {@link TransactionReversalHandler} dans le module de
 * cette source.
 */
@Service
public class ReverseTransactionService {

    private static final Logger log = LoggerFactory.getLogger(ReverseTransactionService.class);

    private final TransactionRepository transactionRepository;
    private final SoldeCascadeService soldeCascadeService;

    /** Un handler par {@link TransactionSource} — voir Javadoc de la classe. */
    private final Map<TransactionSource, TransactionReversalHandler> handlerRegistry;

    public ReverseTransactionService(
            TransactionRepository transactionRepository,
            SoldeCascadeService soldeCascadeService,
            List<TransactionReversalHandler> reversalHandlers) {
        this.transactionRepository = transactionRepository;
        this.soldeCascadeService = soldeCascadeService;
        this.handlerRegistry = reversalHandlers.stream()
                .collect(Collectors.toMap(TransactionReversalHandler::source, h -> h, (a, b) -> {
                    throw new IllegalStateException(
                            "Deux TransactionReversalHandler enregistrés pour la même source: " + a.source());
                }));
    }

    /**
     * Annule une transaction en inversant ses effets
     *
     * @param transactionId ID de la transaction à annuler
     * @throws ReversalException si le type de transaction n'est pas supporté pour l'annulation
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @AuditableAction(operation = AuditOperation.DELETE, entity = Transaction.class)
    public void annulerTransaction(Long transactionId) {
        log.info("Début de l'annulation de la transaction ID: {}", transactionId);

        // 1. Récupérer la transaction
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction introuvable avec l'ID: " + transactionId));

        // Guard — rapprochement définitivement clôturé : aucune modification autorisée
        if (transaction.getRapprochement().getStatut() == StatutRapprochement.CLOTURE_DEFINITIVE) {
            throw new ValidationException(
                    "Impossible d'annuler la transaction " + transactionId
                    + " : le rapprochement est définitivement clôturé.");
        }

        inverserEtSupprimer(transaction);
        log.info("Transaction {} annulée et supprimée avec succès", transactionId);
    }

    /**
     * Résultat de {@link #annulerSiRapproche} : l'identifiant de la transaction supprimée, ou {@code null} si
     * l'opération source n'était rapprochée dans aucune transaction. L'accesseur {@code transactionId} sert à l'audit.
     */
    public record AnnulationLiee(Long transactionId) {
        public boolean effectuee() {
            return transactionId != null;
        }
    }

    /**
     * Retour « en cours » d'une opération source déjà validée (bascule manuelle du statut, réservée à l'administrateur
     * système) : si une transaction de rapprochement pointe sur elle, cette transaction est annulée exactement comme
     * par {@link #annulerTransaction} — l'opération source retrouve son état initial (statut « à valider », dates de
     * valeur et d'opération effacées), la transaction est supprimée et les soldes en cascade sont recalculés. Sans
     * transaction liée, rien n'est fait : l'appelant garde sa bascule de statut libre (correction manuelle).
     *
     * <p>Refusé si le rapprochement est définitivement clôturé, ou si plusieurs transactions pointent sur la même
     * opération (cas ambigu : à traiter depuis le rapprochement).
     *
     * @param sources    sources de transaction possibles de l'opération (plusieurs pour les LCN)
     * @param referentId identifiant de l'opération source
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @AuditableAction(operation = AuditOperation.DELETE, entity = Transaction.class, idAccessor = "transactionId")
    public AnnulationLiee annulerSiRapproche(Collection<TransactionSource> sources, Long referentId) {
        List<Transaction> liees = transactionRepository.findByTransactionSourceInAndReferent(sources, referentId);
        if (liees.isEmpty()) {
            return new AnnulationLiee(null);
        }
        if (liees.size() > 1) {
            throw new ValidationException(
                    "Impossible de remettre cette opération en cours : " + liees.size()
                    + " transactions de rapprochement pointent sur elle. Annulez-les depuis le rapprochement.");
        }
        Transaction transaction = liees.get(0);
        if (transaction.getRapprochement().getStatut() == StatutRapprochement.CLOTURE_DEFINITIVE) {
            throw new ValidationException(
                    "Impossible de remettre cette opération en cours : sa transaction " + transaction.getId()
                    + " appartient à un rapprochement définitivement clôturé.");
        }
        Long transactionId = transaction.getId();
        log.info("Retour en cours de l'opération {} [ID: {}] : annulation de la transaction {}", transaction.getTransactionSource(), referentId, transactionId);
        inverserEtSupprimer(transaction);
        return new AnnulationLiee(transactionId);
    }

    /** Inverse les effets de la transaction : source remise à l'état initial, transaction supprimée, soldes recalculés. */
    private void inverserEtSupprimer(Transaction transaction) {
        // Capturer les informations du rapprochement AVANT la suppression (pour la cascade)
        StatutRapprochement statutRapprochement = transaction.getRapprochement().getStatut();
        Long banqueId = transaction.getBanqueDePrelevement().getId();

        // Remettre l'entité source à son état initial
        resetEntiteSource(transaction);

        // Supprimer la transaction
        transactionRepository.delete(transaction);

        // Si le rapprochement était CLOTURE_PROVISOIRE → recalculer la cascade des soldes
        if (statutRapprochement == StatutRapprochement.CLOTURE_PROVISOIRE) {
            soldeCascadeService.recalculerCascade(banqueId);
        }
    }

    /**
     * Remet l'entité source à son état initial selon le type de transaction.
     */
    private void resetEntiteSource(Transaction transaction) {
        Long referentId = transaction.getReferent();
        TransactionSource source = transaction.getTransactionSource();

        log.debug("Reset entité source: {} [ID: {}]", source, referentId);

        TransactionReversalHandler handler = handlerRegistry.get(source);
        if (handler == null) {
            throw new ReversalException("Type de transaction non supporté pour l'annulation: " + source);
        }
        handler.reset(referentId);
    }
}
