package staport.finance.gBanque.provision.validation.services;

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
import staport.finance.gBanque.provision.entities.Provision;
import staport.finance.gBanque.provision.repositories.ProvisionRepository;
import staport.finance.gBanque.provision.validation.dto.ProvisionTransactionRequestDto;
import staport.finance.gBanque.provision.validation.dto.ProvisionTransactionResponseDto;
import staport.finance.gBanque.provision.validation.mapper.ProvisionTransactionMapper;
import staport.finance.gBanque.provision.validation.services.interfaces.IProvisionValidationService;
import staport.finance.gBanque.rapprochement.entities.Rapprochement;
import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
import staport.finance.gBanque.rapprochement.repository.RapprochementRepository;
import staport.finance.gBanque.rapprochement.services.SoldeCascadeService;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.repositories.TransactionRepository;

import java.time.LocalDate;

/**
 * Implémentation du service de validation des provisions.
 *
 * <p>Extrait du {@code ValidationService} générique pour respecter le bounded context du module
 * provision (architecture.md, Règle 1) — calquée sur {@code FraisBancaireValidationService}/
 * {@code OperationErroneValidationService}.
 */
@Service
public class ProvisionValidationService implements IProvisionValidationService {

    private static final Logger log = LoggerFactory.getLogger(ProvisionValidationService.class);

    private final ProvisionRepository provisionRepository;
    private final ProvisionTransactionMapper provisionTransactionMapper;
    private final TransactionRepository transactionRepository;
    private final RapprochementRepository rapprochementRepository;
    private final SoldeCascadeService soldeCascadeService;

    /**
     * {@code @Lazy} obligatoire — même précaution que les autres {@code XxxValidationService} :
     * casse le cycle de bean Spring {@code BanqueServiceImpl} → {@code ITransactionService} →
     * {@code IProvisionValidationService} → {@code IBanqueService} → {@code BanqueServiceImpl}.
     */
    private final IBanqueService banqueService;

    public ProvisionValidationService(
            ProvisionRepository provisionRepository,
            ProvisionTransactionMapper provisionTransactionMapper,
            TransactionRepository transactionRepository,
            RapprochementRepository rapprochementRepository,
            SoldeCascadeService soldeCascadeService,
            @Lazy IBanqueService banqueService) {
        this.provisionRepository = provisionRepository;
        this.provisionTransactionMapper = provisionTransactionMapper;
        this.transactionRepository = transactionRepository;
        this.rapprochementRepository = rapprochementRepository;
        this.soldeCascadeService = soldeCascadeService;
        this.banqueService = banqueService;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Provision.class, idAccessor = "provisionId")
    public ProvisionTransactionResponseDto processProvisionTransaction(ProvisionTransactionRequestDto requestDto) {
        log.info("Validation provision — id: {}", requestDto.idProvision());

        Provision provision = provisionRepository.findById(requestDto.idProvision())
                .orElseThrow(() -> {
                    log.warn("Provision introuvable — id: {}", requestDto.idProvision());
                    return new ResourceNotFoundException("Provision introuvable avec l'ID: " + requestDto.idProvision());
                });

        if (provision.getStatus() == Status.PAYE) {
            log.warn("Tentative de validation d'une provision déjà PAYE — id: {}", provision.getId());
            throw new ValidationException("La provision id: " + provision.getId() + " est déjà payée.");
        }

        // Résolution via IBanqueService (encapsulation des domaines) plutôt que via
        // provision.getBanque() directement — cohérent avec le reste des XxxValidationService.
        banqueService.findOrThrow(provision.getBanque().getId());

        Transaction transactionEntity = provisionTransactionMapper.toEntity(requestDto, provision);
        Transaction savedTransaction = saveTransactionWithCascade(
                transactionEntity, provision.getBanque().getId(), requestDto.rapprochementId());

        provision.setStatus(Status.PAYE);
        provision.setDateOperation(savedTransaction.getDateOperations());
        provision.setDateValeur(savedTransaction.getDateValeur());
        provision.setObservation(savedTransaction.getObservation());
        provision.setLibelle(savedTransaction.getLibelle());
        provisionRepository.save(provision);

        log.info("Provision id: {} validée — transaction créée id: {}", provision.getId(), savedTransaction.getId());
        return provisionTransactionMapper.toResponseDto(savedTransaction, provision.getSens());
    }

    @Override
    @Transactional
    public void syncProvisionFromTransaction(Long provisionId, LocalDate dateValeur, LocalDate dateOperation) {
        log.info("Synchronisation provision depuis transaction — id: {}", provisionId);

        Provision provision = provisionRepository.findById(provisionId)
                .orElseThrow(() -> {
                    log.warn("Provision introuvable pour synchronisation — id: {}", provisionId);
                    return new ResourceNotFoundException("Provision introuvable avec l'ID: " + provisionId);
                });

        provision.setDateValeur(dateValeur);
        provision.setDateOperation(dateOperation);
        provisionRepository.save(provision);

        log.info("Provision id: {} synchronisée depuis transaction", provisionId);
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
