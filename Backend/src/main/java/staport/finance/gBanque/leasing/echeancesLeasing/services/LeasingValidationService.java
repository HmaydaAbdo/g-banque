package staport.finance.gBanque.leasing.echeancesLeasing.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.LeasingTransactionRequestDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.LeasingTransactionResponseDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.mapper.LeasingTransactionMapper;
import staport.finance.gBanque.leasing.echeancesLeasing.repositories.EcheanceLeasingRepository;
import staport.finance.gBanque.leasing.echeancesLeasing.services.interfaces.ILeasingValidationService;
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
 * Implémentation du service de validation des échéances de leasing.
 * <p>
 * Coordonne la création de la transaction bancaire, l'attachement au rapprochement
 * et la mise à jour de l'échéance source. Extrait du {@code ValidationService}
 * générique pour respecter le bounded context du module leasing.
 */
@Service
public class LeasingValidationService implements ILeasingValidationService {

    private static final Logger log = LoggerFactory.getLogger(LeasingValidationService.class);

    private final EcheanceLeasingRepository echeanceLeasingRepository;
    private final LeasingTransactionMapper leasingTransactionMapper;
    private final TransactionRepository transactionRepository;
    private final RapprochementRepository rapprochementRepository;
    private final SoldeCascadeService soldeCascadeService;

    public LeasingValidationService(
            EcheanceLeasingRepository echeanceLeasingRepository,
            LeasingTransactionMapper leasingTransactionMapper,
            TransactionRepository transactionRepository,
            RapprochementRepository rapprochementRepository,
            SoldeCascadeService soldeCascadeService) {
        this.echeanceLeasingRepository = echeanceLeasingRepository;
        this.leasingTransactionMapper = leasingTransactionMapper;
        this.transactionRepository = transactionRepository;
        this.rapprochementRepository = rapprochementRepository;
        this.soldeCascadeService = soldeCascadeService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @AuditableAction(operation = AuditOperation.UPDATE, entity = EcheanceLeasing.class, idAccessor = "echeanceId")
    public LeasingTransactionResponseDTO processLeasingTransaction(ContratLeasing contratLeasing, LeasingTransactionRequestDTO dto) {
        log.info("Validation échéance leasing — contratId: {}, écheanceId: {}", dto.contratLeasingId(), dto.echeanceId());

        EcheanceLeasing echeanceLeasing = echeanceLeasingRepository
                .findByIdAndContratLeasingId(dto.echeanceId(), dto.contratLeasingId())
                .orElseThrow(() -> {
                    log.warn("Échéance introuvable — ID: {}, contratId: {}", dto.echeanceId(), dto.contratLeasingId());
                    return new ResourceNotFoundException(
                            "Échéance de leasing introuvable avec l'ID: " + dto.echeanceId()
                            + " et contrat ID: " + dto.contratLeasingId());
                });

        if (contratLeasing.hasAvance() && echeanceLeasing.getNumeroEcheance() == 1) {
            throw new ValidationException("L'avance se règle par un chèque, une LCN ou un virement rattaché au contrat : "
                    + "validez ce moyen de paiement au lieu de l'échéance.");
        }

        if (echeanceLeasing.getStatutPaiement() == StatutEcheanceLeasing.PAYE) {
            log.warn("Tentative de validation d'une échéance déjà payée — ID: {}", dto.echeanceId());
            throw new ValidationException("L'échéance ID " + dto.echeanceId() + " est déjà payée.");
        }

        // Règles de saisie de la TVA (taux XOR montant, bornes) : logique métier, donc portée par le
        // service et non par le mapper (conventions/architecture.md). Contrairement aux autres
        // modules, la TVA reste optionnelle ici (aucune des deux saisies renseignée = pas de TVA).
        BigDecimal montantTva = (dto.tauxTva() == null && dto.montantTva() == null)
                ? BigDecimal.ZERO
                : TvaResolver.resoudre(echeanceLeasing.getMontantTTC(), dto.tauxTva(), dto.montantTva());

        Transaction transactionEntity = leasingTransactionMapper.toEntity(dto, contratLeasing, echeanceLeasing, montantTva);
        Transaction savedTransaction = saveTransactionWithCascade(
                transactionEntity,
                contratLeasing.getBanqueDePrelevement().getId(),
                dto.rapprochementId());

        echeanceLeasing.setStatutPaiement(StatutEcheanceLeasing.PAYE);
        echeanceLeasing.setDateValeur(savedTransaction.getDateValeur());
        echeanceLeasing.setDateOperation(savedTransaction.getDateOperations());
        echeanceLeasing.setLibelle(savedTransaction.getLibelle());
        echeanceLeasing.setObservation(savedTransaction.getObservation());
        echeanceLeasingRepository.save(echeanceLeasing);

        log.info("Échéance ID: {} validée — transaction créée ID: {}", dto.echeanceId(), savedTransaction.getId());
        return leasingTransactionMapper.toResponseDTO(savedTransaction);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void syncEcheanceFromTransaction(Long echeanceId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation) {
        log.info("Synchronisation échéance leasing depuis transaction — écheanceId: {}", echeanceId);

        EcheanceLeasing echeance = echeanceLeasingRepository.findById(echeanceId)
                .orElseThrow(() -> {
                    log.warn("Échéance introuvable pour synchronisation — ID: {}", echeanceId);
                    return new ResourceNotFoundException(
                            "EcheanceLeasing introuvable avec l'ID: " + echeanceId);
                });

        echeance.setDateValeur(dateValeur);
        echeance.setDateOperation(dateOperation);
        echeance.setLibelle(libelle);
        echeance.setObservation(observation);
        echeanceLeasingRepository.save(echeance);

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
