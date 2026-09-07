package staport.finance.gBanque.operationErrone.validation.services;

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
import staport.finance.gBanque.operationErrone.entities.OperationErrone;
import staport.finance.gBanque.operationErrone.repositories.OperationErroneRepository;
import staport.finance.gBanque.operationErrone.validation.dto.OperationErroneTransactionRequestDto;
import staport.finance.gBanque.operationErrone.validation.dto.OperationErroneTransactionResponseDto;
import staport.finance.gBanque.operationErrone.validation.mapper.OperationErroneTransactionMapper;
import staport.finance.gBanque.operationErrone.validation.services.interfaces.IOperationErroneValidationService;
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
 * Implémentation du service de validation des opérations erronées.
 *
 * <p>Extrait du {@code ValidationService} générique pour respecter le bounded context du module
 * operationErrone (architecture.md, Règle 1).
 */
@Service
public class OperationErroneValidationService implements IOperationErroneValidationService {

    private static final Logger log = LoggerFactory.getLogger(OperationErroneValidationService.class);

    private final OperationErroneRepository operationErroneRepository;
    private final OperationErroneTransactionMapper operationErroneTransactionMapper;
    private final TransactionRepository transactionRepository;
    private final RapprochementRepository rapprochementRepository;
    private final SoldeCascadeService soldeCascadeService;

    /**
     * {@code @Lazy} obligatoire — même précaution que les autres {@code XxxValidationService}.
     */
    private final IBanqueService banqueService;

    public OperationErroneValidationService(
            OperationErroneRepository operationErroneRepository,
            OperationErroneTransactionMapper operationErroneTransactionMapper,
            TransactionRepository transactionRepository,
            RapprochementRepository rapprochementRepository,
            SoldeCascadeService soldeCascadeService,
            @Lazy IBanqueService banqueService) {
        this.operationErroneRepository = operationErroneRepository;
        this.operationErroneTransactionMapper = operationErroneTransactionMapper;
        this.transactionRepository = transactionRepository;
        this.rapprochementRepository = rapprochementRepository;
        this.soldeCascadeService = soldeCascadeService;
        this.banqueService = banqueService;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @AuditableAction(operation = AuditOperation.UPDATE, entity = OperationErrone.class, idAccessor = "operationErroneId")
    public OperationErroneTransactionResponseDto processOperationErroneTransaction(OperationErroneTransactionRequestDto requestDto) {
        log.info("Validation opération erronée — id: {}", requestDto.idOperationErrone());

        OperationErrone operationErrone = operationErroneRepository.findById(requestDto.idOperationErrone())
                .orElseThrow(() -> {
                    log.warn("Opération erronée introuvable — id: {}", requestDto.idOperationErrone());
                    return new ResourceNotFoundException("Opération erronée introuvable avec l'ID: " + requestDto.idOperationErrone());
                });

        if (operationErrone.getStatus() == Status.PAYE) {
            log.warn("Tentative de validation d'une opération erronée déjà PAYE — id: {}", operationErrone.getId());
            throw new ValidationException("L'opération erronée id: " + operationErrone.getId() + " est déjà payée.");
        }

        banqueService.findOrThrow(operationErrone.getBanque().getId());

        // Règles de saisie de la TVA (taux choisi XOR montant saisi, bornes) : logique métier,
        // donc portée par le service et non par le mapper (conventions/architecture.md).
        BigDecimal montantTva = TvaResolver.resoudre(
                operationErrone.getMontant(), requestDto.tauxTva(), requestDto.montantTva());

        Transaction transactionEntity = operationErroneTransactionMapper.toEntity(requestDto, operationErrone, montantTva);
        Transaction savedTransaction = saveTransactionWithCascade(
                transactionEntity, operationErrone.getBanque().getId(), requestDto.rapprochementId());

        operationErrone.setStatus(Status.PAYE);
        operationErrone.setDateOperation(savedTransaction.getDateOperations());
        operationErrone.setDateValeur(savedTransaction.getDateValeur());
        operationErrone.setObservation(savedTransaction.getObservation());
        operationErrone.setLibelle(savedTransaction.getLibelle());
        operationErroneRepository.save(operationErrone);

        log.info("Opération erronée id: {} validée — transaction créée id: {}", operationErrone.getId(), savedTransaction.getId());
        return operationErroneTransactionMapper.toResponseDto(savedTransaction);
    }

    @Override
    @Transactional
    public void syncOperationErroneFromTransaction(Long operationErroneId, LocalDate dateValeur, LocalDate dateOperation) {
        log.info("Synchronisation opération erronée depuis transaction — id: {}", operationErroneId);

        OperationErrone operationErrone = operationErroneRepository.findById(operationErroneId)
                .orElseThrow(() -> {
                    log.warn("Opération erronée introuvable pour synchronisation — id: {}", operationErroneId);
                    return new ResourceNotFoundException("Opération erronée introuvable avec l'ID: " + operationErroneId);
                });

        operationErrone.setDateValeur(dateValeur);
        operationErrone.setDateOperation(dateOperation);
        operationErroneRepository.save(operationErrone);

        log.info("Opération erronée id: {} synchronisée depuis transaction", operationErroneId);
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
