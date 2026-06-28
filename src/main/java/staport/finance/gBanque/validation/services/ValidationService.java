package staport.finance.gBanque.validation.services;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Agios.entities.Agios;
import staport.finance.gBanque.Agios.repository.AgiosRepository;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.impayees.entities.Impayee;
import staport.finance.gBanque.impayees.repository.ImpayeeRepository;
import staport.finance.gBanque.impot_taxes.entities.ImpotEtTaxe;
import staport.finance.gBanque.impot_taxes.repositories.ImpotEtTaxeRepository;
import staport.finance.gBanque.moyensDePaiements.cheques.entities.Cheque;
import staport.finance.gBanque.moyensDePaiements.cheques.repositories.ChequeRepository;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;
import staport.finance.gBanque.cmt.echeances.enums.StatutEcheanceCMT;
import staport.finance.gBanque.cmt.echeances.repositories.EcheanceCMTRepository;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;
import staport.finance.gBanque.cmt.repositories.CreditMoyenTermeRepository;
import staport.finance.gBanque.commision.entities.Commission;
import staport.finance.gBanque.commision.repository.CommisionRepository;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.factoring.repository.FactoringRepository;
import staport.finance.gBanque.fraisBancaires.entities.FraisBancaire;
import staport.finance.gBanque.fraisBancaires.repositories.FraisBancaireRepository;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.virementInternational.entities.VirementInternational;
import staport.finance.gBanque.moyensDePaiements.virementInternational.repositories.VirementInternationalRepository;
import staport.finance.gBanque.operationErrone.entities.OperationErrone;
import staport.finance.gBanque.operationErrone.repositories.OperationErroneRepository;
import staport.finance.gBanque.prelevements.entitie.Prelevement;
import staport.finance.gBanque.prelevements.repository.PrelevementRepository;
import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
import staport.finance.gBanque.rapprochement.repository.RapprochementRepository;
import staport.finance.gBanque.rapprochement.services.SoldeCascadeService;
import staport.finance.gBanque.restitution.entities.Restitution;
import staport.finance.gBanque.restitution.repository.RestitutionRepository;
import staport.finance.gBanque.validation.dtos.agios.AgiosTransactionResponse;
import staport.finance.gBanque.validation.dtos.agios.AgiosValidationRequest;
import staport.finance.gBanque.validation.dtos.commision.CommissionTransactionResponse;
import staport.finance.gBanque.validation.dtos.commision.CommissionValidationDto;
import staport.finance.gBanque.validation.dtos.decomptes.DecompteTransactionRequestDto;
import staport.finance.gBanque.validation.dtos.decomptes.DecompteTransactionResponseDto;
import staport.finance.gBanque.decomptes.entities.Decompte;
import staport.finance.gBanque.decomptes.repositories.DecompteRepository;
import staport.finance.gBanque.validation.dtos.droitConstate.DroitConstateTransactionRequest;
import staport.finance.gBanque.validation.dtos.droitConstate.DroitConstateTransactionResponse;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.droitConstate.repositories.DroitConstateRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.rapprochement.entities.Rapprochement;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.lcn.repositories.LcnRepository;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.repositories.MiseADispositionRepository;
import staport.finance.gBanque.validation.dtos.factoring.FactoringTransactionRequest;
import staport.finance.gBanque.validation.dtos.factoring.FactoringTransactionResponse;
import staport.finance.gBanque.validation.dtos.fraisBancaire.FraisBancaireTransactionResponse;
import staport.finance.gBanque.validation.dtos.fraisBancaire.FraisBanciareValidationRequest;
import staport.finance.gBanque.validation.dtos.impayee.ImpayeeTransactionResponse;
import staport.finance.gBanque.validation.dtos.impayee.ImpayeeValidationRequest;
import staport.finance.gBanque.validation.dtos.impotsEtTaxes.ImpotEtTaxeTransactionResponse;
import staport.finance.gBanque.validation.dtos.impotsEtTaxes.ImpotEtTaxeValidationRequest;
import staport.finance.gBanque.validation.dtos.prefinancements.PrefinancementTransactionRequest;
import staport.finance.gBanque.validation.dtos.prefinancements.PrefinancementTransactionResponse;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.repository.PrefinancementRepository;
import staport.finance.gBanque.validation.dtos.cheques.ChequeTransactionRequest;
import staport.finance.gBanque.validation.dtos.cheques.ChequeTransactionResponseDto;
import staport.finance.gBanque.validation.dtos.cmt.CmtTransactionRequestDto;
import staport.finance.gBanque.validation.dtos.cmt.CmtTransactionResponseDto;
import staport.finance.gBanque.validation.dtos.lcn.LcnTransactionRequest;
import staport.finance.gBanque.validation.dtos.lcn.LcnTransactionResponse;
import staport.finance.gBanque.validation.dtos.miseAdispo.MiseAdispositionTransactionRequestDto;
import staport.finance.gBanque.validation.dtos.miseAdispo.MiseAdispositionTransactionResponseDto;
import staport.finance.gBanque.validation.dtos.prelevements.PrelevementTransactionResponse;
import staport.finance.gBanque.validation.dtos.prelevements.PrelevementValidationRequest;
import staport.finance.gBanque.validation.dtos.restitutions.RestitutionTransactionResponse;
import staport.finance.gBanque.validation.dtos.restitutions.RestitutionValidationRequest;
import staport.finance.gBanque.validation.dtos.operationErrone.OperationErroneTransactionResponse;
import staport.finance.gBanque.validation.dtos.operationErrone.OperationErroneValidationRequest;
import staport.finance.gBanque.validation.dtos.virementInternationaux.VirementInternationalTransactionRequestDto;
import staport.finance.gBanque.validation.dtos.virementInternationaux.VirementInternationalTransactionResponseDto;
import staport.finance.gBanque.validation.dtos.virements.VirementTransactionRequestDto;
import staport.finance.gBanque.validation.dtos.virements.VirementTransactionResponseDto;
import staport.finance.gBanque.validation.mapper.*;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.repositories.TransactionRepository;
import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;
import staport.finance.gBanque.moyensDePaiements.virements.repositories.VirementRepository;


@Service
public class ValidationService {


    private static final Logger log = LoggerFactory.getLogger(ValidationService.class);

    private final CreditMoyenTermeRepository creditMoyenTermeRepository;
    private final EcheanceCMTRepository echeanceCMTRepository;
    private final CmtTransactionMapper cmtTransactionMapper;

    private final ChequeRepository chequeRepository;
    private final ChequeTransactionMapper chequeTransactionMapper;

    private final LcnRepository lcnRepository;
    private final LcnTransactionMapper lcnTransactionMapper;

    private final VirementTransactionMapper virementTransactionMapper;
    private final VirementRepository virementRepository;

    private final MiseADispositionRepository miseADispositionRepository;
    private final MiseAdispositionTransactionMapper miseAdispositionTransactionMapper;

    private final PrefinancementRepository prefinancementRepository;
    private final PrefinancementTransactionMapper prefinancementTransactionMapper;

    private final DroitConstateRepository droitConstateRepository;
    private final DroitConstateTransactionMapper droitConstateTransactionMapper;

    private final DecompteRepository decompteRepository;
    private final DecompteTransactionMapper decompteTransactionMapper;

    private final CommisionRepository commisionRepository;
    private final CommisionTransactionMapper commisionTransactionMapper;

    private final AgiosTransactionMapper agiosTransactionMapper;
    private  final AgiosRepository agiosRepository;

    private final PrelevementRepository prelevementRepository;
    private final PrelevementTransactionMapper prelevementTransactionMapper;

    private final FraisBancaireRepository fraisBancaireRepository;
    private final FraisBancaireTransactionMapper fraisBancaireTransactionMapper;

    private  final FactoringRepository factoringRepository;
    private final FactoringTransactionMapper factoringTransactionMapper;

    private final TransactionRepository transactionRepository;
    private final BanqueRepository banqueRepository;

    private final ImpotEtTaxeRepository impotEtTaxeRepository;
    private final ImpotEtTaxeTransactionMapper impotEtTaxeTransactionMapper;

    private final RestitutionRepository restitutionRepository;
    private final RestitutionTransactionMapper restitutionTransactionMapper;

    private final OperationErroneRepository operationErroneRepository;
    private final OperationErroneTransactionMapper operationErroneTransactionMapper;

    private final VirementInternationalTransactionMapper virementInternationalTransactionMapper;
    private final VirementInternationalRepository virementInternationalRepository;

    private final ImpayeeRepository impayeeRepository;
    private final ImpayeeTransactionMapper impayeeTransactionMapper;

    private final RapprochementRepository rapprochementRepository;
    private final SoldeCascadeService soldeCascadeService;


    public ValidationService(CmtTransactionMapper cmtMapper, TransactionRepository transactionRepository, BanqueRepository banqueRepository, CreditMoyenTermeRepository creditMoyenTermeRepository, EcheanceCMTRepository echeanceCMTRepository, ChequeRepository chequeRepository, ChequeTransactionMapper chequeTransactionMapper, LcnRepository lcnRepository, LcnTransactionMapper lcnTransactionMapper, VirementTransactionMapper virementTransactionMapper, VirementRepository virementRepository, MiseADispositionRepository miseADispositionRepository, MiseAdispositionTransactionMapper miseAdispositionTransactionMapper, PrefinancementRepository prefinancementRepository, PrefinancementTransactionMapper prefinancementTransactionMapper, DroitConstateRepository droitConstateRepository, DroitConstateTransactionMapper droitConstateTransactionMapper, DecompteRepository decompteRepository, DecompteTransactionMapper decompteTransactionMapper, CommisionRepository commisionRepository, CommisionTransactionMapper commisionTransactionMapper, AgiosTransactionMapper agiosTransactionMapper, AgiosRepository agiosRepository, PrelevementRepository prelevementRepository, PrelevementTransactionMapper prelevementTransactionMapper, FraisBancaireRepository fraisBancaireRepository, FraisBancaireTransactionMapper fraisBancaireTransactionMapper, FactoringRepository factoringRepository, FactoringTransactionMapper factoringTransactionMapper, ImpotEtTaxeRepository impotEtTaxeRepository, ImpotEtTaxeTransactionMapper impotEtTaxeTransactionMapper, RestitutionRepository restitutionRepository, RestitutionTransactionMapper restitutionTransactionMapper, OperationErroneRepository operationErroneRepository, OperationErroneTransactionMapper operationErroneTransactionMapper, VirementInternationalTransactionMapper virementInternationalTransactionMapper, VirementInternationalRepository virementInternationalRepository, ImpayeeRepository impayeeRepository, ImpayeeTransactionMapper impayeeTransactionMapper, RapprochementRepository rapprochementRepository, SoldeCascadeService soldeCascadeService) {
        this.cmtTransactionMapper = cmtMapper;
        this.transactionRepository = transactionRepository;
        this.banqueRepository = banqueRepository;
        this.creditMoyenTermeRepository = creditMoyenTermeRepository;
        this.echeanceCMTRepository = echeanceCMTRepository;
        this.chequeRepository = chequeRepository;
        this.chequeTransactionMapper = chequeTransactionMapper;
        this.lcnRepository = lcnRepository;
        this.lcnTransactionMapper = lcnTransactionMapper;
        this.virementTransactionMapper = virementTransactionMapper;
        this.virementRepository = virementRepository;
        this.miseADispositionRepository = miseADispositionRepository;
        this.miseAdispositionTransactionMapper = miseAdispositionTransactionMapper;
        this.prefinancementRepository = prefinancementRepository;
        this.prefinancementTransactionMapper = prefinancementTransactionMapper;
        this.droitConstateRepository = droitConstateRepository;
        this.droitConstateTransactionMapper = droitConstateTransactionMapper;
        this.decompteRepository = decompteRepository;
        this.decompteTransactionMapper = decompteTransactionMapper;
        this.commisionRepository = commisionRepository;
        this.commisionTransactionMapper = commisionTransactionMapper;
        this.agiosTransactionMapper = agiosTransactionMapper;
        this.agiosRepository = agiosRepository;
        this.prelevementRepository = prelevementRepository;
        this.prelevementTransactionMapper = prelevementTransactionMapper;
        this.fraisBancaireRepository = fraisBancaireRepository;
        this.fraisBancaireTransactionMapper = fraisBancaireTransactionMapper;
        this.factoringRepository = factoringRepository;
        this.factoringTransactionMapper = factoringTransactionMapper;
        this.impotEtTaxeRepository = impotEtTaxeRepository;
        this.impotEtTaxeTransactionMapper = impotEtTaxeTransactionMapper;
        this.restitutionRepository = restitutionRepository;
        this.restitutionTransactionMapper = restitutionTransactionMapper;
        this.operationErroneRepository = operationErroneRepository;
        this.operationErroneTransactionMapper = operationErroneTransactionMapper;
        this.virementInternationalTransactionMapper = virementInternationalTransactionMapper;
        this.virementInternationalRepository = virementInternationalRepository;
        this.impayeeRepository = impayeeRepository;
        this.impayeeTransactionMapper = impayeeTransactionMapper;
        this.rapprochementRepository = rapprochementRepository;
        this.soldeCascadeService = soldeCascadeService;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CmtTransactionResponseDto processCmtTransaction(CmtTransactionRequestDto requestDto)   {


        // Étape 1: Récupérer les entités liées avant le mappage
        CreditMoyenTerme creditMoyenTerme = creditMoyenTermeRepository.findById(requestDto.getCmtId())
                .orElseThrow(() -> new ResourceNotFoundException("Credit Moyen Terme introuvable avec l'ID: " + requestDto.getCmtId()));

        EcheanceCMT echeanceCMT = echeanceCMTRepository.findByIdAndCreditMoyenTermeId(requestDto.getEcheanceId(), requestDto.getCmtId())
                .orElseThrow(() -> new ResourceNotFoundException("Echeance cmt introuvable avec l'ID: " + requestDto.getEcheanceId() + " et credit id " + requestDto.getCmtId()));

        if( echeanceCMT.getStatutPaiement() == StatutEcheanceCMT.PAYE){
            throw new RuntimeException("l echéance est deja payee");
        }

        // Étape 2: Utiliser le mapper pour convertir le DTO et les entités en une nouvelle entité Transaction.
        Transaction transactionEntity = cmtTransactionMapper.toEntity(requestDto, creditMoyenTerme, echeanceCMT);

        // Étape 3: Attacher au rapprochement  puis sauvegarder.
        Banque banque = creditMoyenTerme.getBanque();
        Transaction savedTransaction = this.saveTransactionWithCascade(transactionEntity, banque.getId(), requestDto.getRapprochementId());

        // Étape 5 : Marquer l'échéance comme payée et enregistrer les dates rélatives au rapprochement .
        echeanceCMT.setStatutPaiement(StatutEcheanceCMT.PAYE);
        echeanceCMT.setDateValeur(savedTransaction.getDateValeur());
        echeanceCMT.setDateOperation(savedTransaction.getDateOperations());
        echeanceCMT.setLibelle(savedTransaction.getLibelle());
        echeanceCMT.setObservation(savedTransaction.getObservation());
        echeanceCMTRepository.save(echeanceCMT);

        // Étape 6: Utiliser le mapper pour convertir l'entité sauvegardée en DTO de réponse.
        return cmtTransactionMapper.toResponseDto(savedTransaction);
    }


    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ChequeTransactionResponseDto processChequeDecaissementTransaction(ChequeTransactionRequest requestDto)  {
            // Step 1: Fetch the Chèque entity from the database using the Chèque number from the request.
            // If the Chèque does not exist, throw a ResourceNotFoundException.
            Cheque cheque = chequeRepository.findById(requestDto.getIdCheque())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Cheque with id " + requestDto.getIdCheque() + " not found"));
    
            // Step 2: Check if the chèque is paid.
            // If the Chèque is already paid, throw an exception.
            if (cheque.getStatut() != StatutMoyenPaiement.ENCOURS) {
                throw new RuntimeException("The cheque is not active, it has already been paid");
            }
    
            // Step 3: Use the mapper to convert the DTO and the fetched Chèque entity into a Transaction entity.
            Transaction transactionEntity = chequeTransactionMapper.toEntity(requestDto, cheque);
    
            // Step 4: Attacher au rapprochement  puis sauvegarder.
            Banque banque = cheque.getBanque();
            Transaction savedTransaction = this.saveTransactionWithCascade(transactionEntity, banque.getId(), requestDto.getRapprochementId());


    
            // Step 6: Mark the Chèque as  (paid) and save the updated Chèque entity.
            cheque.setStatut(StatutMoyenPaiement.REGLE);
            cheque.setDateOperation(savedTransaction.getDateOperations());
            cheque.setDateValeur(savedTransaction.getDateValeur());
            cheque.setLibelle(savedTransaction.getLibelle());
            cheque.setObservation(savedTransaction.getObservation());
            chequeRepository.save(cheque);
    
            // Step 7: Convert the saved Transaction entity into a ChequeTransactionResponseDto
            // using the mapper and return it as the response.
            return chequeTransactionMapper.toResponseDto(savedTransaction);
        }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ChequeTransactionResponseDto processChequeEncaissementTransaction(ChequeTransactionRequest requestDto)  {
        // Step 1: Fetch the Chèque entity from the database using the Chèque number from the request.
        // If the Chèque does not exist, throw a ResourceNotFoundException.
        Cheque cheque = chequeRepository.findById(requestDto.getIdCheque())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cheque with id " + requestDto.getIdCheque() + " not found"));

        // Step 2: Check if the chèque is paid.
        // If the Chèque is already paid , throw an exception.
        if (cheque.getStatut() != StatutMoyenPaiement.ENCOURS) {
            throw new RuntimeException("The cheque has already been paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched Chèque entity into a Transaction entity.
        Transaction transactionEntity = chequeTransactionMapper.toEntity(requestDto, cheque);

        Banque banque = cheque.getBanque();

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = this.saveTransactionWithCascade(transactionEntity, banque.getId(), requestDto.getRapprochementId());


        // Step 6: Mark the Chèque as inactive (paid) and save the updated Chèque entity.
        cheque.setStatut(StatutMoyenPaiement.REGLE);
        cheque.setDateOperation(savedTransaction.getDateOperations());
        cheque.setDateValeur(savedTransaction.getDateValeur());
        cheque.setLibelle(savedTransaction.getLibelle());
        cheque.setObservation(savedTransaction.getObservation());
        chequeRepository.save(cheque);

        // Step 7: Convert the saved Transaction entity into a ChequeTransactionResponseDto
        // using the mapper and return it as the response.
        return chequeTransactionMapper.toResponseDto(savedTransaction);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LcnTransactionResponse processDecaissementLcnTransaction(LcnTransactionRequest requestDto)  {
        // Step 1: Fetch the Lcn entity from the database using the Lcn number from the request.
        // If the Lcn does not exist, throw a ResourceNotFoundException.
        Lcn lcn = lcnRepository.findById(requestDto.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lcn with id " + requestDto.getId() + " not found"));

        // Step 2: Check if the Lcn is paid.
        // If the Lcn is already paid , throw an exception.
        if (lcn.getStatut() != StatutMoyenPaiement.ENCOURS) {
            throw new RuntimeException("The lcn  has already been paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched lcn entity into a Transaction entity.
        Transaction transactionEntity = lcnTransactionMapper.toEntity(requestDto, lcn);

        Banque banque = lcn.getBanque();

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = this.saveTransactionWithCascade(transactionEntity, banque.getId(), requestDto.getRapprochementId());


        // Step 6: Mark the lcn as inactive (paid) and save the updated lcn entity.
        lcn.setStatut(StatutMoyenPaiement.REGLE);
        lcn.setDateOperation(savedTransaction.getDateOperations());
        lcn.setDateValeur(savedTransaction.getDateValeur());
        lcn.setLibelle(savedTransaction.getLibelle());
        lcn.setObservation(savedTransaction.getObservation());
        lcnRepository.save(lcn);

        // Step 7: Convert the saved Transaction entity into a LcnTransactionResponse
        // using the mapper and return it as the response.
        return lcnTransactionMapper.toResponseDto(savedTransaction);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LcnTransactionResponse processEncaissementLcnTransaction(LcnTransactionRequest requestDto)  {
        // Step 1: Fetch the Lcn entity from the database using the Lcn number from the request.
        // If the Lcn does not exist, throw a ResourceNotFoundException.
        Lcn lcn = lcnRepository.findById(requestDto.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lcn with id " + requestDto.getId() + " not found"));

        // Step 2: Check if the Lcn is paid.
        // If the Lcn is already paid , throw an exception.
        if (lcn.getStatut() != StatutMoyenPaiement.ENCOURS) {
            throw new RuntimeException("The lcn  has already been paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched lcn entity into a Transaction entity.
        Transaction transactionEntity = lcnTransactionMapper.toEntity(requestDto, lcn);
        Banque banque = lcn.getBanque();

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = this.saveTransactionWithCascade(transactionEntity, banque.getId(), requestDto.getRapprochementId());

        // Step 6: Mark the lcn as inactive (paid) and save the updated lcn entity.
        lcn.setStatut(StatutMoyenPaiement.REGLE);
        lcn.setDateOperation(savedTransaction.getDateOperations());
        lcn.setDateValeur(savedTransaction.getDateValeur());
        lcn.setLibelle(savedTransaction.getLibelle());
        lcn.setObservation(savedTransaction.getObservation());
        lcnRepository.save(lcn);

        // Step 7: Convert the saved Transaction entity into a LcnTransactionResponse
        // using the mapper and return it as the response.
        return lcnTransactionMapper.toResponseDto(savedTransaction);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public VirementTransactionResponseDto processVirementDecaissementTransaction(VirementTransactionRequestDto requestDto) {
        // Step 1: Fetch virement entity from the database using it's number from the request.
        // If the virement does not exist, throw a ResourceNotFoundException.
        Virement virement = virementRepository.findById(requestDto.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "virement with id " + requestDto.getId() + " not found"));

        // Step 2: Check if the virement is paid.
        // If the virement is already paid  throw an exception.
        if (virement.getStatut() != StatutMoyenPaiement.ENCOURS) {
            throw new RuntimeException("The virement has already been paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched virement entity into a Transaction entity.
        Transaction transactionEntity = virementTransactionMapper.toEntity(requestDto, virement);

        Banque banque = virement.getBanqueEmettrice();

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = this.saveTransactionWithCascade(transactionEntity, banque.getId(), requestDto.getRapprochementId());


        // Step 6: Mark the virement as inactive (paid) and save the updated virement entity.
        virement.setStatut(StatutMoyenPaiement.REGLE);
        virement.setDateOperation(savedTransaction.getDateOperations());
        virement.setDateValeur(savedTransaction.getDateValeur());
        virement.setLibelle(savedTransaction.getLibelle());
        virement.setObservation(savedTransaction.getObservation());
        virementRepository.save(virement);

        // Step 7: Convert the saved Transaction entity into a VirementTransactionResponseDto
        // using the mapper and return it as the response.
        return virementTransactionMapper.toResponseDto(savedTransaction);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public VirementTransactionResponseDto processVirementEncaissementTransaction(VirementTransactionRequestDto requestDto) {
        // Step 1: Fetch virement entity from the database using it's number from the request.
        // If the virement does not exist, throw a ResourceNotFoundException.
        Virement virement = virementRepository.findById(requestDto.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "virement with id " + requestDto.getId() + " not found"));

        // Step 2: Check if the virement is active.
        // If the virement is already used/paid (active = false), throw an exception.
        if (virement.getStatut() != StatutMoyenPaiement.ENCOURS) {
            throw new RuntimeException("The virement  has already been paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched virement entity into a Transaction entity.
        Transaction transactionEntity = virementTransactionMapper.toEntity(requestDto, virement);

        Banque banque = virement.getBanqueBeneficiare();

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = this.saveTransactionWithCascade(transactionEntity, banque.getId(), requestDto.getRapprochementId());



        // Step 6: Mark the virement as inactive (paid) and save the updated virement entity.
        virement.setStatut(StatutMoyenPaiement.REGLE);
        virement.setDateOperation(savedTransaction.getDateOperations());
        virement.setDateValeur(savedTransaction.getDateValeur());
        virement.setLibelle(savedTransaction.getLibelle());
        virement.setObservation(savedTransaction.getObservation());
        virementRepository.save(virement);

        // Step 7: Convert the saved Transaction entity into a VirementTransactionResponseDto
        // using the mapper and return it as the response.
        return virementTransactionMapper.toResponseDto(savedTransaction);
    }


    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MiseAdispositionTransactionResponseDto processMiseAdispositionDecaissementTransaction(MiseAdispositionTransactionRequestDto requestDto)  {
        // Step 1: Fetch the MiseAdispo entity from the database.
        // If the MiseAdispo does not exist, throw a ResourceNotFoundException.
        MiseADisposition miseADisposition = miseADispositionRepository.findById(requestDto.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Mise à disposition  with id " + requestDto.getId() + " not found"));

        // Step 2: Check if the MiseAdispo is paid.
        // If the MiseAdispo is already paid  throw an exception.
        if (miseADisposition.getStatut() != StatutMoyenPaiement.ENCOURS) {
            throw new RuntimeException("The miseADisposition  has already been paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched miseADisposition entity into a Transaction entity.
        Transaction transactionEntity = miseAdispositionTransactionMapper.toEntity(requestDto, miseADisposition);

        Banque banque = miseADisposition.getBanque();

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = this.saveTransactionWithCascade(transactionEntity, banque.getId(), requestDto.getRapprochementId());



        // Step 6: Mark the miseADisposition as inactive (paid) and save the updated miseADisposition entity.
        miseADisposition.setStatut(StatutMoyenPaiement.REGLE);
        miseADisposition.setDateOperation(savedTransaction.getDateOperations());
        miseADisposition.setDateValeur(savedTransaction.getDateValeur());
        miseADisposition.setLibelle(savedTransaction.getLibelle());
        miseADisposition.setObservation(savedTransaction.getObservation());
        miseADispositionRepository.save(miseADisposition);

        // Step 7: Convert the saved Transaction entity into a MiseAdispositionTransactionResponseDto
        // using the mapper and return it as the response.
        return miseAdispositionTransactionMapper.toResponseDto(savedTransaction);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MiseAdispositionTransactionResponseDto processMiseAdispositionEncaissementTransaction(MiseAdispositionTransactionRequestDto requestDto)  {
        // Step 1: Fetch the MiseAdispo entity from the database.
        // If the MiseAdispo does not exist, throw a ResourceNotFoundException.
        MiseADisposition miseADisposition = miseADispositionRepository.findById(requestDto.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Mise à disposition  with id " + requestDto.getId() + " not found"));

        // Step 2: Check if the MiseAdispo is paid.
        // If the MiseAdispo is already paid , throw an exception.
        if (miseADisposition.getStatut() != StatutMoyenPaiement.ENCOURS) {
            throw new RuntimeException("The miseADisposition  has already been paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched miseADisposition entity into a Transaction entity.
        Transaction transactionEntity = miseAdispositionTransactionMapper.toEntity(requestDto, miseADisposition);

        Banque banque = miseADisposition.getBanque();

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = this.saveTransactionWithCascade(transactionEntity, banque.getId(), requestDto.getRapprochementId());



        // Step 6: Mark the miseADisposition as inactive (paid) and save the updated miseADisposition entity.
        miseADisposition.setStatut(StatutMoyenPaiement.REGLE);
        miseADisposition.setDateOperation(savedTransaction.getDateOperations());
        miseADisposition.setDateValeur(savedTransaction.getDateValeur());
        miseADisposition.setLibelle(savedTransaction.getLibelle());
        miseADisposition.setObservation(savedTransaction.getObservation());
        miseADispositionRepository.save(miseADisposition);

        // Step 7: Convert the saved Transaction entity into a MiseAdispositionTransactionResponseDto
        // using the mapper and return it as the response.
        return miseAdispositionTransactionMapper.toResponseDto(savedTransaction);
    }


    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PrefinancementTransactionResponse processPrefinancementTransaction(PrefinancementTransactionRequest requestDto)  {
        // Step 1: Fetch prefi  from the database using it's id from the request.
        Prefinancement prefinancement=this.prefinancementRepository.findById(requestDto.getPrefinancementId())
                .orElseThrow(()->new ResourceNotFoundException("Prefinancement with id "+requestDto.getPrefinancementId()+" not found"));

        // Step 2: Check if the prefi is not already paid.
        // If it's already paid , throw an exception.
        if (prefinancement.getStatus()== Status.PAYE) {
            throw new RuntimeException(" préfinancement is already paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched prefi entity into a Transaction entity.
        Transaction transactionEntity = prefinancementTransactionMapper.toEntity(requestDto);

        Banque banque = banqueRepository.findById(requestDto.getBanqueId()).orElseThrow(()->new ResourceNotFoundException("Banque with id "+requestDto.getBanqueId()+" not found"));

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = this.saveTransactionWithCascade(transactionEntity, banque.getId(), requestDto.getRapprochementId());



        this.banqueRepository.save(banque);

        // Step 6: Mark the prefi as * (paid) and save the updated prefi entity.
        prefinancement.setStatus(Status.PAYE);
        prefinancement.setDateOperation(savedTransaction.getDateOperations());
        prefinancement.setDateValeur(savedTransaction.getDateValeur());
        prefinancement.setObservation(savedTransaction.getObservation());
        prefinancement.setLibelle(savedTransaction.getLibelle());
        prefinancementRepository.save(prefinancement);

        // Step 7: Convert the saved Transaction entity into a PrefinancementTransactionResponse
        // using the mapper and return it as the response.
        return prefinancementTransactionMapper.toResponseDto(savedTransaction);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DroitConstateTransactionResponse processDroitConstateTransaction(DroitConstateTransactionRequest requestDto) {
        // Step 1: Fetch dc  from the database using it's id from the request.
        DroitConstate droitConstate=this.droitConstateRepository.findById(requestDto.getDroitConstateId())
                .orElseThrow(()->new ResourceNotFoundException("Droit constate with id "+requestDto.getDroitConstateId()+" not found"));

        // Step 2: Check if the dc is not already paid.
        // If it's already paid , throw an exception.
        if (droitConstate.getStatus()== Status.PAYE) {
            throw new RuntimeException(" droit constate is already paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched dc entity into a Transaction entity.
        Transaction transactionEntity = droitConstateTransactionMapper.toEntity(requestDto);

        Banque banque = banqueRepository.findById(requestDto.getBanqueId()).orElseThrow(()->new ResourceNotFoundException("Banque with id "+requestDto.getBanqueId()+" not found"));

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = this.saveTransactionWithCascade(transactionEntity, banque.getId(), requestDto.getRapprochementId());


        // Step 6: Mark the prefi as * (paid) and save the updated prefi entity.
        droitConstate.setStatus(Status.PAYE);
        droitConstate.setDateOperation(savedTransaction.getDateOperations());
        droitConstate.setDateValeur(savedTransaction.getDateValeur());
        droitConstate.setObservation(savedTransaction.getObservation());
        droitConstate.setLibelle(savedTransaction.getLibelle());

        droitConstateRepository.save(droitConstate);

        // Step 7: Convert the saved Transaction entity into a DroitConstateTransactionResponse
        // using the mapper and return it as the response.
        return droitConstateTransactionMapper.toResponseDto(savedTransaction);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FactoringTransactionResponse processFactoringTransaction(FactoringTransactionRequest requestDto)  {
        // Step 1: Fetch factoring  from the database using it's id from the request.
        Factoring factoring=this.factoringRepository.findById(requestDto.getFactoringId())
                .orElseThrow(()->new ResourceNotFoundException("Factoring with id "+requestDto.getFactoringId()+" not found"));

        // Step 2: Check if the factoring is not already paid.
        // If it's already paid , throw an exception.
        if (factoring.getStatus()== Status.PAYE) {
            throw new RuntimeException(" factoring is already paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched dc entity into a Transaction entity.
        Transaction transactionEntity = factoringTransactionMapper.toEntity(requestDto);

        Banque banque = banqueRepository.findById(requestDto.getBanqueId()).orElseThrow(()->new ResourceNotFoundException("Banque with id "+requestDto.getBanqueId()+" not found"));

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = this.saveTransactionWithCascade(transactionEntity, banque.getId(), requestDto.getRapprochementId());


        // Step 6: Mark the factoring as paid and save the updated factoring entity.
        factoring.setStatus(Status.PAYE);
        factoring.setDateOperation(savedTransaction.getDateOperations());
        factoring.setDateValeur(savedTransaction.getDateValeur());
        factoring.setObservation(savedTransaction.getObservation());
        factoring.setLibelle(savedTransaction.getLibelle());

        factoringRepository.save(factoring);

        // Step 7: Convert the saved Transaction entity into a FactoringTransactionResponse
        // using the mapper and return it as the response.
        return factoringTransactionMapper.toResponseDto(savedTransaction);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DecompteTransactionResponseDto processDecompteToTransaction(DecompteTransactionRequestDto requestDto)  {
        // Step 1: Fetch decompte  from the database using it's id from the request.
        Decompte decompte=this.decompteRepository.findById(requestDto.getDecompteId())
                .orElseThrow(()->new ResourceNotFoundException("Décompte with id "+requestDto.getDecompteId()+" not found"));

        // Step 2: Check if the decompte is not already paid.
        // If it's already paid , throw an exception.
        if (decompte.getStatus()== Status.PAYE) {
            throw new RuntimeException(" décompte is already paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched dc entity into a Transaction entity.
        Transaction transactionEntity = decompteTransactionMapper.toEntity(requestDto);

        Banque banque = banqueRepository.findById(requestDto.getBanqueId()).orElseThrow(()->new ResourceNotFoundException("Banque with id "+requestDto.getBanqueId()+" not found"));

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = this.saveTransactionWithCascade(transactionEntity, banque.getId(), requestDto.getRapprochementId());


        // Step 6: Mark the decompte as * (paid) and save the updated decompte entity.
        decompte.setStatus(Status.PAYE);
        decompte.setDateOperation(savedTransaction.getDateOperations());
        decompte.setDateValeur(savedTransaction.getDateValeur());
        decompte.setObservation(savedTransaction.getObservation());
        decompte.setLibelle(savedTransaction.getLibelle());
        decompteRepository.save(decompte);

        // Step 7: Convert the saved Transaction entity into a DroitConstateTransactionResponse
        // using the mapper and return it as the response.
        return decompteTransactionMapper.toResponseDto(savedTransaction);
    }

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

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AgiosTransactionResponse processAgiosToTransaction(AgiosValidationRequest requestDto)  {
        // Step 1: Fetch agios from the database using it's id from the request.
        Agios agios=this.agiosRepository.findById(requestDto.getIdAgio())
                .orElseThrow(()->new ResourceNotFoundException("Agios with id "+requestDto.getIdAgio()+" not found"));

        // Step 2: Check if the agios is not already paid.
        // If it's already paid , throw an exception.
        if (agios.getStatus()== Status.PAYE) {
            throw new RuntimeException(" agios is already paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched dc entity into a Transaction entity.
        Transaction transactionEntity = agiosTransactionMapper.toEntity(requestDto,agios);

        Banque banque = banqueRepository.findById(agios.getBanque().getId()).orElseThrow(()->new ResourceNotFoundException("Banque with id "+agios.getBanque().getId()+" not found"));

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = this.saveTransactionWithCascade(transactionEntity, banque.getId(), requestDto.getRapprochementId());


        // Step 6: Mark the agios as * (paid) and save the updated agios entity.
        agios.setStatus(Status.PAYE);
        agios.setObservation(savedTransaction.getObservation());
        agios.setLibelle(savedTransaction.getLibelle());
        agios.setDateValeur(savedTransaction.getDateValeur());
        agios.setDateOperation(savedTransaction.getDateOperations());
        agiosRepository.save(agios);

        // Step 7: Convert the saved Transaction entity into a AgiosTransactionResponse
        // using the mapper and return it as the response.
        return agiosTransactionMapper.toResponseDto(savedTransaction);
    }


    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PrelevementTransactionResponse processPrelevementToTransaction(PrelevementValidationRequest requestDto)  {
        // Step 1: Fetch prelevement from the database using it's id from the request.
        Prelevement prelevement=this.prelevementRepository.findById(requestDto.getIdPrelevement())
                .orElseThrow(()->new ResourceNotFoundException("Prelevement with id "+requestDto.getIdPrelevement()+" not found"));

        // Step 2: Check if the prelevement is not already paid.
        // If it's already paid , throw an exception.
        if (prelevement.getStatus()== Status.PAYE) {
            throw new RuntimeException(" prelevement is already paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched dc entity into a Transaction entity.
        Transaction transactionEntity = prelevementTransactionMapper.toEntity(requestDto,prelevement);

        Banque banque = banqueRepository.findById(prelevement.getBanque().getId()).orElseThrow(()->new ResourceNotFoundException("Banque with id "+prelevement.getBanque().getId()+" not found"));

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = this.saveTransactionWithCascade(transactionEntity, banque.getId(), requestDto.getRapprochementId());


        this.banqueRepository.save(banque);

        // Step 6: Mark the prelevement as * (paid) and save the updated prelevement entity.
        prelevement.setStatus(Status.PAYE);
        prelevement.setObservation(savedTransaction.getObservation());
        prelevement.setLibelle(savedTransaction.getLibelle());
        prelevement.setDateValeur(savedTransaction.getDateValeur());
        prelevement.setDateOperation(savedTransaction.getDateOperations());
        prelevementRepository.save(prelevement);

        // Step 7: Convert the saved Transaction entity into a PrelevementTransactionResponse
        // using the mapper and return it as the response.
        return prelevementTransactionMapper.toResponseDto(savedTransaction);
    }


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

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public VirementInternationalTransactionResponseDto processVirementInternationalDecaissementTransaction(
            VirementInternationalTransactionRequestDto requestDto)  {

        // Step 1: Fetch virement international entity from the database using its id from the request.
        // If the virement international does not exist, throw a ResourceNotFoundException.
        VirementInternational virementInternational = virementInternationalRepository.findById(requestDto.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Virement international with id " + requestDto.getId() + " not found"));

        // Step 2: Check if the virement international is paid.
        // If the virement international is already paid, throw an exception.
        if (virementInternational.getStatut() != StatutMoyenPaiement.ENCOURS) {
            throw new RuntimeException("The virement international  has already been paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched virement international entity into a Transaction entity.
        Transaction transactionEntity = virementInternationalTransactionMapper.toEntity(requestDto, virementInternational);
        Banque banque = virementInternational.getBanque();

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = this.saveTransactionWithCascade(transactionEntity, banque.getId(), requestDto.getRapprochementId());


        // Step 6: Mark the virement international as inactive (paid) and save the updated virement international entity.
        virementInternational.setStatut(StatutMoyenPaiement.REGLE);
        virementInternational.setDateOperation(savedTransaction.getDateOperations());
        virementInternational.setDateValeur(savedTransaction.getDateValeur());
        virementInternational.setLibelle(savedTransaction.getLibelle());
        virementInternational.setObservation(savedTransaction.getObservation());
        virementInternationalRepository.save(virementInternational);

        // Step 7: Convert the saved Transaction entity into a VirementInternationalTransactionResponseDto
        // using the mapper and return it as the response.
        return virementInternationalTransactionMapper.toResponseDto(savedTransaction);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public VirementInternationalTransactionResponseDto processVirementInternationalEncaissementTransaction(
            VirementInternationalTransactionRequestDto requestDto) {

        // Step 1: Fetch virement international entity from the database using its id from the request.
        // If the virement international does not exist, throw a ResourceNotFoundException.
        VirementInternational virementInternational = virementInternationalRepository.findById(requestDto.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Virement international with id " + requestDto.getId() + " not found"));

        // Step 2: Check if the virement international is paid.
        // If the virement international is already paid , throw an exception.
        if (virementInternational.getStatut() != StatutMoyenPaiement.ENCOURS) {
            throw new RuntimeException("The virement international  has already been paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched virement international entity into a Transaction entity.
        Transaction transactionEntity = virementInternationalTransactionMapper.toEntity(requestDto, virementInternational);

        Banque banque = virementInternational.getBanque();

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = this.saveTransactionWithCascade(transactionEntity, banque.getId(), requestDto.getRapprochementId());


        // Step 6: Mark the virement international as inactive (paid) and save the updated virement international entity.
        virementInternational.setStatut(StatutMoyenPaiement.REGLE);
        virementInternational.setDateOperation(savedTransaction.getDateOperations());
        virementInternational.setDateValeur(savedTransaction.getDateValeur());
        virementInternational.setLibelle(savedTransaction.getLibelle());
        virementInternational.setObservation(savedTransaction.getObservation());
        virementInternationalRepository.save(virementInternational);

        // Step 7: Convert the saved Transaction entity into a VirementInternationalTransactionResponseDto
        // using the mapper and return it as the response.
        return virementInternationalTransactionMapper.toResponseDto(savedTransaction);
    }

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
