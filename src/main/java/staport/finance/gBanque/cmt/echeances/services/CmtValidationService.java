package staport.finance.gBanque.cmt.echeances.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.cmt.echeances.dtos.CmtTransactionRequestDto;
import staport.finance.gBanque.cmt.echeances.dtos.CmtTransactionResponseDto;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;
import staport.finance.gBanque.cmt.echeances.enums.StatutEcheanceCMT;
import staport.finance.gBanque.cmt.echeances.mappers.CmtTransactionMapper;
import staport.finance.gBanque.cmt.echeances.repositories.EcheanceCMTRepository;
import staport.finance.gBanque.cmt.echeances.services.interfaces.ICmtValidationService;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.rapprochement.entities.Rapprochement;
import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
import staport.finance.gBanque.rapprochement.repository.RapprochementRepository;
import staport.finance.gBanque.rapprochement.services.SoldeCascadeService;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.repositories.TransactionRepository;

import java.time.LocalDate;

/**
 * Implémentation du service de validation des échéances CMT.
 * <p>
 * Coordonne la création de la transaction bancaire, l'attachement au rapprochement
 * et la mise à jour de l'échéance source. Extrait du {@code ValidationService}
 * générique pour respecter le bounded context du module cmt (architecture.md, Règle 1).
 */
@Service
public class CmtValidationService implements ICmtValidationService {

    private static final Logger log = LoggerFactory.getLogger(CmtValidationService.class);

    private final EcheanceCMTRepository echeanceCMTRepository;
    private final CmtTransactionMapper cmtTransactionMapper;
    private final TransactionRepository transactionRepository;
    private final RapprochementRepository rapprochementRepository;
    private final SoldeCascadeService soldeCascadeService;

    public CmtValidationService(
            EcheanceCMTRepository echeanceCMTRepository,
            CmtTransactionMapper cmtTransactionMapper,
            TransactionRepository transactionRepository,
            RapprochementRepository rapprochementRepository,
            SoldeCascadeService soldeCascadeService) {
        this.echeanceCMTRepository = echeanceCMTRepository;
        this.cmtTransactionMapper = cmtTransactionMapper;
        this.transactionRepository = transactionRepository;
        this.rapprochementRepository = rapprochementRepository;
        this.soldeCascadeService = soldeCascadeService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @AuditableAction(operation = AuditOperation.UPDATE, entity = EcheanceCMT.class, idAccessor = "echeanceId")
    public CmtTransactionResponseDto processCmtTransaction(CreditMoyenTerme creditMoyenTerme, CmtTransactionRequestDto dto) {
        log.info("Validation échéance CMT — creditId: {}, écheanceId: {}", dto.creditMoyenTermeId(), dto.echeanceId());

        EcheanceCMT echeanceCMT = echeanceCMTRepository
                .findByIdAndCreditMoyenTermeId(dto.echeanceId(), dto.creditMoyenTermeId())
                .orElseThrow(() -> {
                    log.warn("Échéance introuvable — ID: {}, creditId: {}", dto.echeanceId(), dto.creditMoyenTermeId());
                    return new ResourceNotFoundException(
                            "Echeance cmt introuvable avec l'ID: " + dto.echeanceId()
                            + " et credit id " + dto.creditMoyenTermeId());
                });

        if (echeanceCMT.getStatutPaiement() == StatutEcheanceCMT.PAYE) {
            log.warn("Tentative de validation d'une échéance déjà payée — ID: {}", dto.echeanceId());
            throw new ValidationException("L'échéance ID " + dto.echeanceId() + " est déjà payée.");
        }

        Transaction transactionEntity = cmtTransactionMapper.toEntity(dto, creditMoyenTerme, echeanceCMT);
        Transaction savedTransaction = saveTransactionWithCascade(
                transactionEntity,
                creditMoyenTerme.getBanque().getId(),
                dto.rapprochementId());

        echeanceCMT.setStatutPaiement(StatutEcheanceCMT.PAYE);
        echeanceCMT.setDateValeur(savedTransaction.getDateValeur());
        echeanceCMT.setDateOperation(savedTransaction.getDateOperations());
        echeanceCMT.setLibelle(savedTransaction.getLibelle());
        echeanceCMT.setObservation(savedTransaction.getObservation());
        echeanceCMTRepository.save(echeanceCMT);

        log.info("Échéance ID: {} validée — transaction créée ID: {}", dto.echeanceId(), savedTransaction.getId());
        return cmtTransactionMapper.toResponseDto(savedTransaction);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = EcheanceCMT.class)
    public void syncEcheanceFromTransaction(Long echeanceId, LocalDate dateValeur, LocalDate dateOperation) {
        log.info("Synchronisation échéance CMT depuis transaction — écheanceId: {}", echeanceId);

        EcheanceCMT echeance = echeanceCMTRepository.findById(echeanceId)
                .orElseThrow(() -> {
                    log.warn("Échéance introuvable pour synchronisation — ID: {}", echeanceId);
                    return new ResourceNotFoundException("EcheanceCMT introuvable avec l'ID: " + echeanceId);
                });

        echeance.setDateValeur(dateValeur);
        echeance.setDateOperation(dateOperation);
        echeanceCMTRepository.save(echeance);

        log.info("Échéance ID: {} synchronisée depuis transaction", echeanceId);
    }

    // -------------------------------------------------------------------------
    // Méthodes privées
    // -------------------------------------------------------------------------

    /**
     * Attache la transaction au rapprochement cible, la sauvegarde
     * et déclenche le recalcul de solde en cascade si nécessaire.
     */
    private Transaction saveTransactionWithCascade(Transaction transaction, Long banqueId, Long rapprochementId) {
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
