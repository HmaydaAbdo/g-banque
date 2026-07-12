package staport.finance.gBanque.moyensDePaiements.cheques.validation.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.moyensDePaiements.cheques.entities.Cheque;
import staport.finance.gBanque.moyensDePaiements.cheques.repositories.ChequeRepository;
import staport.finance.gBanque.moyensDePaiements.cheques.validation.dto.ChequeTransactionRequest;
import staport.finance.gBanque.moyensDePaiements.cheques.validation.dto.ChequeTransactionResponseDto;
import staport.finance.gBanque.moyensDePaiements.cheques.validation.mapper.ChequeTransactionMapper;
import staport.finance.gBanque.moyensDePaiements.cheques.validation.services.interfaces.IChequeValidationService;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.rapprochement.entities.Rapprochement;
import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
import staport.finance.gBanque.rapprochement.repository.RapprochementRepository;
import staport.finance.gBanque.rapprochement.services.SoldeCascadeService;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.repositories.TransactionRepository;

import java.time.LocalDate;

/**
 * Implémentation du service de validation des chèques.
 *
 * <p>Coordonne la création de la transaction bancaire, l'attachement au rapprochement
 * et la mise à jour du chèque source. Extrait du {@code ValidationService} générique
 * pour respecter le bounded context du module chèques (architecture.md, Règle 1).
 */
@Service
public class ChequeValidationService implements IChequeValidationService {

    private static final Logger log = LoggerFactory.getLogger(ChequeValidationService.class);

    private final ChequeRepository chequeRepository;
    private final ChequeTransactionMapper chequeTransactionMapper;
    private final TransactionRepository transactionRepository;
    private final RapprochementRepository rapprochementRepository;
    private final SoldeCascadeService soldeCascadeService;

    public ChequeValidationService(
            ChequeRepository chequeRepository,
            ChequeTransactionMapper chequeTransactionMapper,
            TransactionRepository transactionRepository,
            RapprochementRepository rapprochementRepository,
            SoldeCascadeService soldeCascadeService) {
        this.chequeRepository = chequeRepository;
        this.chequeTransactionMapper = chequeTransactionMapper;
        this.transactionRepository = transactionRepository;
        this.rapprochementRepository = rapprochementRepository;
        this.soldeCascadeService = soldeCascadeService;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Cheque.class, idAccessor = "idCheque")
    public ChequeTransactionResponseDto processChequeDecaissementTransaction(ChequeTransactionRequest requestDto) {
        log.info("Validation chèque décaissement — id: {}", requestDto.getIdCheque());
        return processTransaction(requestDto);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Cheque.class, idAccessor = "idCheque")
    public ChequeTransactionResponseDto processChequeEncaissementTransaction(ChequeTransactionRequest requestDto) {
        log.info("Validation chèque encaissement — id: {}", requestDto.getIdCheque());
        return processTransaction(requestDto);
    }

    /**
     * Logique commune décaissement/encaissement — seul le sens du chèque
     * ({@code Cheque.type}) change le traitement effectué par le mapper.
     */
    private ChequeTransactionResponseDto processTransaction(ChequeTransactionRequest requestDto) {
        Cheque cheque = chequeRepository.findById(requestDto.getIdCheque())
                .orElseThrow(() -> {
                    log.warn("Chèque introuvable — id: {}", requestDto.getIdCheque());
                    return new ResourceNotFoundException(
                            "Cheque with id " + requestDto.getIdCheque() + " not found");
                });

        if (cheque.getStatut() != StatutMoyenPaiement.ENCOURS) {
            log.warn("Tentative de validation d'un chèque non ENCOURS — id: {}, statut: {}",
                    cheque.getId(), cheque.getStatut());
            throw new ValidationException("Le chèque id: " + cheque.getId() + " n'est pas ENCOURS.");
        }

        Transaction transactionEntity = chequeTransactionMapper.toEntity(requestDto, cheque);
        Transaction savedTransaction = saveTransactionWithCascade(
                transactionEntity, cheque.getBanque().getId(), requestDto.getRapprochementId());

        cheque.setStatut(StatutMoyenPaiement.REGLE);
        cheque.setDateOperation(savedTransaction.getDateOperations());
        cheque.setDateValeur(savedTransaction.getDateValeur());
        cheque.setLibelle(savedTransaction.getLibelle());
        cheque.setObservation(savedTransaction.getObservation());
        chequeRepository.save(cheque);

        log.info("Chèque id: {} validé — transaction créée id: {}", cheque.getId(), savedTransaction.getId());
        return chequeTransactionMapper.toResponseDto(savedTransaction);
    }

    @Override
    @Transactional
    public void syncChequeFromTransaction(Long chequeId, LocalDate dateValeur, LocalDate dateOperation) {
        log.info("Synchronisation chèque depuis transaction — id: {}", chequeId);

        Cheque cheque = chequeRepository.findById(chequeId)
                .orElseThrow(() -> {
                    log.warn("Chèque introuvable pour synchronisation — id: {}", chequeId);
                    return new ResourceNotFoundException("Cheque introuvable avec l'ID: " + chequeId);
                });

        cheque.setDateValeur(dateValeur);
        cheque.setDateOperation(dateOperation);
        chequeRepository.save(cheque);

        log.info("Chèque id: {} synchronisé depuis transaction", chequeId);
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
