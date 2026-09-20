package staport.finance.gBanque.restitution.validation.services;

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
import staport.finance.gBanque.rapprochement.entities.Rapprochement;
import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
import staport.finance.gBanque.rapprochement.repository.RapprochementRepository;
import staport.finance.gBanque.rapprochement.services.SoldeCascadeService;
import staport.finance.gBanque.restitution.entities.Restitution;
import staport.finance.gBanque.restitution.repository.RestitutionRepository;
import staport.finance.gBanque.restitution.validation.dto.RestitutionTransactionRequestDto;
import staport.finance.gBanque.restitution.validation.dto.RestitutionTransactionResponseDto;
import staport.finance.gBanque.restitution.validation.mapper.RestitutionTransactionMapper;
import staport.finance.gBanque.restitution.validation.services.interfaces.IRestitutionValidationService;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.repositories.TransactionRepository;
import staport.finance.gBanque.validation.utils.TvaResolver;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Implémentation du service de validation des restitutions.
 *
 * <p>Extrait du {@code ValidationService} générique pour respecter le bounded context du module
 * restitution (architecture.md, Règle 1).
 */
@Service
public class RestitutionValidationService implements IRestitutionValidationService {

    private static final Logger log = LoggerFactory.getLogger(RestitutionValidationService.class);

    private final RestitutionRepository restitutionRepository;
    private final RestitutionTransactionMapper restitutionTransactionMapper;
    private final TransactionRepository transactionRepository;
    private final RapprochementRepository rapprochementRepository;
    private final SoldeCascadeService soldeCascadeService;

    /**
     * {@code @Lazy} obligatoire — même précaution que les autres {@code XxxValidationService}.
     */
    private final IBanqueService banqueService;

    public RestitutionValidationService(
            RestitutionRepository restitutionRepository,
            RestitutionTransactionMapper restitutionTransactionMapper,
            TransactionRepository transactionRepository,
            RapprochementRepository rapprochementRepository,
            SoldeCascadeService soldeCascadeService,
            @Lazy IBanqueService banqueService) {
        this.restitutionRepository = restitutionRepository;
        this.restitutionTransactionMapper = restitutionTransactionMapper;
        this.transactionRepository = transactionRepository;
        this.rapprochementRepository = rapprochementRepository;
        this.soldeCascadeService = soldeCascadeService;
        this.banqueService = banqueService;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Restitution.class, idAccessor = "restitutionId")
    public RestitutionTransactionResponseDto processRestitutionTransaction(RestitutionTransactionRequestDto requestDto) {
        log.info("Validation restitution — id: {}", requestDto.idRestitution());

        Restitution restitution = restitutionRepository.findById(requestDto.idRestitution())
                .orElseThrow(() -> {
                    log.warn("Restitution introuvable — id: {}", requestDto.idRestitution());
                    return new ResourceNotFoundException("Restitution introuvable avec l'ID: " + requestDto.idRestitution());
                });

        if (restitution.getStatus() == Status.PAYE) {
            log.warn("Tentative de validation d'une restitution déjà PAYE — id: {}", restitution.getId());
            throw new ValidationException("La restitution id: " + restitution.getId() + " est déjà payée.");
        }

        banqueService.findOrThrow(restitution.getBanque().getId());

        // Règles de saisie de la TVA (taux choisi XOR montant saisi, bornes) : logique métier,
        // donc portée par le service et non par le mapper (conventions/architecture.md).
        BigDecimal montantTva = TvaResolver.resoudre(
                restitution.getMontant(), requestDto.tauxTva(), requestDto.montantTva());

        Transaction transactionEntity = restitutionTransactionMapper.toEntity(requestDto, restitution, montantTva);
        Transaction savedTransaction = saveTransactionWithCascade(
                transactionEntity, restitution.getBanque().getId(), requestDto.rapprochementId());

        restitution.setStatus(Status.PAYE);
        restitution.setDateOperation(savedTransaction.getDateOperations());
        restitution.setDateValeur(savedTransaction.getDateValeur());
        restitution.setObservation(savedTransaction.getObservation());
        restitution.setLibelle(savedTransaction.getLibelle());
        restitutionRepository.save(restitution);

        log.info("Restitution id: {} validée — transaction créée id: {}", restitution.getId(), savedTransaction.getId());
        return restitutionTransactionMapper.toResponseDto(savedTransaction);
    }

    @Override
    @Transactional
    public void syncRestitutionFromTransaction(Long restitutionId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation) {
        log.info("Synchronisation restitution depuis transaction — id: {}", restitutionId);

        Restitution restitution = restitutionRepository.findById(restitutionId)
                .orElseThrow(() -> {
                    log.warn("Restitution introuvable pour synchronisation — id: {}", restitutionId);
                    return new ResourceNotFoundException("Restitution introuvable avec l'ID: " + restitutionId);
                });

        restitution.setDateValeur(dateValeur);
        restitution.setDateOperation(dateOperation);
        restitution.setLibelle(libelle);
        restitution.setObservation(observation);
        restitutionRepository.save(restitution);

        log.info("Restitution id: {} synchronisée depuis transaction", restitutionId);
    }

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
