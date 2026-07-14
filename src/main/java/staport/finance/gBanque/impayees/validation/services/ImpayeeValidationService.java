package staport.finance.gBanque.impayees.validation.services;

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
import staport.finance.gBanque.impayees.entities.Impayee;
import staport.finance.gBanque.impayees.repository.ImpayeeRepository;
import staport.finance.gBanque.impayees.validation.dto.ImpayeeTransactionRequestDto;
import staport.finance.gBanque.impayees.validation.dto.ImpayeeTransactionResponseDto;
import staport.finance.gBanque.impayees.validation.mapper.ImpayeeTransactionMapper;
import staport.finance.gBanque.impayees.validation.services.interfaces.IImpayeeValidationService;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.rapprochement.entities.Rapprochement;
import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
import staport.finance.gBanque.rapprochement.repository.RapprochementRepository;
import staport.finance.gBanque.rapprochement.services.SoldeCascadeService;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.repositories.TransactionRepository;

import java.time.LocalDate;

/**
 * Implémentation du service de validation des impayées.
 *
 * <p>Extrait du {@code ValidationService} générique pour respecter le bounded context du module
 * impayees (architecture.md, Règle 1).
 */
@Service
public class ImpayeeValidationService implements IImpayeeValidationService {

    private static final Logger log = LoggerFactory.getLogger(ImpayeeValidationService.class);

    private final ImpayeeRepository impayeeRepository;
    private final ImpayeeTransactionMapper impayeeTransactionMapper;
    private final TransactionRepository transactionRepository;
    private final RapprochementRepository rapprochementRepository;
    private final SoldeCascadeService soldeCascadeService;

    /**
     * {@code @Lazy} obligatoire — même précaution que les autres {@code XxxValidationService}.
     */
    private final IBanqueService banqueService;

    public ImpayeeValidationService(
            ImpayeeRepository impayeeRepository,
            ImpayeeTransactionMapper impayeeTransactionMapper,
            TransactionRepository transactionRepository,
            RapprochementRepository rapprochementRepository,
            SoldeCascadeService soldeCascadeService,
            @Lazy IBanqueService banqueService) {
        this.impayeeRepository = impayeeRepository;
        this.impayeeTransactionMapper = impayeeTransactionMapper;
        this.transactionRepository = transactionRepository;
        this.rapprochementRepository = rapprochementRepository;
        this.soldeCascadeService = soldeCascadeService;
        this.banqueService = banqueService;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Impayee.class, idAccessor = "impayeeId")
    public ImpayeeTransactionResponseDto processImpayeeTransaction(ImpayeeTransactionRequestDto requestDto) {
        log.info("Validation impayée — id: {}", requestDto.idImpayee());

        Impayee impayee = impayeeRepository.findById(requestDto.idImpayee())
                .orElseThrow(() -> {
                    log.warn("Impayée introuvable — id: {}", requestDto.idImpayee());
                    return new ResourceNotFoundException("Impayée introuvable avec l'ID: " + requestDto.idImpayee());
                });

        if (impayee.getStatus() == Status.PAYE) {
            log.warn("Tentative de validation d'une impayée déjà PAYE — id: {}", impayee.getId());
            throw new ValidationException("L'impayée id: " + impayee.getId() + " est déjà payée.");
        }

        banqueService.findOrThrow(impayee.getBanque().getId());

        Transaction transactionEntity = impayeeTransactionMapper.toEntity(requestDto, impayee);
        Transaction savedTransaction = saveTransactionWithCascade(
                transactionEntity, impayee.getBanque().getId(), requestDto.rapprochementId());

        impayee.setStatus(Status.PAYE);
        impayee.setDateOperation(savedTransaction.getDateOperations());
        impayee.setDateValeur(savedTransaction.getDateValeur());
        impayee.setObservation(savedTransaction.getObservation());
        impayee.setLibelle(savedTransaction.getLibelle());
        impayeeRepository.save(impayee);

        log.info("Impayée id: {} validée — transaction créée id: {}", impayee.getId(), savedTransaction.getId());
        return impayeeTransactionMapper.toResponseDto(savedTransaction);
    }

    @Override
    @Transactional
    public void syncImpayeeFromTransaction(Long impayeeId, LocalDate dateValeur, LocalDate dateOperation) {
        log.info("Synchronisation impayée depuis transaction — id: {}", impayeeId);

        Impayee impayee = impayeeRepository.findById(impayeeId)
                .orElseThrow(() -> {
                    log.warn("Impayée introuvable pour synchronisation — id: {}", impayeeId);
                    return new ResourceNotFoundException("Impayée introuvable avec l'ID: " + impayeeId);
                });

        impayee.setDateValeur(dateValeur);
        impayee.setDateOperation(dateOperation);
        impayeeRepository.save(impayee);

        log.info("Impayée id: {} synchronisée depuis transaction", impayeeId);
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
