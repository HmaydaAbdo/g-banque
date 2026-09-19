package staport.finance.gBanque.moyensDePaiements.virements.validation.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.moyensDePaiements.virements.repositories.VirementRepository;
import staport.finance.gBanque.moyensDePaiements.virements.validation.dto.VirementTransactionRequestDto;
import staport.finance.gBanque.moyensDePaiements.virements.validation.dto.VirementTransactionResponseDto;
import staport.finance.gBanque.moyensDePaiements.virements.validation.mapper.VirementTransactionMapper;
import staport.finance.gBanque.moyensDePaiements.virements.validation.services.interfaces.IVirementValidationService;
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
 * Implémentation du service de validation des virements.
 *
 * <p>Coordonne la création de la transaction bancaire, l'attachement au rapprochement
 * et la mise à jour du virement source. Extrait du {@code ValidationService} générique
 * pour respecter le bounded context du module virements (architecture.md, Règle 1).
 */
@Service
public class VirementValidationService implements IVirementValidationService {

    private static final Logger log = LoggerFactory.getLogger(VirementValidationService.class);

    private final VirementRepository virementRepository;
    private final VirementTransactionMapper virementTransactionMapper;
    private final TransactionRepository transactionRepository;
    private final RapprochementRepository rapprochementRepository;
    private final SoldeCascadeService soldeCascadeService;

    public VirementValidationService(
            VirementRepository virementRepository,
            VirementTransactionMapper virementTransactionMapper,
            TransactionRepository transactionRepository,
            RapprochementRepository rapprochementRepository,
            SoldeCascadeService soldeCascadeService) {
        this.virementRepository = virementRepository;
        this.virementTransactionMapper = virementTransactionMapper;
        this.transactionRepository = transactionRepository;
        this.rapprochementRepository = rapprochementRepository;
        this.soldeCascadeService = soldeCascadeService;
    }

    /**
     * Valide un virement, décaissement ou encaissement indifféremment — le sens
     * ({@code Virement.type}) est lu directement sur l'entité (il détermine la banque de
     * prélèvement retenue ci-dessous : émettrice ou bénéficiaire), jamais transmis par l'appelant :
     * une seule méthode suffit, sans branchement côté appelant (fusion du 19/09/2026 des deux
     * anciennes méthodes {@code processVirementDecaissementTransaction}/
     * {@code processVirementEncaissementTransaction}, dont les corps étaient déjà identiques).
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Virement.class, idAccessor = "getVirementId")
    public VirementTransactionResponseDto processVirementTransaction(VirementTransactionRequestDto requestDto) {
        log.info("Validation virement — id: {}", requestDto.getId());

        Virement virement = virementRepository.findById(requestDto.getId())
                .orElseThrow(() -> {
                    log.warn("Virement introuvable — id: {}", requestDto.getId());
                    return new ResourceNotFoundException("Virement with id " + requestDto.getId() + " not found");
                });

        if (virement.getStatut() != StatutMoyenPaiement.ENCOURS) {
            log.warn("Tentative de validation d'un virement non ENCOURS — id: {}, statut: {}",
                    virement.getId(), virement.getStatut());
            throw new ValidationException("Le virement id: " + virement.getId() + " n'est pas ENCOURS.");
        }

        // Règles de saisie de la TVA (taux choisi XOR montant saisi, bornes) : logique métier,
        // donc portée par le service et non par le mapper (conventions/architecture.md).
        BigDecimal montantTva = TvaResolver.resoudre(
                virement.getMontant(), requestDto.getTauxTva(), requestDto.getMontantTva());

        Transaction transactionEntity = virementTransactionMapper.toEntity(requestDto, virement, montantTva);
        Banque banque = virement.getType() == Type.DECAISSEMENT
                ? virement.getBanqueEmettrice()
                : virement.getBanqueBeneficiare();
        Transaction savedTransaction = saveTransactionWithCascade(
                transactionEntity, banque != null ? banque.getId() : null, requestDto.getRapprochementId());

        virement.setStatut(StatutMoyenPaiement.REGLE);
        virement.setDateOperation(savedTransaction.getDateOperations());
        virement.setDateValeur(savedTransaction.getDateValeur());
        virement.setLibelle(savedTransaction.getLibelle());
        virement.setObservation(savedTransaction.getObservation());
        virementRepository.save(virement);

        log.info("Virement id: {} validé — transaction créée id: {}", virement.getId(), savedTransaction.getId());
        return virementTransactionMapper.toResponseDto(savedTransaction);
    }

    @Override
    @Transactional
    public void syncVirementFromTransaction(Long virementId, LocalDate dateValeur, LocalDate dateOperation) {
        log.info("Synchronisation virement depuis transaction — id: {}", virementId);

        Virement virement = virementRepository.findById(virementId)
                .orElseThrow(() -> {
                    log.warn("Virement introuvable pour synchronisation — id: {}", virementId);
                    return new ResourceNotFoundException("Virement introuvable avec l'ID: " + virementId);
                });

        virement.setDateValeur(dateValeur);
        virement.setDateOperation(dateOperation);
        virementRepository.save(virement);

        log.info("Virement id: {} synchronisé depuis transaction", virementId);
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

        if (rapprochement.getStatut() == StatutRapprochement.CLOTURE_PROVISOIRE && banqueId != null) {
            soldeCascadeService.recalculerCascade(banqueId);
        }

        return saved;
    }
}
