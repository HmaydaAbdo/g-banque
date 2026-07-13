package staport.finance.gBanque.moyensDePaiements.lcn.validation.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.lcn.repositories.LcnRepository;
import staport.finance.gBanque.moyensDePaiements.lcn.validation.dto.LcnTransactionRequest;
import staport.finance.gBanque.moyensDePaiements.lcn.validation.dto.LcnTransactionResponseDto;
import staport.finance.gBanque.moyensDePaiements.lcn.validation.mapper.LcnTransactionMapper;
import staport.finance.gBanque.moyensDePaiements.lcn.validation.services.interfaces.ILcnValidationService;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.rapprochement.entities.Rapprochement;
import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
import staport.finance.gBanque.rapprochement.repository.RapprochementRepository;
import staport.finance.gBanque.rapprochement.services.SoldeCascadeService;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.repositories.TransactionRepository;

import java.time.LocalDate;

/**
 * Implémentation du service de validation des LCN.
 *
 * <p>Coordonne la création de la transaction bancaire, l'attachement au rapprochement
 * et la mise à jour du LCN source. Extrait du {@code ValidationService} générique
 * pour respecter le bounded context du module LCN (architecture.md, Règle 1).
 */
@Service
public class LcnValidationService implements ILcnValidationService {

    private static final Logger log = LoggerFactory.getLogger(LcnValidationService.class);

    private final LcnRepository lcnRepository;
    private final LcnTransactionMapper lcnTransactionMapper;
    private final TransactionRepository transactionRepository;
    private final RapprochementRepository rapprochementRepository;
    private final SoldeCascadeService soldeCascadeService;

    public LcnValidationService(
            LcnRepository lcnRepository,
            LcnTransactionMapper lcnTransactionMapper,
            TransactionRepository transactionRepository,
            RapprochementRepository rapprochementRepository,
            SoldeCascadeService soldeCascadeService) {
        this.lcnRepository = lcnRepository;
        this.lcnTransactionMapper = lcnTransactionMapper;
        this.transactionRepository = transactionRepository;
        this.rapprochementRepository = rapprochementRepository;
        this.soldeCascadeService = soldeCascadeService;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Lcn.class, idAccessor = "getLcnId")
    public LcnTransactionResponseDto processLcnDecaissementTransaction(LcnTransactionRequest requestDto) {
        log.info("Validation LCN décaissement — id: {}", requestDto.getId());
        return processTransaction(requestDto);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Lcn.class, idAccessor = "getLcnId")
    public LcnTransactionResponseDto processLcnEncaissementTransaction(LcnTransactionRequest requestDto) {
        log.info("Validation LCN encaissement — id: {}", requestDto.getId());
        return processTransaction(requestDto);
    }

    /**
     * Logique commune décaissement/encaissement — le sens et la nature du LCN
     * ({@code Lcn.type}/{@code Lcn.natureLcn}) déterminent le traitement effectué par le mapper.
     */
    private LcnTransactionResponseDto processTransaction(LcnTransactionRequest requestDto) {
        Lcn lcn = lcnRepository.findById(requestDto.getId())
                .orElseThrow(() -> {
                    log.warn("LCN introuvable — id: {}", requestDto.getId());
                    return new ResourceNotFoundException("Lcn with id " + requestDto.getId() + " not found");
                });

        if (lcn.getStatut() != StatutMoyenPaiement.ENCOURS) {
            log.warn("Tentative de validation d'un LCN non ENCOURS — id: {}, statut: {}",
                    lcn.getId(), lcn.getStatut());
            throw new ValidationException("Le LCN id: " + lcn.getId() + " n'est pas ENCOURS.");
        }

        Transaction transactionEntity = lcnTransactionMapper.toEntity(requestDto, lcn);
        Transaction savedTransaction = saveTransactionWithCascade(
                transactionEntity, lcn.getBanque().getId(), requestDto.getRapprochementId());

        lcn.setStatut(StatutMoyenPaiement.REGLE);
        lcn.setDateOperation(savedTransaction.getDateOperations());
        lcn.setDateValeur(savedTransaction.getDateValeur());
        lcn.setLibelle(savedTransaction.getLibelle());
        lcn.setObservation(savedTransaction.getObservation());
        lcnRepository.save(lcn);

        log.info("LCN id: {} validé — transaction créée id: {}", lcn.getId(), savedTransaction.getId());
        return lcnTransactionMapper.toResponseDto(savedTransaction, lcn);
    }

    @Override
    @Transactional
    public void syncLcnFromTransaction(Long lcnId, LocalDate dateValeur, LocalDate dateOperation) {
        log.info("Synchronisation LCN depuis transaction — id: {}", lcnId);

        Lcn lcn = lcnRepository.findById(lcnId)
                .orElseThrow(() -> {
                    log.warn("LCN introuvable pour synchronisation — id: {}", lcnId);
                    return new ResourceNotFoundException("Lcn introuvable avec l'ID: " + lcnId);
                });

        lcn.setDateValeur(dateValeur);
        lcn.setDateOperation(dateOperation);
        lcnRepository.save(lcn);

        log.info("LCN id: {} synchronisé depuis transaction", lcnId);
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
