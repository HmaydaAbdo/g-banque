package staport.finance.gBanque.validation.services;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.impayees.entities.Impayee;
import staport.finance.gBanque.impayees.repository.ImpayeeRepository;
import staport.finance.gBanque.impot_taxes.entities.ImpotEtTaxe;
import staport.finance.gBanque.impot_taxes.repositories.ImpotEtTaxeRepository;
import staport.finance.gBanque.commision.entities.Commission;
import staport.finance.gBanque.commision.repository.CommisionRepository;
import staport.finance.gBanque.fraisBancaires.entities.FraisBancaire;
import staport.finance.gBanque.fraisBancaires.repositories.FraisBancaireRepository;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.operationErrone.entities.OperationErrone;
import staport.finance.gBanque.operationErrone.repositories.OperationErroneRepository;
import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
import staport.finance.gBanque.rapprochement.repository.RapprochementRepository;
import staport.finance.gBanque.rapprochement.services.SoldeCascadeService;
import staport.finance.gBanque.restitution.entities.Restitution;
import staport.finance.gBanque.restitution.repository.RestitutionRepository;
import staport.finance.gBanque.validation.dtos.commision.CommissionTransactionResponse;
import staport.finance.gBanque.validation.dtos.commision.CommissionValidationDto;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.rapprochement.entities.Rapprochement;
import staport.finance.gBanque.validation.dtos.fraisBancaire.FraisBancaireTransactionResponse;
import staport.finance.gBanque.validation.dtos.fraisBancaire.FraisBanciareValidationRequest;
import staport.finance.gBanque.validation.dtos.impayee.ImpayeeTransactionResponse;
import staport.finance.gBanque.validation.dtos.impayee.ImpayeeValidationRequest;
import staport.finance.gBanque.validation.dtos.impotsEtTaxes.ImpotEtTaxeTransactionResponse;
import staport.finance.gBanque.validation.dtos.impotsEtTaxes.ImpotEtTaxeValidationRequest;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.validation.dtos.restitutions.RestitutionTransactionResponse;
import staport.finance.gBanque.validation.dtos.restitutions.RestitutionValidationRequest;
import staport.finance.gBanque.validation.dtos.operationErrone.OperationErroneTransactionResponse;
import staport.finance.gBanque.validation.dtos.operationErrone.OperationErroneValidationRequest;
import staport.finance.gBanque.validation.mapper.*;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.repositories.TransactionRepository;


@Service
public class ValidationService {


    private static final Logger log = LoggerFactory.getLogger(ValidationService.class);

    private final CommisionRepository commisionRepository;
    private final CommisionTransactionMapper commisionTransactionMapper;

    private final FraisBancaireRepository fraisBancaireRepository;
    private final FraisBancaireTransactionMapper fraisBancaireTransactionMapper;

    private final TransactionRepository transactionRepository;
    private final BanqueRepository banqueRepository;

    private final ImpotEtTaxeRepository impotEtTaxeRepository;
    private final ImpotEtTaxeTransactionMapper impotEtTaxeTransactionMapper;

    private final RestitutionRepository restitutionRepository;
    private final RestitutionTransactionMapper restitutionTransactionMapper;

    private final OperationErroneRepository operationErroneRepository;
    private final OperationErroneTransactionMapper operationErroneTransactionMapper;

    private final ImpayeeRepository impayeeRepository;
    private final ImpayeeTransactionMapper impayeeTransactionMapper;

    private final RapprochementRepository rapprochementRepository;
    private final SoldeCascadeService soldeCascadeService;


    public ValidationService(TransactionRepository transactionRepository, BanqueRepository banqueRepository, CommisionRepository commisionRepository, CommisionTransactionMapper commisionTransactionMapper, FraisBancaireRepository fraisBancaireRepository, FraisBancaireTransactionMapper fraisBancaireTransactionMapper, ImpotEtTaxeRepository impotEtTaxeRepository, ImpotEtTaxeTransactionMapper impotEtTaxeTransactionMapper, RestitutionRepository restitutionRepository, RestitutionTransactionMapper restitutionTransactionMapper, OperationErroneRepository operationErroneRepository, OperationErroneTransactionMapper operationErroneTransactionMapper, ImpayeeRepository impayeeRepository, ImpayeeTransactionMapper impayeeTransactionMapper, RapprochementRepository rapprochementRepository, SoldeCascadeService soldeCascadeService) {
        this.transactionRepository = transactionRepository;
        this.banqueRepository = banqueRepository;
        this.commisionRepository = commisionRepository;
        this.commisionTransactionMapper = commisionTransactionMapper;
        this.fraisBancaireRepository = fraisBancaireRepository;
        this.fraisBancaireTransactionMapper = fraisBancaireTransactionMapper;
        this.impotEtTaxeRepository = impotEtTaxeRepository;
        this.impotEtTaxeTransactionMapper = impotEtTaxeTransactionMapper;
        this.restitutionRepository = restitutionRepository;
        this.restitutionTransactionMapper = restitutionTransactionMapper;
        this.operationErroneRepository = operationErroneRepository;
        this.operationErroneTransactionMapper = operationErroneTransactionMapper;
        this.impayeeRepository = impayeeRepository;
        this.impayeeTransactionMapper = impayeeTransactionMapper;
        this.rapprochementRepository = rapprochementRepository;
        this.soldeCascadeService = soldeCascadeService;
    }

    // processChequeDecaissementTransaction / processChequeEncaissementTransaction extraits vers
    // staport.finance.gBanque.moyensDePaiements.cheques.validation.services.ChequeValidationService
    // (architecture.md, Règle 1 — la logique de validation d'un module lui appartient).

    // processDecaissementLcnTransaction / processEncaissementLcnTransaction extraits vers
    // staport.finance.gBanque.moyensDePaiements.lcn.validation.services.LcnValidationService
    // (architecture.md, Règle 1 — la logique de validation d'un module lui appartient).

    // processVirementDecaissementTransaction / processVirementEncaissementTransaction extraits vers
    // staport.finance.gBanque.moyensDePaiements.virements.validation.services.VirementValidationService
    // (architecture.md, Règle 1 — la logique de validation d'un module lui appartient).

    // processMiseAdispositionDecaissementTransaction / processMiseAdispositionEncaissementTransaction
    // extraits vers staport.finance.gBanque.moyensDePaiements.miseADisposition.validation.services.
    // MiseADispositionValidationService (architecture.md, Règle 1 — la logique de validation d'un
    // module lui appartient).

    // processPrefinancementTransaction extrait vers
    // staport.finance.gBanque.prefinancement.validation.services.PrefinancementValidationService
    // (architecture.md, Règle 1 — la logique de validation d'un module lui appartient).

    // processDroitConstateTransaction extrait vers
    // staport.finance.gBanque.droitConstate.validation.services.DroitConstateValidationService
    // (architecture.md, Règle 1 — la logique de validation d'un module lui appartient).

    // processFactoringTransaction extrait vers
    // staport.finance.gBanque.factoring.validation.services.FactoringValidationService
    // (architecture.md, Règle 1 — la logique de validation d'un module lui appartient).

    // processDecompteToTransaction extrait vers
    // staport.finance.gBanque.decomptes.validation.services.DecompteValidationService
    // (architecture.md, Règle 1 — la logique de validation d'un module lui appartient).

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CommissionTransactionResponse processCommissionToTransaction(CommissionValidationDto requestDto)  {
        // Step 1: Fetch commission from the database using it's id from the request.
        Commission commission=this.commisionRepository.findById(requestDto.getIdCommission())
                .orElseThrow(()->new ResourceNotFoundException("Commission with id "+requestDto.getIdCommission()+" not found"));

        // Step 2: Check if the commission is not already paid.
        // If it's already paid , throw an exception.
        if (commission.getStatus()== Status.PAYE) {
            throw new RuntimeException(" commission is already paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched dc entity into a Transaction entity.
        Transaction transactionEntity = commisionTransactionMapper.toEntity(requestDto,commission);

        Banque banque = banqueRepository.findById(commission.getBanque().getId()).orElseThrow(()->new ResourceNotFoundException("Banque with id "+commission.getBanque().getId()+" not found"));

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = this.saveTransactionWithCascade(transactionEntity, banque.getId(), requestDto.getRapprochementId());


        // Step 6: Mark the commission as * (paid) and save the updated commission entity.
        commission.setStatus(Status.PAYE);
        commission.setObservation(savedTransaction.getObservation());
        commission.setLibelle(savedTransaction.getLibelle());
        commission.setDateValeur(savedTransaction.getDateValeur());
        commission.setDateOperation(savedTransaction.getDateOperations());
        commisionRepository.save(commission);

        // Step 7: Convert the saved Transaction entity into a CommissionTransactionResponse
        // using the mapper and return it as the response.
        return commisionTransactionMapper.toResponseDto(savedTransaction);
    }

    // Logique de validation des prélèvements extraite vers
    // staport.finance.gBanque.prelevements.validation.services.PrelevementValidationService
    // (architecture.md, Règle 1 — un ValidationService générique ne doit contenir aucune logique
    // métier propre à un module).


    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FraisBancaireTransactionResponse processFraisBancaireToTransaction(FraisBanciareValidationRequest requestDto)  {
        // Step 1: Fetch frais bancaire from the database using it's id from the request.
        FraisBancaire fraisBancaire=this.fraisBancaireRepository.findById(requestDto.getIdFraisBancaire())
                .orElseThrow(()->new ResourceNotFoundException("Interet with id "+requestDto.getIdFraisBancaire()+" not found"));

        // Step 2: Check if the frais bancaire is not already paid.
        // If it's already paid , throw an exception.
        if (fraisBancaire.getStatus()== Status.PAYE) {
            throw new RuntimeException(" frais bancaire is already paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched dc entity into a Transaction entity.
        Transaction transactionEntity = fraisBancaireTransactionMapper.toEntity(requestDto,fraisBancaire);
        Banque banque = banqueRepository.findById(fraisBancaire.getBanque().getId()).orElseThrow(()->new ResourceNotFoundException("Banque with id "+fraisBancaire.getBanque().getId()+" not found"));

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = this.saveTransactionWithCascade(transactionEntity, banque.getId(), requestDto.getRapprochementId());


        // Step 6: Mark the frais bancaire as * (paid) and save the updated agios entity.
        fraisBancaire.setStatus(Status.PAYE);
        fraisBancaire.setObservation(savedTransaction.getObservation());
        fraisBancaire.setLibelle(savedTransaction.getLibelle());
        fraisBancaire.setDateValeur(savedTransaction.getDateValeur());
        fraisBancaire.setDateOperation(savedTransaction.getDateOperations());
        fraisBancaireRepository.save(fraisBancaire);

        // Step 7: Convert the saved Transaction entity into a FraisBancaireTransactionResponse
        // using the mapper and return it as the response.
        return fraisBancaireTransactionMapper.toResponseDto(savedTransaction);
    }


    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ImpotEtTaxeTransactionResponse processImpotEtTaxeToTransaction(ImpotEtTaxeValidationRequest requestDto)   {
        // Step 1: Fetch impot et taxe from the database using its id from the request.
        ImpotEtTaxe impotEtTaxe = this.impotEtTaxeRepository.findById(requestDto.getIdImpotEtTaxe())
                .orElseThrow(() -> new ResourceNotFoundException("Impôt/Taxe with id " + requestDto.getIdImpotEtTaxe() + " not found"));

        // Step 2: Check if the impot et taxe is not already paid.
        // If it's already paid, throw an exception.
        if (impotEtTaxe.getStatus() == Status.PAYE) {
            throw new RuntimeException("L'impôt/taxe est déjà payé(e)");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched entity into a Transaction entity.
        Transaction transactionEntity = impotEtTaxeTransactionMapper.toEntity(requestDto, impotEtTaxe);

        Banque banque = banqueRepository.findById(impotEtTaxe.getBanque().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Banque with id " + impotEtTaxe.getBanque().getId() + " not found"));

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = this.saveTransactionWithCascade(transactionEntity, banque.getId(), requestDto.getRapprochementId());


        // Step 6: Mark the impot et taxe as paid and save the updated entity.
        impotEtTaxe.setStatus(Status.PAYE);
        impotEtTaxe.setObservation(savedTransaction.getObservation());
        impotEtTaxe.setLibelle(savedTransaction.getLibelle());
        impotEtTaxe.setDateValeur(savedTransaction.getDateValeur());
        impotEtTaxe.setDateOperation(savedTransaction.getDateOperations());
        impotEtTaxeRepository.save(impotEtTaxe);

        // Step 7: Convert the saved Transaction entity into an ImpotEtTaxeTransactionResponse
        // using the mapper and return it as the response.
        return impotEtTaxeTransactionMapper.toResponseDto(savedTransaction);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RestitutionTransactionResponse processRestitutionToTransaction(RestitutionValidationRequest requestDto)  {
        log.info("Début du traitement de la restitution ID: {}", requestDto.getIdRestitution());

        // Step 1: Récupérer la restitution depuis la base de données
        Restitution restitution = this.restitutionRepository.findById(requestDto.getIdRestitution())
                .orElseThrow(() -> {
                    log.error("Restitution introuvable avec l'ID: {}", requestDto.getIdRestitution());
                    return new ResourceNotFoundException("Restitution introuvable avec l'ID: " + requestDto.getIdRestitution());
                });

        // Step 2: Vérifier que la restitution n'est pas déjà payée
        if (restitution.getStatus() == Status.PAYE) {
            log.error("La restitution ID {} est déjà payée", requestDto.getIdRestitution());
            throw new RuntimeException("La restitution est déjà payée");
        }

        // Step 3: Mapper le DTO et l'entité restitution vers une Transaction
        Transaction transactionEntity = restitutionTransactionMapper.toEntity(requestDto, restitution);

        Banque banque = banqueRepository.findById(restitution.getBanque().getId())
                .orElseThrow(() -> {
                    log.error("Banque introuvable avec l'ID: {}", restitution.getBanque().getId());
                    return new ResourceNotFoundException("Banque introuvable avec l'ID: " + restitution.getBanque().getId());
                });

        // Step 4: Sauvegarder la transaction
        Transaction savedTransaction = this.saveTransactionWithCascade(transactionEntity, banque.getId(), requestDto.getRapprochementId());

        log.info("Transaction créée avec l'ID: {}", savedTransaction.getId());


        // Step 6: Marquer la restitution comme payée et mettre à jour les dates
        restitution.setStatus(Status.PAYE);
        restitution.setObservation(savedTransaction.getObservation());
        restitution.setLibelle(savedTransaction.getLibelle());
        restitution.setDateValeur(savedTransaction.getDateValeur());
        restitution.setDateOperation(savedTransaction.getDateOperations());
        restitutionRepository.save(restitution);
        log.info("Restitution ID {} marquée comme payée", restitution.getId());

        // Step 7: Convertir la transaction sauvegardée en DTO de réponse
        RestitutionTransactionResponse response = restitutionTransactionMapper.toResponseDto(savedTransaction);
        log.info("Traitement de la restitution terminé avec succès");

        return response;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OperationErroneTransactionResponse processOperationErroneToTransaction(OperationErroneValidationRequest requestDto)  {
        // Step 1: Fetch operation erroné from the database using its id from the request.
        OperationErrone operationErrone = this.operationErroneRepository.findById(requestDto.getIdOperationErrone())
                .orElseThrow(() -> new ResourceNotFoundException("operation erronée with id " + requestDto.getIdOperationErrone() + " not found"));

        // Step 2: Check if the operation erroné is not already paid.
        // If it's already paid, throw an exception.
        if (operationErrone.getStatus() == Status.PAYE) {
            throw new RuntimeException("L' operation erroné est déjà payé");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched entity into a Transaction entity.
        Transaction transactionEntity = operationErroneTransactionMapper.toEntity(requestDto, operationErrone);

        Banque banque = banqueRepository.findById(operationErrone.getBanque().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Banque with id " + operationErrone.getBanque().getId() + " not found"));

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = this.saveTransactionWithCascade(transactionEntity, banque.getId(), requestDto.getRapprochementId());


        // Step 6: Mark the operation erroné as paid and save the updated entity.
        operationErrone.setStatus(Status.PAYE);
        operationErrone.setObservation(savedTransaction.getObservation());
        operationErrone.setLibelle(savedTransaction.getLibelle());
        operationErrone.setDateValeur(savedTransaction.getDateValeur());
        operationErrone.setDateOperation(savedTransaction.getDateOperations());
        operationErroneRepository.save(operationErrone);

        // Step 7: Convert the saved Transaction entity into response dto
        // using the mapper and return it as the response.
        return operationErroneTransactionMapper.toResponseDto(savedTransaction);
    }

    // processVirementInternationalDecaissementTransaction / processVirementInternationalEncaissementTransaction
    // extraits vers staport.finance.gBanque.moyensDePaiements.virementInternational.validation.services.
    // VirementInternationalValidationService (architecture.md, Règle 1 — la logique de validation d'un
    // module lui appartient).

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ImpayeeTransactionResponse processImpayeeToTransaction(ImpayeeValidationRequest requestDto)  {
        log.info("Début du traitement de l'impayée ID: {}", requestDto.getIdImpayee());

        // Step 1: Fetch impayee from the database using its id from the request.
        Impayee impayee = this.impayeeRepository.findById(requestDto.getIdImpayee())
                .orElseThrow(() -> {
                    log.error("Impayée introuvable avec l'ID: {}", requestDto.getIdImpayee());
                    return new ResourceNotFoundException("Impayée introuvable avec l'ID: " + requestDto.getIdImpayee());
                });

        // Step 2: Check if the impayee is not already paid.
        // If it's already paid, throw an exception.
        if (impayee.getStatus() == Status.PAYE) {
            log.error("L'impayée ID {} est déjà payée", requestDto.getIdImpayee());
            throw new RuntimeException("L'impayée est déjà payée");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched entity into a Transaction entity.
        Transaction transactionEntity = impayeeTransactionMapper.toEntity(requestDto, impayee);

        Banque banque = banqueRepository.findById(impayee.getBanque().getId())
                .orElseThrow(() -> {
                    log.error("Banque d'encaissement introuvable avec l'ID: {}", impayee.getBanque().getId());
                    return new ResourceNotFoundException("Banque d'encaissement introuvable avec l'ID: " + impayee.getBanque().getId());
                });
        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = this.saveTransactionWithCascade(transactionEntity, banque.getId(), requestDto.getRapprochementId());

        log.info("Transaction créée avec l'ID: {}", savedTransaction.getId());


        // Step 6: Mark the impayee as paid and save the updated entity.
        impayee.setStatus(Status.PAYE);
        impayee.setObservation(savedTransaction.getObservation());
        impayee.setLibelle(savedTransaction.getLibelle());
        impayee.setDateValeur(savedTransaction.getDateValeur());
        impayee.setDateOperation(savedTransaction.getDateOperations());
        impayeeRepository.save(impayee);
        log.info("Impayée ID {} marquée comme payée", impayee.getId());

        // Step 7: Convert the saved Transaction entity into an ImpayeeTransactionResponse
        // using the mapper and return it as the response.
        ImpayeeTransactionResponse response = impayeeTransactionMapper.toResponseDto(savedTransaction);
        log.info("Traitement de l'impayée terminé avec succès");

        return response;
    }

    /**
     * Resolves and attaches the target rapprochement to a transaction.
     * rapprochementId is mandatory — must reference an EN_COURS or CLOTURE_PROVISOIRE rapprochement.
     */
    private Rapprochement attachToRapprochement(Transaction transaction, Long rapprochementId) {
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
        return rapprochement;
    }

    /**
     * Attaches, saves, and triggers cascade if the target rapprochement is CLOTURE_PROVISOIRE.
     * Use this in place of the old attachToRapprochementEnCours + transactionRepository.save pattern.
     */
    private Transaction saveTransactionWithCascade(Transaction transaction, Long banqueId, Long rapprochementId) {
        Rapprochement target = attachToRapprochement(transaction, rapprochementId);
        Transaction saved = transactionRepository.save(transaction);
        if (target.getStatut() == StatutRapprochement.CLOTURE_PROVISOIRE) {
            soldeCascadeService.recalculerCascade(banqueId);
        }
        return saved;
    }
}
