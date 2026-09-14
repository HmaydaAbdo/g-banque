package staport.finance.gBanque.moyensDePaiements.miseADisposition.validation.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.repositories.MiseADispositionRepository;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.validation.dto.MiseAdispositionTransactionRequestDto;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.validation.dto.MiseAdispositionTransactionResponseDto;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.validation.mapper.MiseAdispositionTransactionMapper;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.validation.services.interfaces.IMiseADispositionValidationService;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
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
 * Implémentation du service de validation des mises à disposition.
 *
 * <p>Coordonne la création de la transaction bancaire, l'attachement au rapprochement
 * et la mise à jour de la mise à disposition source. Extrait du {@code ValidationService}
 * générique pour respecter le bounded context du module miseADisposition (architecture.md,
 * Règle 1).
 */
@Service
public class MiseADispositionValidationService implements IMiseADispositionValidationService {

    private static final Logger log = LoggerFactory.getLogger(MiseADispositionValidationService.class);

    private final MiseADispositionRepository miseADispositionRepository;
    private final MiseAdispositionTransactionMapper miseAdispositionTransactionMapper;
    private final TransactionRepository transactionRepository;
    private final RapprochementRepository rapprochementRepository;
    private final SoldeCascadeService soldeCascadeService;

    public MiseADispositionValidationService(
            MiseADispositionRepository miseADispositionRepository,
            MiseAdispositionTransactionMapper miseAdispositionTransactionMapper,
            TransactionRepository transactionRepository,
            RapprochementRepository rapprochementRepository,
            SoldeCascadeService soldeCascadeService) {
        this.miseADispositionRepository = miseADispositionRepository;
        this.miseAdispositionTransactionMapper = miseAdispositionTransactionMapper;
        this.transactionRepository = transactionRepository;
        this.rapprochementRepository = rapprochementRepository;
        this.soldeCascadeService = soldeCascadeService;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @AuditableAction(operation = AuditOperation.UPDATE, entity = MiseADisposition.class, idAccessor = "getMiseADispositionId")
    public MiseAdispositionTransactionResponseDto processMiseAdispositionDecaissementTransaction(MiseAdispositionTransactionRequestDto requestDto) {
        log.info("Validation mise à disposition décaissement — id: {}", requestDto.getId());
        return processTransaction(requestDto);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @AuditableAction(operation = AuditOperation.UPDATE, entity = MiseADisposition.class, idAccessor = "getMiseADispositionId")
    public MiseAdispositionTransactionResponseDto processMiseAdispositionEncaissementTransaction(MiseAdispositionTransactionRequestDto requestDto) {
        log.info("Validation mise à disposition encaissement — id: {}", requestDto.getId());
        return processTransaction(requestDto);
    }

    private MiseAdispositionTransactionResponseDto processTransaction(MiseAdispositionTransactionRequestDto requestDto) {
        MiseADisposition miseADisposition = miseADispositionRepository.findById(requestDto.getId())
                .orElseThrow(() -> {
                    log.warn("Mise à disposition introuvable — id: {}", requestDto.getId());
                    return new ResourceNotFoundException("Mise à disposition with id " + requestDto.getId() + " not found");
                });

        if (miseADisposition.getStatut() != StatutMoyenPaiement.ENCOURS) {
            log.warn("Tentative de validation d'une mise à disposition non ENCOURS — id: {}, statut: {}",
                    miseADisposition.getId(), miseADisposition.getStatut());
            throw new ValidationException("La mise à disposition id: " + miseADisposition.getId() + " n'est pas ENCOURS.");
        }

        // Règles de saisie de la TVA (taux choisi XOR montant saisi, bornes) : logique métier,
        // donc portée par le service et non par le mapper (conventions/architecture.md).
        BigDecimal montantTva = TvaResolver.resoudre(
                miseADisposition.getMontant(), requestDto.getTauxTva(), requestDto.getMontantTva());

        Transaction transactionEntity = miseAdispositionTransactionMapper.toEntity(requestDto, miseADisposition, montantTva);
        Transaction savedTransaction = saveTransactionWithCascade(
                transactionEntity, miseADisposition.getBanque().getId(), requestDto.getRapprochementId());

        miseADisposition.setStatut(StatutMoyenPaiement.REGLE);
        miseADisposition.setDateOperation(savedTransaction.getDateOperations());
        miseADisposition.setDateValeur(savedTransaction.getDateValeur());
        miseADisposition.setLibelle(savedTransaction.getLibelle());
        miseADisposition.setObservation(savedTransaction.getObservation());
        miseADispositionRepository.save(miseADisposition);

        log.info("Mise à disposition id: {} validée — transaction créée id: {}", miseADisposition.getId(), savedTransaction.getId());
        return miseAdispositionTransactionMapper.toResponseDto(savedTransaction, miseADisposition);
    }

    @Override
    @Transactional
    public void syncMiseADispositionFromTransaction(Long miseADispositionId, LocalDate dateValeur, LocalDate dateOperation) {
        log.info("Synchronisation mise à disposition depuis transaction — id: {}", miseADispositionId);

        MiseADisposition miseADisposition = miseADispositionRepository.findById(miseADispositionId)
                .orElseThrow(() -> {
                    log.warn("Mise à disposition introuvable pour synchronisation — id: {}", miseADispositionId);
                    return new ResourceNotFoundException("Mise à disposition introuvable avec l'ID: " + miseADispositionId);
                });

        miseADisposition.setDateValeur(dateValeur);
        miseADisposition.setDateOperation(dateOperation);
        miseADispositionRepository.save(miseADisposition);

        log.info("Mise à disposition id: {} synchronisée depuis transaction", miseADispositionId);
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
