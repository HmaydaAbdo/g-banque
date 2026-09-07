package staport.finance.gBanque.prefinancement.validation.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.repository.PrefinancementRepository;
import staport.finance.gBanque.prefinancement.validation.dto.PrefinancementTransactionRequest;
import staport.finance.gBanque.prefinancement.validation.dto.PrefinancementTransactionResponse;
import staport.finance.gBanque.prefinancement.validation.mapper.PrefinancementTransactionMapper;
import staport.finance.gBanque.prefinancement.validation.services.interfaces.IPrefinancementValidationService;
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
 * Implémentation du service de validation des préfinancements.
 *
 * <p>Coordonne la création de la transaction bancaire, l'attachement au rapprochement
 * et la mise à jour du préfinancement source. Extrait du {@code ValidationService}
 * générique pour respecter le bounded context du module prefinancement (architecture.md,
 * Règle 1).
 */
@Service
public class PrefinancementValidationService implements IPrefinancementValidationService {

    private static final Logger log = LoggerFactory.getLogger(PrefinancementValidationService.class);

    private final PrefinancementRepository prefinancementRepository;
    private final PrefinancementTransactionMapper prefinancementTransactionMapper;
    private final TransactionRepository transactionRepository;
    private final RapprochementRepository rapprochementRepository;
    private final SoldeCascadeService soldeCascadeService;

    /**
     * {@code @Lazy} obligatoire — même précaution que {@code PrelevementValidationService} (voir
     * historique des correctifs, entrée du 12/07/2026) : sans lui, cycle de bean Spring au
     * démarrage ({@code BanqueServiceImpl} → {@code ITransactionService} →
     * {@code IPrefinancementValidationService} → {@code IBanqueService} → {@code BanqueServiceImpl}).
     * Le proxy lazy ne résout la dépendance réelle qu'au premier appel de
     * {@link #processPrefinancementTransaction}, après la fin du démarrage du contexte.
     */
    private final IBanqueService banqueService;

    public PrefinancementValidationService(
            PrefinancementRepository prefinancementRepository,
            PrefinancementTransactionMapper prefinancementTransactionMapper,
            TransactionRepository transactionRepository,
            RapprochementRepository rapprochementRepository,
            SoldeCascadeService soldeCascadeService,
            @Lazy IBanqueService banqueService) {
        this.prefinancementRepository = prefinancementRepository;
        this.prefinancementTransactionMapper = prefinancementTransactionMapper;
        this.transactionRepository = transactionRepository;
        this.rapprochementRepository = rapprochementRepository;
        this.soldeCascadeService = soldeCascadeService;
        this.banqueService = banqueService;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Prefinancement.class, idAccessor = "getPrefinancementId")
    public PrefinancementTransactionResponse processPrefinancementTransaction(PrefinancementTransactionRequest requestDto) {
        log.info("Validation préfinancement — id: {}", requestDto.getPrefinancementId());

        Prefinancement prefinancement = prefinancementRepository.findById(requestDto.getPrefinancementId())
                .orElseThrow(() -> {
                    log.warn("Préfinancement introuvable — id: {}", requestDto.getPrefinancementId());
                    return new ResourceNotFoundException("Prefinancement with id " + requestDto.getPrefinancementId() + " not found");
                });

        if (prefinancement.getStatus() == Status.PAYE) {
            log.warn("Tentative de validation d'un préfinancement déjà PAYE — id: {}", prefinancement.getId());
            throw new ValidationException("Le préfinancement id: " + prefinancement.getId() + " est déjà payé.");
        }

        Marche marche = prefinancement.getMarche();
        if (marche == null) {
            throw new ResourceNotFoundException("Marché introuvable pour le préfinancement " + prefinancement.getId());
        }

        Banque banque = banqueService.findOrThrow(requestDto.getBanqueId());
        if (marche.getBanque() == null || !marche.getBanque().getId().equals(banque.getId())) {
            throw new ValidationException("Le banqueId fourni (" + requestDto.getBanqueId()
                    + ") ne correspond pas à la banque du marché associé au préfinancement "
                    + prefinancement.getId() + ".");
        }

        // Règles de saisie de la TVA (taux choisi XOR montant saisi, bornes) : logique métier,
        // donc portée par le service et non par le mapper (conventions/architecture.md).
        BigDecimal montantTva = TvaResolver.resoudre(
                prefinancement.getMontant(), requestDto.getTauxTva(), requestDto.getMontantTva());

        Transaction transactionEntity = prefinancementTransactionMapper.toEntity(requestDto, prefinancement, banque, montantTva);
        Transaction savedTransaction = saveTransactionWithCascade(
                transactionEntity, banque.getId(), requestDto.getRapprochementId());

        prefinancement.setStatus(Status.PAYE);
        prefinancement.setDateOperation(savedTransaction.getDateOperations());
        prefinancement.setDateValeur(savedTransaction.getDateValeur());
        prefinancement.setObservation(savedTransaction.getObservation());
        prefinancement.setLibelle(savedTransaction.getLibelle());
        prefinancementRepository.save(prefinancement);

        log.info("Préfinancement id: {} validé — transaction créée id: {}", prefinancement.getId(), savedTransaction.getId());
        return prefinancementTransactionMapper.toResponseDto(savedTransaction);
    }

    @Override
    @Transactional
    public void syncPrefinancementFromTransaction(Long prefinancementId, LocalDate dateValeur, LocalDate dateOperation) {
        log.info("Synchronisation préfinancement depuis transaction — id: {}", prefinancementId);

        Prefinancement prefinancement = prefinancementRepository.findById(prefinancementId)
                .orElseThrow(() -> {
                    log.warn("Préfinancement introuvable pour synchronisation — id: {}", prefinancementId);
                    return new ResourceNotFoundException("Préfinancement introuvable avec l'ID: " + prefinancementId);
                });

        prefinancement.setDateValeur(dateValeur);
        prefinancement.setDateOperation(dateOperation);
        prefinancementRepository.save(prefinancement);

        log.info("Préfinancement id: {} synchronisé depuis transaction", prefinancementId);
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
