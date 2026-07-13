package staport.finance.gBanque.decomptes.validation.services;

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
import staport.finance.gBanque.decomptes.entities.Decompte;
import staport.finance.gBanque.decomptes.repositories.DecompteRepository;
import staport.finance.gBanque.decomptes.validation.dto.DecompteTransactionRequestDto;
import staport.finance.gBanque.decomptes.validation.dto.DecompteTransactionResponseDto;
import staport.finance.gBanque.decomptes.validation.mapper.DecompteTransactionMapper;
import staport.finance.gBanque.decomptes.validation.services.interfaces.IDecompteValidationService;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.rapprochement.entities.Rapprochement;
import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
import staport.finance.gBanque.rapprochement.repository.RapprochementRepository;
import staport.finance.gBanque.rapprochement.services.SoldeCascadeService;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.repositories.TransactionRepository;

import java.time.LocalDate;

/**
 * Implémentation du service de validation des décomptes.
 *
 * <p>Coordonne la création de la transaction bancaire, l'attachement au rapprochement
 * et la mise à jour du décompte source. Extrait du {@code ValidationService}
 * générique pour respecter le bounded context du module decomptes (architecture.md,
 * Règle 1).
 */
@Service
public class DecompteValidationService implements IDecompteValidationService {

    private static final Logger log = LoggerFactory.getLogger(DecompteValidationService.class);

    private final DecompteRepository decompteRepository;
    private final DecompteTransactionMapper decompteTransactionMapper;
    private final TransactionRepository transactionRepository;
    private final RapprochementRepository rapprochementRepository;
    private final SoldeCascadeService soldeCascadeService;

    /**
     * {@code @Lazy} obligatoire — même précaution que {@code PrelevementValidationService} (cf.
     * TODO.md, correctif du 12/07/2026) : sans lui, cycle de bean Spring au démarrage
     * ({@code BanqueServiceImpl} → {@code ITransactionService} → {@code IDecompteValidationService}
     * → {@code IBanqueService} → {@code BanqueServiceImpl}).
     */
    private final IBanqueService banqueService;

    public DecompteValidationService(
            DecompteRepository decompteRepository,
            DecompteTransactionMapper decompteTransactionMapper,
            TransactionRepository transactionRepository,
            RapprochementRepository rapprochementRepository,
            SoldeCascadeService soldeCascadeService,
            @Lazy IBanqueService banqueService) {
        this.decompteRepository = decompteRepository;
        this.decompteTransactionMapper = decompteTransactionMapper;
        this.transactionRepository = transactionRepository;
        this.rapprochementRepository = rapprochementRepository;
        this.soldeCascadeService = soldeCascadeService;
        this.banqueService = banqueService;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Decompte.class, idAccessor = "getDecompteId")
    public DecompteTransactionResponseDto processDecompteTransaction(DecompteTransactionRequestDto requestDto) {
        log.info("Validation décompte — id: {}", requestDto.getDecompteId());

        Decompte decompte = decompteRepository.findById(requestDto.getDecompteId())
                .orElseThrow(() -> {
                    log.warn("Décompte introuvable — id: {}", requestDto.getDecompteId());
                    return new ResourceNotFoundException("Décompte with id " + requestDto.getDecompteId() + " not found");
                });

        if (decompte.getStatus() == Status.PAYE) {
            log.warn("Tentative de validation d'un décompte déjà PAYE — id: {}", decompte.getId());
            throw new ValidationException("Le décompte id: " + decompte.getId() + " est déjà payé.");
        }

        Banque banque = banqueService.findOrThrow(requestDto.getBanqueId());
        if (decompte.getBanqueEncaissement() == null || !decompte.getBanqueEncaissement().getId().equals(banque.getId())) {
            throw new ValidationException("Le banqueId fourni (" + requestDto.getBanqueId()
                    + ") ne correspond pas à la banque d'encaissement du décompte "
                    + decompte.getId() + ".");
        }

        Transaction transactionEntity = decompteTransactionMapper.toEntity(requestDto, decompte, banque);
        Transaction savedTransaction = saveTransactionWithCascade(
                transactionEntity, banque.getId(), requestDto.getRapprochementId());

        decompte.setStatus(Status.PAYE);
        decompte.setDateOperation(savedTransaction.getDateOperations());
        decompte.setDateValeur(savedTransaction.getDateValeur());
        decompte.setObservation(savedTransaction.getObservation());
        decompte.setLibelle(savedTransaction.getLibelle());
        decompteRepository.save(decompte);

        log.info("Décompte id: {} validé — transaction créée id: {}", decompte.getId(), savedTransaction.getId());
        return decompteTransactionMapper.toResponseDto(savedTransaction);
    }

    @Override
    @Transactional
    public void syncDecompteFromTransaction(Long decompteId, LocalDate dateValeur, LocalDate dateOperation) {
        log.info("Synchronisation décompte depuis transaction — id: {}", decompteId);

        Decompte decompte = decompteRepository.findById(decompteId)
                .orElseThrow(() -> {
                    log.warn("Décompte introuvable pour synchronisation — id: {}", decompteId);
                    return new ResourceNotFoundException("Décompte introuvable avec l'ID: " + decompteId);
                });

        decompte.setDateValeur(dateValeur);
        decompte.setDateOperation(dateOperation);
        decompteRepository.save(decompte);

        log.info("Décompte id: {} synchronisé depuis transaction", decompteId);
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
