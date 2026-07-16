package staport.finance.gBanque.rapprochement.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.rapprochement.dtos.RapprochementAggregates;
import staport.finance.gBanque.rapprochement.entities.Rapprochement;
import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
import staport.finance.gBanque.rapprochement.repository.RapprochementRepository;
import staport.finance.gBanque.transactions.dtos.TransactionAggregatesDto;
import staport.finance.gBanque.transactions.service.ITransactionService;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Maintains the chain of opening balances between a CLOTURE_PROVISOIRE
 * rapprochement and the following EN_COURS rapprochement whenever
 * a transaction is added to or removed from the provisional period.
 *
 * <p>Rule: soldeFin(PROVISOIRE) = soldeDebut(EN_COURS)</p>
 *
 * <p>This service is intentionally lightweight — it recalculates via a
 * single aggregate query (no transaction list in memory) and performs at
 * most two JPA saves per call.</p>
 */
@Service
public class SoldeCascadeService {

    private static final Logger log = LoggerFactory.getLogger(SoldeCascadeService.class);

    private final RapprochementRepository rapprochementRepository;
    private final ITransactionService transactionService;

    /**
     * {@code ITransactionService} est injecté en {@code @Lazy} : {@code SoldeCascadeService} est
     * appelé par les 18 {@code XxxValidationService} (via {@code processXxxTransaction}) et par
     * {@code ReverseTransactionService}, tous eux-mêmes appelés par
     * {@code TransactionServiceImpl.updateTransactionById()} (dispatch de synchronisation,
     * architecture.md — Règle 2). Une injection directe (non {@code @Lazy}) fermerait un cycle de
     * bean Spring transitif : {@code SoldeCascadeService → ITransactionService →
     * TransactionServiceImpl → IXxxValidationService → XxxValidationService →
     * SoldeCascadeService}. Voir mémoire {@code feedback_gbanque} (leçon Leasing/Prélèvements) —
     * ne jamais revenir à l'injection directe d'un Repository pour "corriger" ce point.
     */
    public SoldeCascadeService(
            RapprochementRepository rapprochementRepository,
            @Lazy ITransactionService transactionService) {
        this.rapprochementRepository = rapprochementRepository;
        this.transactionService      = transactionService;
    }

    /**
     * Recalculates soldeFin of the CLOTURE_PROVISOIRE rapprochement for
     * {@code banqueId}, then propagates the new value as soldeDebut of
     * the EN_COURS rapprochement.
     *
     * <p>No-op if no CLOTURE_PROVISOIRE exists for this bank — there is
     * nothing to cascade in that case.</p>
     *
     * @param banqueId the bank whose provisional rapprochement must be refreshed
     */
    @Transactional
    public void recalculerCascade(Long banqueId) {

        Optional<Rapprochement> provisoireOpt =
                rapprochementRepository.findByBanqueIdAndStatut(banqueId, StatutRapprochement.CLOTURE_PROVISOIRE);

        if (provisoireOpt.isEmpty()) {
            // No provisional rapprochement — nothing to cascade.
            return;
        }

        Rapprochement provisoire = provisoireOpt.get();

        // Recompute soldeFin of the provisional rapprochement — single SQL round-trip.
        RapprochementAggregates agg = fetchAggregates(provisoire.getId());
        BigDecimal newSoldeFin = agg.soldeFin(provisoire.getSoldeDebut());

        log.debug("Cascade [banqueId={}] provisoire id={} soldeDebut={} → newSoldeFin={}",
                banqueId, provisoire.getId(), provisoire.getSoldeDebut(), newSoldeFin);

        // Propagate to EN_COURS rapprochement.
        rapprochementRepository
                .findByBanqueIdAndStatut(banqueId, StatutRapprochement.EN_COURS)
                .ifPresent(enCours -> {
                    log.debug("Cascade → EN_COURS id={} soldeDebut {} → {}",
                            enCours.getId(), enCours.getSoldeDebut(), newSoldeFin);
                    enCours.setSoldeDebut(newSoldeFin);
                    rapprochementRepository.save(enCours);
                });
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private RapprochementAggregates fetchAggregates(Long rapprochementId) {
        TransactionAggregatesDto dto = transactionService.computeAggregatesForRapprochement(rapprochementId);
        return new RapprochementAggregates(
                dto.totalDebit(), dto.totalCredit(), dto.totalTvaDeductible(), dto.totalTvaCollectee());
    }
}
