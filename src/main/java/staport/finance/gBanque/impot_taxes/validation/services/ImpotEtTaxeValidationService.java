package staport.finance.gBanque.impot_taxes.validation.services;

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
import staport.finance.gBanque.impot_taxes.entities.ImpotEtTaxe;
import staport.finance.gBanque.impot_taxes.repositories.ImpotEtTaxeRepository;
import staport.finance.gBanque.impot_taxes.validation.dto.ImpotEtTaxeTransactionRequestDto;
import staport.finance.gBanque.impot_taxes.validation.dto.ImpotEtTaxeTransactionResponseDto;
import staport.finance.gBanque.impot_taxes.validation.mapper.ImpotEtTaxeTransactionMapper;
import staport.finance.gBanque.impot_taxes.validation.services.interfaces.IImpotEtTaxeValidationService;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.rapprochement.entities.Rapprochement;
import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
import staport.finance.gBanque.rapprochement.repository.RapprochementRepository;
import staport.finance.gBanque.rapprochement.services.SoldeCascadeService;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.repositories.TransactionRepository;

import java.time.LocalDate;

/**
 * Implémentation du service de validation des impôts/taxes.
 *
 * <p>Coordonne la création de la transaction bancaire, l'attachement au rapprochement
 * et la mise à jour de l'impôt/taxe source. Extrait du {@code ValidationService}
 * générique pour respecter le bounded context du module impot_taxes (architecture.md,
 * Règle 1).
 */
@Service
public class ImpotEtTaxeValidationService implements IImpotEtTaxeValidationService {

    private static final Logger log = LoggerFactory.getLogger(ImpotEtTaxeValidationService.class);

    private final ImpotEtTaxeRepository impotEtTaxeRepository;
    private final ImpotEtTaxeTransactionMapper impotEtTaxeTransactionMapper;
    private final TransactionRepository transactionRepository;
    private final RapprochementRepository rapprochementRepository;
    private final SoldeCascadeService soldeCascadeService;

    /**
     * {@code @Lazy} obligatoire — même précaution que {@code PrelevementValidationService}/
     * {@code DecompteValidationService} : sans lui, cycle de bean Spring au démarrage
     * ({@code BanqueServiceImpl} → {@code ITransactionService} →
     * {@code IImpotEtTaxeValidationService} → {@code IBanqueService} → {@code BanqueServiceImpl}).
     */
    private final IBanqueService banqueService;

    public ImpotEtTaxeValidationService(
            ImpotEtTaxeRepository impotEtTaxeRepository,
            ImpotEtTaxeTransactionMapper impotEtTaxeTransactionMapper,
            TransactionRepository transactionRepository,
            RapprochementRepository rapprochementRepository,
            SoldeCascadeService soldeCascadeService,
            @Lazy IBanqueService banqueService) {
        this.impotEtTaxeRepository = impotEtTaxeRepository;
        this.impotEtTaxeTransactionMapper = impotEtTaxeTransactionMapper;
        this.transactionRepository = transactionRepository;
        this.rapprochementRepository = rapprochementRepository;
        this.soldeCascadeService = soldeCascadeService;
        this.banqueService = banqueService;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @AuditableAction(operation = AuditOperation.UPDATE, entity = ImpotEtTaxe.class, idAccessor = "impotEtTaxeId")
    public ImpotEtTaxeTransactionResponseDto processImpotEtTaxeTransaction(ImpotEtTaxeTransactionRequestDto requestDto) {
        log.info("Validation impôt/taxe — id: {}", requestDto.idImpotEtTaxe());

        ImpotEtTaxe impotEtTaxe = impotEtTaxeRepository.findById(requestDto.idImpotEtTaxe())
                .orElseThrow(() -> {
                    log.warn("Impôt/taxe introuvable — id: {}", requestDto.idImpotEtTaxe());
                    return new ResourceNotFoundException("Impôt/taxe introuvable avec l'id: " + requestDto.idImpotEtTaxe());
                });

        if (impotEtTaxe.getStatus() == Status.PAYE) {
            log.warn("Tentative de validation d'un impôt/taxe déjà PAYE — id: {}", impotEtTaxe.getId());
            throw new ValidationException("L'impôt/taxe id: " + impotEtTaxe.getId() + " est déjà payé.");
        }

        // Vérifie que la banque de l'impôt/taxe existe toujours (garde d'intégrité, cf. ancien
        // comportement de ValidationService.processImpotEtTaxeToTransaction).
        banqueService.findOrThrow(impotEtTaxe.getBanque().getId());

        Transaction transactionEntity = impotEtTaxeTransactionMapper.toEntity(requestDto, impotEtTaxe);
        Transaction savedTransaction = saveTransactionWithCascade(
                transactionEntity, impotEtTaxe.getBanque().getId(), requestDto.rapprochementId());

        impotEtTaxe.setStatus(Status.PAYE);
        impotEtTaxe.setObservation(savedTransaction.getObservation());
        impotEtTaxe.setLibelle(savedTransaction.getLibelle());
        impotEtTaxe.setDateValeur(savedTransaction.getDateValeur());
        impotEtTaxe.setDateOperation(savedTransaction.getDateOperations());
        impotEtTaxeRepository.save(impotEtTaxe);

        log.info("Impôt/taxe id: {} validé — transaction créée id: {}", impotEtTaxe.getId(), savedTransaction.getId());
        return impotEtTaxeTransactionMapper.toResponseDto(savedTransaction);
    }

    @Override
    @Transactional
    public void syncImpotEtTaxeFromTransaction(Long impotEtTaxeId, LocalDate dateValeur, LocalDate dateOperation) {
        log.info("Synchronisation impôt/taxe depuis transaction — id: {}", impotEtTaxeId);

        ImpotEtTaxe impotEtTaxe = impotEtTaxeRepository.findById(impotEtTaxeId)
                .orElseThrow(() -> {
                    log.warn("Impôt/taxe introuvable pour synchronisation — id: {}", impotEtTaxeId);
                    return new ResourceNotFoundException("Impôt/taxe introuvable avec l'ID: " + impotEtTaxeId);
                });

        impotEtTaxe.setDateValeur(dateValeur);
        impotEtTaxe.setDateOperation(dateOperation);
        impotEtTaxeRepository.save(impotEtTaxe);

        log.info("Impôt/taxe id: {} synchronisé depuis transaction", impotEtTaxeId);
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
