package staport.finance.gBanque.commision.validation.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.commision.entities.Commission;
import staport.finance.gBanque.commision.repository.CommisionRepository;
import staport.finance.gBanque.commision.validation.dto.CommissionTransactionRequestDto;
import staport.finance.gBanque.commision.validation.dto.CommissionTransactionResponseDto;
import staport.finance.gBanque.commision.validation.mapper.CommissionTransactionMapper;
import staport.finance.gBanque.commision.validation.services.interfaces.ICommissionValidationService;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
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
 * Implémentation du service de validation des commissions.
 *
 * <p>Coordonne la création de la transaction bancaire, l'attachement au rapprochement
 * et la mise à jour de la commission source. Extrait du {@code ValidationService}
 * générique pour respecter le bounded context du module commision (architecture.md,
 * Règle 1).
 */
@Service
public class CommissionValidationService implements ICommissionValidationService {

    private static final Logger log = LoggerFactory.getLogger(CommissionValidationService.class);

    private final CommisionRepository commisionRepository;
    private final CommissionTransactionMapper commissionTransactionMapper;
    private final TransactionRepository transactionRepository;
    private final RapprochementRepository rapprochementRepository;
    private final SoldeCascadeService soldeCascadeService;

    /**
     * {@code @Lazy} obligatoire — même précaution que {@code PrelevementValidationService}/
     * {@code ImpotEtTaxeValidationService} : sans lui, cycle de bean Spring au démarrage
     * ({@code BanqueServiceImpl} → {@code ITransactionService} →
     * {@code ICommissionValidationService} → {@code IBanqueService} → {@code BanqueServiceImpl}).
     */
    private final IBanqueService banqueService;

    public CommissionValidationService(
            CommisionRepository commisionRepository,
            CommissionTransactionMapper commissionTransactionMapper,
            TransactionRepository transactionRepository,
            RapprochementRepository rapprochementRepository,
            SoldeCascadeService soldeCascadeService,
            @Lazy IBanqueService banqueService) {
        this.commisionRepository = commisionRepository;
        this.commissionTransactionMapper = commissionTransactionMapper;
        this.transactionRepository = transactionRepository;
        this.rapprochementRepository = rapprochementRepository;
        this.soldeCascadeService = soldeCascadeService;
        this.banqueService = banqueService;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Commission.class, idAccessor = "commissionId")
    public CommissionTransactionResponseDto processCommissionTransaction(CommissionTransactionRequestDto requestDto) {
        log.info("Validation commission — id: {}", requestDto.idCommission());

        Commission commission = commisionRepository.findById(requestDto.idCommission())
                .orElseThrow(() -> {
                    log.warn("Commission introuvable — id: {}", requestDto.idCommission());
                    return new ResourceNotFoundException("Commission introuvable avec l'id: " + requestDto.idCommission());
                });

        if (commission.getStatus() == Status.PAYE) {
            log.warn("Tentative de validation d'une commission déjà PAYE — id: {}", commission.getId());
            throw new ValidationException("La commission id: " + commission.getId() + " est déjà payée.");
        }

        // Vérifie que la banque de la commission existe toujours (garde d'intégrité, cf. ancien
        // comportement de ValidationService.processCommissionToTransaction).
        banqueService.findOrThrow(commission.getBanque().getId());

        // Règles de saisie de la TVA (taux choisi XOR montant saisi, bornes) : logique métier,
        // donc portée par le service et non par le mapper (conventions/architecture.md).
        BigDecimal montantTva = TvaResolver.resoudre(
                commission.getMontant(), requestDto.tauxTva(), requestDto.montantTva());

        Transaction transactionEntity = commissionTransactionMapper.toEntity(requestDto, commission, montantTva);
        Transaction savedTransaction = saveTransactionWithCascade(
                transactionEntity, commission.getBanque().getId(), requestDto.rapprochementId());

        commission.setStatus(Status.PAYE);
        commission.setObservation(savedTransaction.getObservation());
        commission.setLibelle(savedTransaction.getLibelle());
        commission.setDateValeur(savedTransaction.getDateValeur());
        commission.setDateOperation(savedTransaction.getDateOperations());
        commisionRepository.save(commission);

        log.info("Commission id: {} validée — transaction créée id: {}", commission.getId(), savedTransaction.getId());
        return commissionTransactionMapper.toResponseDto(savedTransaction);
    }

    @Override
    @Transactional
    public void syncCommissionFromTransaction(Long commissionId, LocalDate dateValeur, LocalDate dateOperation) {
        log.info("Synchronisation commission depuis transaction — id: {}", commissionId);

        Commission commission = commisionRepository.findById(commissionId)
                .orElseThrow(() -> {
                    log.warn("Commission introuvable pour synchronisation — id: {}", commissionId);
                    return new ResourceNotFoundException("Commission introuvable avec l'ID: " + commissionId);
                });

        commission.setDateValeur(dateValeur);
        commission.setDateOperation(dateOperation);
        commisionRepository.save(commission);

        log.info("Commission id: {} synchronisée depuis transaction", commissionId);
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
