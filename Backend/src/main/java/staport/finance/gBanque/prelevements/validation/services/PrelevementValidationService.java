package staport.finance.gBanque.prelevements.validation.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prelevements.entitie.Prelevement;
import staport.finance.gBanque.prelevements.repository.PrelevementRepository;
import staport.finance.gBanque.prelevements.validation.dto.PrelevementTransactionResponse;
import staport.finance.gBanque.prelevements.validation.dto.PrelevementValidationRequest;
import staport.finance.gBanque.prelevements.validation.mapper.PrelevementTransactionMapper;
import staport.finance.gBanque.prelevements.validation.services.interfaces.IPrelevementValidationService;
import staport.finance.gBanque.rapprochement.entities.Rapprochement;
import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
import staport.finance.gBanque.rapprochement.repository.RapprochementRepository;
import staport.finance.gBanque.rapprochement.services.SoldeCascadeService;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.repositories.TransactionRepository;
import staport.finance.gBanque.validation.utils.TvaResolver;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Implémentation du service de validation des prélèvements.
 *
 * <p>Coordonne la création de la transaction bancaire, l'attachement au rapprochement
 * et la mise à jour du prélèvement source. Extrait du {@code ValidationService}
 * générique pour respecter le bounded context du module prelevements (architecture.md,
 * Règle 1).
 */
@Service
public class PrelevementValidationService implements IPrelevementValidationService {

    private static final Logger log = LoggerFactory.getLogger(PrelevementValidationService.class);

    private final PrelevementRepository prelevementRepository;
    private final PrelevementTransactionMapper prelevementTransactionMapper;
    private final TransactionRepository transactionRepository;
    private final RapprochementRepository rapprochementRepository;
    private final SoldeCascadeService soldeCascadeService;

    /**
     * {@code @Lazy} obligatoire — sans lui, cycle de bean Spring au démarrage :
     * {@code BanqueServiceImpl} dépend de {@code ITransactionService}, qui dépend de
     * {@code IPrelevementValidationService} (dispatch dans {@code TransactionService}), qui dépend
     * ici de {@code IBanqueService} → boucle fermée sur {@code BanqueServiceImpl}. Le proxy lazy
     * ne résout la dépendance réelle qu'au premier appel de {@link #processPrelevementToTransaction},
     * après la fin du démarrage du contexte. Corrigé le 12/07/2026 (app en échec de démarrage —
     * `ApplicationContext` cycle report).
     */
    private final IBanqueService banqueService;

    public PrelevementValidationService(
            PrelevementRepository prelevementRepository,
            PrelevementTransactionMapper prelevementTransactionMapper,
            TransactionRepository transactionRepository,
            RapprochementRepository rapprochementRepository,
            SoldeCascadeService soldeCascadeService,
            @Lazy IBanqueService banqueService) {
        this.prelevementRepository = prelevementRepository;
        this.prelevementTransactionMapper = prelevementTransactionMapper;
        this.transactionRepository = transactionRepository;
        this.rapprochementRepository = rapprochementRepository;
        this.soldeCascadeService = soldeCascadeService;
        this.banqueService = banqueService;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Prelevement.class, idAccessor = "getPrelevementId")
    public PrelevementTransactionResponse processPrelevementToTransaction(PrelevementValidationRequest requestDto) {
        log.info("Validation prélèvement — id: {}", requestDto.getIdPrelevement());

        Prelevement prelevement = prelevementRepository.findById(requestDto.getIdPrelevement())
                .orElseThrow(() -> {
                    log.warn("Prélèvement introuvable — id: {}", requestDto.getIdPrelevement());
                    return new ResourceNotFoundException("Prelevement with id " + requestDto.getIdPrelevement() + " not found");
                });

        if (prelevement.getStatus() == Status.PAYE) {
            log.warn("Tentative de validation d'un prélèvement déjà PAYE — id: {}", prelevement.getId());
            throw new ValidationException("Le prélèvement id: " + prelevement.getId() + " est déjà payé.");
        }

        // Règles de saisie de la TVA (taux choisi XOR montant saisi, bornes) : logique métier,
        // donc portée par le service et non par le mapper (conventions/architecture.md).
        BigDecimal montantTva = TvaResolver.resoudre(
                prelevement.getMontant(), requestDto.getTauxTva(), requestDto.getMontantTva());

        Transaction transactionEntity = prelevementTransactionMapper.toEntity(requestDto, prelevement, montantTva);

        // Vérifie que la banque du prélèvement existe toujours (garde d'intégrité, cf. ancien
        // comportement de ValidationService.processPrelevementToTransaction).
        banqueService.findOrThrow(prelevement.getBanque().getId());

        Transaction savedTransaction = saveTransactionWithCascade(
                transactionEntity, prelevement.getBanque().getId(), requestDto.getRapprochementId());

        prelevement.setStatus(Status.PAYE);
        prelevement.setObservation(savedTransaction.getObservation());
        prelevement.setLibelle(savedTransaction.getLibelle());
        prelevement.setDateValeur(savedTransaction.getDateValeur());
        prelevement.setDateOperation(savedTransaction.getDateOperations());
        prelevementRepository.save(prelevement);

        log.info("Prélèvement id: {} validé — transaction créée id: {}", prelevement.getId(), savedTransaction.getId());
        return prelevementTransactionMapper.toResponseDto(savedTransaction);
    }

    @Override
    @Transactional
    public void syncPrelevementFromTransaction(Long prelevementId, LocalDate dateValeur, LocalDate dateOperation) {
        log.info("Synchronisation prélèvement depuis transaction — id: {}", prelevementId);

        Prelevement prelevement = prelevementRepository.findById(prelevementId)
                .orElseThrow(() -> {
                    log.warn("Prélèvement introuvable pour synchronisation — id: {}", prelevementId);
                    return new ResourceNotFoundException("Prélèvement introuvable avec l'ID: " + prelevementId);
                });

        prelevement.setDateValeur(dateValeur);
        prelevement.setDateOperation(dateOperation);
        prelevementRepository.save(prelevement);

        log.info("Prélèvement id: {} synchronisé depuis transaction", prelevementId);
    }

    // -------------------------------------------------------------------------
    // Méthodes privées
    // -------------------------------------------------------------------------

    /**
     * Attache la transaction au rapprochement cible, la sauvegarde
     * et déclenche le recalcul de solde en cascade si nécessaire.
     */
    private Transaction saveTransactionWithCascade(Transaction transaction, Long banqueId, Long rapprochementId) {
        if (rapprochementId == null) {
            throw new ValidationException(
                    "rapprochementId est obligatoire. Veuillez sélectionner le rapprochement cible avant de valider.");
        }

        Rapprochement rapprochement = rapprochementRepository.findById(rapprochementId)
                .orElseThrow(() -> new ValidationException(
                        "Rapprochement introuvable avec l'ID: " + rapprochementId));

        if (rapprochement.getStatut() != StatutRapprochement.EN_COURS
                && rapprochement.getStatut() != StatutRapprochement.CLOTURE_PROVISOIRE) {
            throw new ValidationException(
                    "Impossible de valider vers un rapprochement " + rapprochement.getStatut()
                    + ". Seuls EN_COURS et CLOTURE_PROVISOIRE sont acceptés.");
        }

        transaction.setRapprochement(rapprochement);
        Transaction saved = transactionRepository.save(transaction);

        if (rapprochement.getStatut() == StatutRapprochement.CLOTURE_PROVISOIRE) {
            soldeCascadeService.recalculerCascade(banqueId);
        }

        return saved;
    }
}
