package staport.finance.gBanque.moyensDePaiements.virementInternational.validation.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.moyensDePaiements.virementInternational.entities.VirementInternational;
import staport.finance.gBanque.moyensDePaiements.virementInternational.repositories.VirementInternationalRepository;
import staport.finance.gBanque.moyensDePaiements.virementInternational.validation.dto.VirementInternationalTransactionRequestDto;
import staport.finance.gBanque.moyensDePaiements.virementInternational.validation.dto.VirementInternationalTransactionResponseDto;
import staport.finance.gBanque.moyensDePaiements.virementInternational.validation.mapper.VirementInternationalTransactionMapper;
import staport.finance.gBanque.moyensDePaiements.virementInternational.validation.services.interfaces.IVirementInternationalValidationService;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.rapprochement.entities.Rapprochement;
import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
import staport.finance.gBanque.rapprochement.repository.RapprochementRepository;
import staport.finance.gBanque.rapprochement.services.SoldeCascadeService;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.repositories.TransactionRepository;

import java.time.LocalDate;

/**
 * Implémentation du service de validation des virements internationaux.
 *
 * <p>Coordonne la création de la transaction bancaire, l'attachement au rapprochement
 * et la mise à jour du virement international source. Extrait du {@code ValidationService}
 * générique pour respecter le bounded context du module virementInternational (architecture.md,
 * Règle 1).
 */
@Service
public class VirementInternationalValidationService implements IVirementInternationalValidationService {

    private static final Logger log = LoggerFactory.getLogger(VirementInternationalValidationService.class);

    private final VirementInternationalRepository virementInternationalRepository;
    private final VirementInternationalTransactionMapper virementInternationalTransactionMapper;
    private final TransactionRepository transactionRepository;
    private final RapprochementRepository rapprochementRepository;
    private final SoldeCascadeService soldeCascadeService;

    public VirementInternationalValidationService(
            VirementInternationalRepository virementInternationalRepository,
            VirementInternationalTransactionMapper virementInternationalTransactionMapper,
            TransactionRepository transactionRepository,
            RapprochementRepository rapprochementRepository,
            SoldeCascadeService soldeCascadeService) {
        this.virementInternationalRepository = virementInternationalRepository;
        this.virementInternationalTransactionMapper = virementInternationalTransactionMapper;
        this.transactionRepository = transactionRepository;
        this.rapprochementRepository = rapprochementRepository;
        this.soldeCascadeService = soldeCascadeService;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @AuditableAction(operation = AuditOperation.UPDATE, entity = VirementInternational.class, idAccessor = "getVirementInternationalId")
    public VirementInternationalTransactionResponseDto processVirementInternationalDecaissementTransaction(VirementInternationalTransactionRequestDto requestDto) {
        log.info("Validation virement international décaissement — id: {}", requestDto.getId());
        return processTransaction(requestDto);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @AuditableAction(operation = AuditOperation.UPDATE, entity = VirementInternational.class, idAccessor = "getVirementInternationalId")
    public VirementInternationalTransactionResponseDto processVirementInternationalEncaissementTransaction(VirementInternationalTransactionRequestDto requestDto) {
        log.info("Validation virement international encaissement — id: {}", requestDto.getId());
        return processTransaction(requestDto);
    }

    private VirementInternationalTransactionResponseDto processTransaction(VirementInternationalTransactionRequestDto requestDto) {
        VirementInternational virementInternational = virementInternationalRepository.findById(requestDto.getId())
                .orElseThrow(() -> {
                    log.warn("Virement international introuvable — id: {}", requestDto.getId());
                    return new ResourceNotFoundException("Virement international with id " + requestDto.getId() + " not found");
                });

        if (virementInternational.getStatut() != StatutMoyenPaiement.ENCOURS) {
            log.warn("Tentative de validation d'un virement international non ENCOURS — id: {}, statut: {}",
                    virementInternational.getId(), virementInternational.getStatut());
            throw new ValidationException("Le virement international id: " + virementInternational.getId() + " n'est pas ENCOURS.");
        }

        Transaction transactionEntity = virementInternationalTransactionMapper.toEntity(requestDto, virementInternational);
        Transaction savedTransaction = saveTransactionWithCascade(
                transactionEntity, virementInternational.getBanque().getId(), requestDto.getRapprochementId());

        virementInternational.setStatut(StatutMoyenPaiement.REGLE);
        virementInternational.setDateOperation(savedTransaction.getDateOperations());
        virementInternational.setDateValeur(savedTransaction.getDateValeur());
        virementInternational.setLibelle(savedTransaction.getLibelle());
        virementInternational.setObservation(savedTransaction.getObservation());
        virementInternationalRepository.save(virementInternational);

        log.info("Virement international id: {} validé — transaction créée id: {}", virementInternational.getId(), savedTransaction.getId());
        return virementInternationalTransactionMapper.toResponseDto(savedTransaction);
    }

    @Override
    @Transactional
    public void syncVirementInternationalFromTransaction(Long virementInternationalId, LocalDate dateValeur, LocalDate dateOperation) {
        log.info("Synchronisation virement international depuis transaction — id: {}", virementInternationalId);

        VirementInternational virementInternational = virementInternationalRepository.findById(virementInternationalId)
                .orElseThrow(() -> {
                    log.warn("Virement international introuvable pour synchronisation — id: {}", virementInternationalId);
                    return new ResourceNotFoundException("Virement international introuvable avec l'ID: " + virementInternationalId);
                });

        virementInternational.setDateValeur(dateValeur);
        virementInternational.setDateOperation(dateOperation);
        virementInternationalRepository.save(virementInternational);

        log.info("Virement international id: {} synchronisé depuis transaction", virementInternationalId);
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
