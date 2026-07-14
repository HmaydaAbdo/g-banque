package staport.finance.gBanque.fraisBancaires.validation.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.fraisBancaires.entities.FraisBancaire;
import staport.finance.gBanque.fraisBancaires.repositories.FraisBancaireRepository;
import staport.finance.gBanque.fraisBancaires.validation.dto.FraisBancaireTransactionRequestDto;
import staport.finance.gBanque.fraisBancaires.validation.dto.FraisBancaireTransactionResponseDto;
import staport.finance.gBanque.fraisBancaires.validation.mapper.FraisBancaireTransactionMapper;
import staport.finance.gBanque.fraisBancaires.validation.services.interfaces.IFraisBancaireValidationService;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.rapprochement.entities.Rapprochement;
import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
import staport.finance.gBanque.rapprochement.repository.RapprochementRepository;
import staport.finance.gBanque.rapprochement.services.SoldeCascadeService;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.repositories.TransactionRepository;

import java.time.LocalDate;

/**
 * Implémentation du service de validation des frais bancaires.
 *
 * <p>Extrait du {@code ValidationService} générique pour respecter le bounded context du module
 * fraisBancaires (architecture.md, Règle 1).
 */
@Service
public class FraisBancaireValidationService implements IFraisBancaireValidationService {

    private static final Logger log = LoggerFactory.getLogger(FraisBancaireValidationService.class);

    private final FraisBancaireRepository fraisBancaireRepository;
    private final FraisBancaireTransactionMapper fraisBancaireTransactionMapper;
    private final TransactionRepository transactionRepository;
    private final RapprochementRepository rapprochementRepository;
    private final SoldeCascadeService soldeCascadeService;

    /**
     * {@code @Lazy} obligatoire — même précaution que {@code PrelevementValidationService}/
     * {@code DecompteValidationService} : casse le cycle de bean Spring
     * {@code BanqueServiceImpl} → {@code ITransactionService} → {@code IFraisBancaireValidationService}
     * → {@code IBanqueService} → {@code BanqueServiceImpl}.
     */
    private final IBanqueService banqueService;

    public FraisBancaireValidationService(
            FraisBancaireRepository fraisBancaireRepository,
            FraisBancaireTransactionMapper fraisBancaireTransactionMapper,
            TransactionRepository transactionRepository,
            RapprochementRepository rapprochementRepository,
            SoldeCascadeService soldeCascadeService,
            @Lazy IBanqueService banqueService) {
        this.fraisBancaireRepository = fraisBancaireRepository;
        this.fraisBancaireTransactionMapper = fraisBancaireTransactionMapper;
        this.transactionRepository = transactionRepository;
        this.rapprochementRepository = rapprochementRepository;
        this.soldeCascadeService = soldeCascadeService;
        this.banqueService = banqueService;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @AuditableAction(operation = AuditOperation.UPDATE, entity = FraisBancaire.class, idAccessor = "fraisBancaireId")
    public FraisBancaireTransactionResponseDto processFraisBancaireTransaction(FraisBancaireTransactionRequestDto requestDto) {
        log.info("Validation frais bancaire — id: {}", requestDto.idFraisBancaire());

        FraisBancaire fraisBancaire = fraisBancaireRepository.findById(requestDto.idFraisBancaire())
                .orElseThrow(() -> {
                    log.warn("Frais bancaire introuvable — id: {}", requestDto.idFraisBancaire());
                    return new ResourceNotFoundException("Frais bancaire introuvable avec l'ID: " + requestDto.idFraisBancaire());
                });

        if (fraisBancaire.getStatus() == Status.PAYE) {
            log.warn("Tentative de validation d'un frais bancaire déjà PAYE — id: {}", fraisBancaire.getId());
            throw new ValidationException("Le frais bancaire id: " + fraisBancaire.getId() + " est déjà payé.");
        }

        // Résolution via IBanqueService (encapsulation des domaines) plutôt que via
        // fraisBancaire.getBanque() directement — cohérent avec le reste des XxxValidationService.
        banqueService.findOrThrow(fraisBancaire.getBanque().getId());

        Transaction transactionEntity = fraisBancaireTransactionMapper.toEntity(requestDto, fraisBancaire);
        Transaction savedTransaction = saveTransactionWithCascade(
                transactionEntity, fraisBancaire.getBanque().getId(), requestDto.rapprochementId());

        fraisBancaire.setStatus(Status.PAYE);
        fraisBancaire.setDateOperation(savedTransaction.getDateOperations());
        fraisBancaire.setDateValeur(savedTransaction.getDateValeur());
        fraisBancaire.setObservation(savedTransaction.getObservation());
        fraisBancaire.setLibelle(savedTransaction.getLibelle());
        fraisBancaireRepository.save(fraisBancaire);

        log.info("Frais bancaire id: {} validé — transaction créée id: {}", fraisBancaire.getId(), savedTransaction.getId());
        return fraisBancaireTransactionMapper.toResponseDto(savedTransaction);
    }

    @Override
    @Transactional
    public void syncFraisBancaireFromTransaction(Long fraisBancaireId, LocalDate dateValeur, LocalDate dateOperation) {
        log.info("Synchronisation frais bancaire depuis transaction — id: {}", fraisBancaireId);

        FraisBancaire fraisBancaire = fraisBancaireRepository.findById(fraisBancaireId)
                .orElseThrow(() -> {
                    log.warn("Frais bancaire introuvable pour synchronisation — id: {}", fraisBancaireId);
                    return new ResourceNotFoundException("Frais bancaire introuvable avec l'ID: " + fraisBancaireId);
                });

        fraisBancaire.setDateValeur(dateValeur);
        fraisBancaire.setDateOperation(dateOperation);
        fraisBancaireRepository.save(fraisBancaire);

        log.info("Frais bancaire id: {} synchronisé depuis transaction", fraisBancaireId);
    }

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
