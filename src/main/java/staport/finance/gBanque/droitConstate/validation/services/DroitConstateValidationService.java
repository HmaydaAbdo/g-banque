package staport.finance.gBanque.droitConstate.validation.services;

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
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.droitConstate.repositories.DroitConstateRepository;
import staport.finance.gBanque.droitConstate.validation.dto.DroitConstateTransactionRequest;
import staport.finance.gBanque.droitConstate.validation.dto.DroitConstateTransactionResponse;
import staport.finance.gBanque.droitConstate.validation.mapper.DroitConstateTransactionMapper;
import staport.finance.gBanque.droitConstate.validation.services.interfaces.IDroitConstateValidationService;
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
 * Implémentation du service de validation des droits constatés.
 *
 * <p>Coordonne la création de la transaction bancaire, l'attachement au rapprochement
 * et la mise à jour du droit constaté source. Extrait du {@code ValidationService}
 * générique pour respecter le bounded context du module droitConstate (architecture.md,
 * Règle 1).
 *
 * <p>Particularité par rapport à Préfinancement/Factoring : la banque validée ici est celle
 * propre au droit constaté ({@code DroitConstate.banque}, champ direct de l'entité), pas celle du
 * marché — aucune lecture de {@code marche.getBanque()} n'est nécessaire.
 */
@Service
public class DroitConstateValidationService implements IDroitConstateValidationService {

    private static final Logger log = LoggerFactory.getLogger(DroitConstateValidationService.class);

    private final DroitConstateRepository droitConstateRepository;
    private final DroitConstateTransactionMapper droitConstateTransactionMapper;
    private final TransactionRepository transactionRepository;
    private final RapprochementRepository rapprochementRepository;
    private final SoldeCascadeService soldeCascadeService;

    /**
     * {@code @Lazy} obligatoire — même précaution que {@code PrelevementValidationService} (voir
     * historique des correctifs, entrée du 12/07/2026) : sans lui, cycle de bean Spring au
     * démarrage ({@code BanqueServiceImpl} → {@code ITransactionService} →
     * {@code IDroitConstateValidationService} → {@code IBanqueService} → {@code BanqueServiceImpl}).
     */
    private final IBanqueService banqueService;

    public DroitConstateValidationService(
            DroitConstateRepository droitConstateRepository,
            DroitConstateTransactionMapper droitConstateTransactionMapper,
            TransactionRepository transactionRepository,
            RapprochementRepository rapprochementRepository,
            SoldeCascadeService soldeCascadeService,
            @Lazy IBanqueService banqueService) {
        this.droitConstateRepository = droitConstateRepository;
        this.droitConstateTransactionMapper = droitConstateTransactionMapper;
        this.transactionRepository = transactionRepository;
        this.rapprochementRepository = rapprochementRepository;
        this.soldeCascadeService = soldeCascadeService;
        this.banqueService = banqueService;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @AuditableAction(operation = AuditOperation.UPDATE, entity = DroitConstate.class, idAccessor = "getDroitConstateId")
    public DroitConstateTransactionResponse processDroitConstateTransaction(DroitConstateTransactionRequest requestDto) {
        log.info("Validation droit constaté — id: {}", requestDto.getDroitConstateId());

        DroitConstate droitConstate = droitConstateRepository.findById(requestDto.getDroitConstateId())
                .orElseThrow(() -> {
                    log.warn("Droit constaté introuvable — id: {}", requestDto.getDroitConstateId());
                    return new ResourceNotFoundException("Droit constate with id " + requestDto.getDroitConstateId() + " not found");
                });

        if (droitConstate.getStatus() == Status.PAYE) {
            log.warn("Tentative de validation d'un droit constaté déjà PAYE — id: {}", droitConstate.getId());
            throw new ValidationException("Le droit constaté id: " + droitConstate.getId() + " est déjà payé.");
        }

        if (droitConstate.getMarche() == null) {
            throw new ResourceNotFoundException("Marché introuvable pour le droit constaté " + droitConstate.getId());
        }

        Banque banque = banqueService.findOrThrow(requestDto.getBanqueId());
        if (droitConstate.getBanque() == null || !droitConstate.getBanque().getId().equals(banque.getId())) {
            throw new ValidationException("Le banqueId fourni (" + requestDto.getBanqueId()
                    + ") ne correspond pas à la banque du droit constaté "
                    + droitConstate.getId() + ".");
        }

        // Règles de saisie de la TVA (taux choisi XOR montant saisi, bornes) : logique métier,
        // donc portée par le service et non par le mapper (conventions/architecture.md).
        BigDecimal montantTva = TvaResolver.resoudre(
                droitConstate.getMontant(), requestDto.getTauxTva(), requestDto.getMontantTva());

        Transaction transactionEntity = droitConstateTransactionMapper.toEntity(requestDto, droitConstate, banque, montantTva);
        Transaction savedTransaction = saveTransactionWithCascade(
                transactionEntity, banque.getId(), requestDto.getRapprochementId());

        droitConstate.setStatus(Status.PAYE);
        droitConstate.setDateOperation(savedTransaction.getDateOperations());
        droitConstate.setDateValeur(savedTransaction.getDateValeur());
        droitConstate.setObservation(savedTransaction.getObservation());
        droitConstate.setLibelle(savedTransaction.getLibelle());
        droitConstateRepository.save(droitConstate);

        log.info("Droit constaté id: {} validé — transaction créée id: {}", droitConstate.getId(), savedTransaction.getId());
        return droitConstateTransactionMapper.toResponseDto(savedTransaction);
    }

    @Override
    @Transactional
    public void syncDroitConstateFromTransaction(Long droitConstateId, LocalDate dateValeur, LocalDate dateOperation) {
        log.info("Synchronisation droit constaté depuis transaction — id: {}", droitConstateId);

        DroitConstate droitConstate = droitConstateRepository.findById(droitConstateId)
                .orElseThrow(() -> {
                    log.warn("Droit constaté introuvable pour synchronisation — id: {}", droitConstateId);
                    return new ResourceNotFoundException("Droit constaté introuvable avec l'ID: " + droitConstateId);
                });

        droitConstate.setDateValeur(dateValeur);
        droitConstate.setDateOperation(dateOperation);
        droitConstateRepository.save(droitConstate);

        log.info("Droit constaté id: {} synchronisé depuis transaction", droitConstateId);
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
