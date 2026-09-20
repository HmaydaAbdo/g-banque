package staport.finance.gBanque.factoring.validation.services;

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
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.factoring.repository.FactoringRepository;
import staport.finance.gBanque.factoring.validation.dto.FactoringTransactionRequest;
import staport.finance.gBanque.factoring.validation.dto.FactoringTransactionResponse;
import staport.finance.gBanque.factoring.validation.mapper.FactoringTransactionMapper;
import staport.finance.gBanque.factoring.validation.services.interfaces.IFactoringValidationService;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.enums.Status;
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
 * Implémentation du service de validation des factorings.
 *
 * <p>Coordonne la création de la transaction bancaire, l'attachement au rapprochement
 * et la mise à jour du factoring source. Extrait du {@code ValidationService}
 * générique pour respecter le bounded context du module factoring (architecture.md,
 * Règle 1).
 */
@Service
public class FactoringValidationService implements IFactoringValidationService {

    private static final Logger log = LoggerFactory.getLogger(FactoringValidationService.class);

    private final FactoringRepository factoringRepository;
    private final FactoringTransactionMapper factoringTransactionMapper;
    private final TransactionRepository transactionRepository;
    private final RapprochementRepository rapprochementRepository;
    private final SoldeCascadeService soldeCascadeService;

    /**
     * {@code @Lazy} obligatoire — même précaution que {@code PrelevementValidationService} (voir
     * historique des correctifs, entrée du 12/07/2026) : sans lui, cycle de bean Spring au
     * démarrage ({@code BanqueServiceImpl} → {@code ITransactionService} →
     * {@code IFactoringValidationService} → {@code IBanqueService} → {@code BanqueServiceImpl}).
     */
    private final IBanqueService banqueService;

    public FactoringValidationService(
            FactoringRepository factoringRepository,
            FactoringTransactionMapper factoringTransactionMapper,
            TransactionRepository transactionRepository,
            RapprochementRepository rapprochementRepository,
            SoldeCascadeService soldeCascadeService,
            @Lazy IBanqueService banqueService) {
        this.factoringRepository = factoringRepository;
        this.factoringTransactionMapper = factoringTransactionMapper;
        this.transactionRepository = transactionRepository;
        this.rapprochementRepository = rapprochementRepository;
        this.soldeCascadeService = soldeCascadeService;
        this.banqueService = banqueService;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Factoring.class, idAccessor = "getFactoringId")
    public FactoringTransactionResponse processFactoringTransaction(FactoringTransactionRequest requestDto) {
        log.info("Validation factoring — id: {}", requestDto.getFactoringId());

        Factoring factoring = factoringRepository.findById(requestDto.getFactoringId())
                .orElseThrow(() -> {
                    log.warn("Factoring introuvable — id: {}", requestDto.getFactoringId());
                    return new ResourceNotFoundException("Factoring with id " + requestDto.getFactoringId() + " not found");
                });

        if (factoring.getStatus() == Status.PAYE) {
            log.warn("Tentative de validation d'un factoring déjà PAYE — id: {}", factoring.getId());
            throw new ValidationException("Le factoring id: " + factoring.getId() + " est déjà payé.");
        }

        Marche marche = factoring.getMarche();
        if (marche == null) {
            throw new ResourceNotFoundException("Marché introuvable pour le factoring " + factoring.getId());
        }

        Banque banque = banqueService.findOrThrow(requestDto.getBanqueId());
        if (marche.getBanque() == null || !marche.getBanque().getId().equals(banque.getId())) {
            throw new ValidationException("Le banqueId fourni (" + requestDto.getBanqueId()
                    + ") ne correspond pas à la banque du marché associé au factoring "
                    + factoring.getId() + ".");
        }

        // Règles de saisie de la TVA (taux choisi XOR montant saisi, bornes) : logique métier,
        // donc portée par le service et non par le mapper (conventions/architecture.md).
        BigDecimal montantTva = TvaResolver.resoudre(
                factoring.getMontant(), requestDto.getTauxTva(), requestDto.getMontantTva());

        Transaction transactionEntity = factoringTransactionMapper.toEntity(requestDto, factoring, banque, montantTva);
        Transaction savedTransaction = saveTransactionWithCascade(
                transactionEntity, banque.getId(), requestDto.getRapprochementId());

        factoring.setStatus(Status.PAYE);
        factoring.setDateOperation(savedTransaction.getDateOperations());
        factoring.setDateValeur(savedTransaction.getDateValeur());
        factoring.setObservation(savedTransaction.getObservation());
        factoring.setLibelle(savedTransaction.getLibelle());
        factoringRepository.save(factoring);

        log.info("Factoring id: {} validé — transaction créée id: {}", factoring.getId(), savedTransaction.getId());
        return factoringTransactionMapper.toResponseDto(savedTransaction);
    }

    @Override
    @Transactional
    public void syncFactoringFromTransaction(Long factoringId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation) {
        log.info("Synchronisation factoring depuis transaction — id: {}", factoringId);

        Factoring factoring = factoringRepository.findById(factoringId)
                .orElseThrow(() -> {
                    log.warn("Factoring introuvable pour synchronisation — id: {}", factoringId);
                    return new ResourceNotFoundException("Factoring introuvable avec l'ID: " + factoringId);
                });

        factoring.setDateValeur(dateValeur);
        factoring.setDateOperation(dateOperation);
        factoring.setLibelle(libelle);
        factoring.setObservation(observation);
        factoringRepository.save(factoring);

        log.info("Factoring id: {} synchronisé depuis transaction", factoringId);
    }

    // -------------------------------------------------------------------------
    // Méthodes privées
    // -------------------------------------------------------------------------

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
