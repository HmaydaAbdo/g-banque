package staport.finance.gBanque.Agios.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Agios.entities.Agios;
import staport.finance.gBanque.Agios.exceptions.AgiosBusinessException;
import staport.finance.gBanque.Agios.exceptions.AgiosNotFoundException;
import staport.finance.gBanque.Agios.repository.AgiosRepository;
import staport.finance.gBanque.Agios.service.interfaces.IAgiosValidationService;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.rapprochement.entities.Rapprochement;
import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
import staport.finance.gBanque.rapprochement.repository.RapprochementRepository;
import staport.finance.gBanque.rapprochement.services.SoldeCascadeService;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.repositories.TransactionRepository;
import staport.finance.gBanque.validation.utils.TvaResolver;
import staport.finance.gBanque.Agios.dtos.AgiosTransactionResponse;
import staport.finance.gBanque.Agios.dtos.AgiosValidationRequest;
import staport.finance.gBanque.Agios.mapper.AgiosTransactionMapper;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Implémentation du service de validation des agios.
 * <p>
 * Coordonne la création de la transaction bancaire, l'attachement au rapprochement
 * et la mise à jour de l'agios source. Extrait du {@code ValidationService}
 * générique pour respecter le bounded context du module Agios.
 */
@Service
public class AgiosValidationService implements IAgiosValidationService {

    private static final Logger log = LoggerFactory.getLogger(AgiosValidationService.class);

    private final AgiosRepository agiosRepository;
    private final AgiosTransactionMapper agiosTransactionMapper;
    private final TransactionRepository transactionRepository;
    private final RapprochementRepository rapprochementRepository;
    private final SoldeCascadeService soldeCascadeService;

    public AgiosValidationService(
            AgiosRepository agiosRepository,
            AgiosTransactionMapper agiosTransactionMapper,
            TransactionRepository transactionRepository,
            RapprochementRepository rapprochementRepository,
            SoldeCascadeService soldeCascadeService) {
        this.agiosRepository        = agiosRepository;
        this.agiosTransactionMapper = agiosTransactionMapper;
        this.transactionRepository  = transactionRepository;
        this.rapprochementRepository = rapprochementRepository;
        this.soldeCascadeService    = soldeCascadeService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Agios.class)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AgiosTransactionResponse processAgiosToTransaction(AgiosValidationRequest dto) {
        log.info("Validation agios — agiosId: {}", dto.idAgio());

        Agios agios = agiosRepository.findById(dto.idAgio())
                .orElseThrow(() -> {
                    log.warn("Agios introuvable — ID: {}", dto.idAgio());
                    return new AgiosNotFoundException(dto.idAgio());
                });

        if (agios.getStatus() == Status.PAYE) {
            log.warn("Tentative de validation d'un agios déjà payé — ID: {}", dto.idAgio());
            throw new AgiosBusinessException("L'agios ID " + dto.idAgio() + " est déjà payé.");
        }

        // Règles de saisie de la TVA (taux choisi XOR montant saisi, bornes) : logique métier,
        // donc portée par le service et non par le mapper (conventions/architecture.md).
        BigDecimal montantTva = TvaResolver.resoudre(
                agios.getMontant(), dto.tauxTva(), dto.montantTva());

        Transaction transactionEntity = agiosTransactionMapper.toEntity(dto, agios, montantTva);
        Transaction savedTransaction  = saveTransactionWithCascade(
                transactionEntity,
                agios.getBanque().getId(),
                dto.rapprochementId());

        agios.setStatus(Status.PAYE);
        agios.setObservation(savedTransaction.getObservation());
        agios.setLibelle(savedTransaction.getLibelle());
        agios.setDateValeur(savedTransaction.getDateValeur());
        agios.setDateOperation(savedTransaction.getDateOperations());
        agiosRepository.save(agios);

        log.info("Agios ID: {} validé — transaction créée ID: {}", dto.idAgio(), savedTransaction.getId());
        return agiosTransactionMapper.toResponseDto(savedTransaction);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void syncAgiosFromTransaction(Long agiosId, LocalDate dateValeur, LocalDate dateOperation) {
        log.info("Synchronisation agios depuis transaction — agiosId: {}", agiosId);

        Agios agios = agiosRepository.findById(agiosId)
                .orElseThrow(() -> {
                    log.warn("Agios introuvable pour synchronisation — ID: {}", agiosId);
                    return new AgiosNotFoundException(agiosId);
                });

        if (agios.getStatus() != Status.PAYE) {
            log.warn("Synchronisation ignorée — agios ID: {} n'est pas PAYE", agiosId);
            return;
        }

        agios.setDateValeur(dateValeur);
        agios.setDateOperation(dateOperation);
        agiosRepository.save(agios);

        log.info("Agios ID: {} synchronisé depuis transaction", agiosId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Agios.class)
    @Transactional
    public void resetAgios(Long agiosId) {
        log.info("Réinitialisation agios — ID: {}", agiosId);

        Agios agios = agiosRepository.findById(agiosId)
                .orElseThrow(() -> {
                    log.warn("Agios introuvable pour réinitialisation — ID: {}", agiosId);
                    return new AgiosNotFoundException(agiosId);
                });

        agios.setStatus(Status.PREVU);
        agios.setDateOperation(null);
        agios.setDateValeur(null);
        agiosRepository.save(agios);

        log.info("Agios ID: {} remis à PREVU", agiosId);
    }

    // ── Méthodes privées ──────────────────────────────────────────────────────

    /**
     * Attache la transaction au rapprochement cible, la sauvegarde
     * et déclenche le recalcul de solde en cascade si nécessaire.
     */
    private Transaction saveTransactionWithCascade(Transaction transaction,
                                                    Long banqueId,
                                                    Long rapprochementId) {
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
