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
import staport.finance.gBanque.interets.entities.Interet;
import staport.finance.gBanque.interets.repository.InteretRepository;
import staport.finance.gBanque.operationErrone.entities.OperationErrone;
import staport.finance.gBanque.operationErrone.repositories.OperationErroneRepository;
import staport.finance.gBanque.prelevements.entitie.Prelevement;
import staport.finance.gBanque.prelevements.repository.PrelevementRepository;
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
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.lcn.repositories.LcnRepository;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.contratLeasing.repositories.ContratLeasingRepository;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.repositories.EcheanceLeasingRepository;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.repositories.MiseADispositionRepository;
import staport.finance.gBanque.validation.dtos.factoring.FactoringTransactionRequest;
import staport.finance.gBanque.validation.dtos.factoring.FactoringTransactionResponse;
import staport.finance.gBanque.validation.dtos.fraisBancaire.FraisBancaireTransactionResponse;
import staport.finance.gBanque.validation.dtos.fraisBancaire.FraisBanciareValidationRequest;
import staport.finance.gBanque.validation.dtos.impotsEtTaxes.ImpotEtTaxeTransactionResponse;
import staport.finance.gBanque.validation.dtos.impotsEtTaxes.ImpotEtTaxeValidationRequest;
import staport.finance.gBanque.validation.dtos.interet.InteretTransactionResponse;
import staport.finance.gBanque.validation.dtos.interet.InteretValidationRequest;
import staport.finance.gBanque.validation.dtos.prefinancements.PrefinancementTransactionRequest;
import staport.finance.gBanque.validation.dtos.prefinancements.PrefinancementTransactionResponse;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;
import staport.finance.gBanque.prefinancement.repository.PrefinancementRepository;
import staport.finance.gBanque.validation.dtos.cheques.ChequeTransactionRequest;
import staport.finance.gBanque.validation.dtos.cheques.ChequeTransactionResponseDto;
import staport.finance.gBanque.validation.dtos.cmt.CmtTransactionRequestDto;
import staport.finance.gBanque.validation.dtos.cmt.CmtTransactionResponseDto;
import staport.finance.gBanque.validation.dtos.lcn.LcnTransactionRequest;
import staport.finance.gBanque.validation.dtos.lcn.LcnTransactionResponse;
import staport.finance.gBanque.validation.dtos.leasing.LeasingTransactionRequestDto;
import staport.finance.gBanque.validation.dtos.leasing.LeasingTransactionResponseDto;
import staport.finance.gBanque.validation.dtos.miseAdispo.MiseAdispositionTransactionRequestDto;
import staport.finance.gBanque.validation.dtos.miseAdispo.MiseAdispositionTransactionResponseDto;
import staport.finance.gBanque.validation.dtos.prelevements.PrelevementTransactionResponse;
import staport.finance.gBanque.validation.dtos.prelevements.PrelevementValidationRequest;
import staport.finance.gBanque.validation.dtos.restitutions.RestitutionTransactionResponse;
import staport.finance.gBanque.validation.dtos.restitutions.RestitutionValidationRequest;
import staport.finance.gBanque.validation.dtos.operationErrone.OperationErroneTransactionResponse;
import staport.finance.gBanque.validation.dtos.operationErrone.OperationErroneValidationRequest;
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

    private final LeasingTransactionMapper leasingTransactionMapper;
    private final ContratLeasingRepository contratLeasingRepository;
    private final EcheanceLeasingRepository echeanceLeasingRepository;

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

    private final InteretRepository interetRepository;
    private final InteretTransactionMapper interetTransactionMapper;

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


    public ValidationService(LeasingTransactionMapper mapper, CmtTransactionMapper cmtMapper, TransactionRepository transactionRepository, EcheanceLeasingRepository echeanceLeasingRepository, ContratLeasingRepository contratLeasingRepository, BanqueRepository banqueRepository, CreditMoyenTermeRepository creditMoyenTermeRepository, EcheanceCMTRepository echeanceCMTRepository, ChequeRepository chequeRepository, ChequeTransactionMapper chequeTransactionMapper, LcnRepository lcnRepository, LcnTransactionMapper lcnTransactionMapper, VirementTransactionMapper virementTransactionMapper, VirementRepository virementRepository, MiseADispositionRepository miseADispositionRepository, MiseAdispositionTransactionMapper miseAdispositionTransactionMapper, PrefinancementRepository prefinancementRepository, PrefinancementTransactionMapper prefinancementTransactionMapper, DroitConstateRepository droitConstateRepository, DroitConstateTransactionMapper droitConstateTransactionMapper, DecompteRepository decompteRepository, DecompteTransactionMapper decompteTransactionMapper, CommisionRepository commisionRepository, CommisionTransactionMapper commisionTransactionMapper, AgiosTransactionMapper agiosTransactionMapper, AgiosRepository agiosRepository, PrelevementRepository prelevementRepository, PrelevementTransactionMapper prelevementTransactionMapper, InteretRepository interetRepository, InteretTransactionMapper interetTransactionMapper, FraisBancaireRepository fraisBancaireRepository, FraisBancaireTransactionMapper fraisBancaireTransactionMapper, FactoringRepository factoringRepository, FactoringTransactionMapper factoringTransactionMapper, ImpotEtTaxeRepository impotEtTaxeRepository, ImpotEtTaxeTransactionMapper impotEtTaxeTransactionMapper, RestitutionRepository restitutionRepository, RestitutionTransactionMapper restitutionTransactionMapper, OperationErroneRepository operationErroneRepository, OperationErroneTransactionMapper operationErroneTransactionMapper) {
        this.leasingTransactionMapper = mapper;
        this.cmtTransactionMapper = cmtMapper;
        this.transactionRepository = transactionRepository;
        this.echeanceLeasingRepository = echeanceLeasingRepository;
        this.contratLeasingRepository = contratLeasingRepository;
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
        this.interetRepository = interetRepository;
        this.interetTransactionMapper = interetTransactionMapper;
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
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public LeasingTransactionResponseDto processLeasingTransaction(LeasingTransactionRequestDto requestDto) throws Exception {

        // Étape 1: Récupérer les entités liées avant le mappage
        ContratLeasing contratLeasing = contratLeasingRepository.findById(requestDto.getContratLeasingId())
                .orElseThrow(() -> new ResourceNotFoundException("Contrat de leasing introuvable avec l'ID: " + requestDto.getContratLeasingId()));

        EcheanceLeasing echeanceLeasing = echeanceLeasingRepository.findByIdAndContratLeasingId(requestDto.getEcheanceId(), requestDto.getContratLeasingId())
                .orElseThrow(() -> new ResourceNotFoundException("Echeance de leasing introuvable avec l'ID: " + requestDto.getEcheanceId() + " et contrat id " + requestDto.getContratLeasingId()));

        if( echeanceLeasing.getStatutPaiement() == StatutEcheanceLeasing.PAYE){
            throw new Exception("l echéance est deja payee");
        }
        // Étape 2: Utiliser le mapper pour convertir le DTO et les entités en une nouvelle entité Transaction.
        Transaction transactionEntity = leasingTransactionMapper.toEntity(requestDto, contratLeasing, echeanceLeasing);

        // Étape 3: Sauvegarder la nouvelle transaction.
        Transaction savedTransaction = transactionRepository.save(transactionEntity);

        // Étape 4: Mettre à jour le solde de la banque.
        Banque banque = contratLeasing.getBanqueDePrelevement();
        banque.setSoldeActuel(banque.getSoldeActuel().subtract(echeanceLeasing.getMontantTTC()));
        this.banqueRepository.save(banque);


        // Étape 5: Marquer l'échéance comme payée.
        echeanceLeasing.setStatutPaiement(StatutEcheanceLeasing.PAYE);
        echeanceLeasing.setDateValeur(savedTransaction.getDateValeur());
        echeanceLeasing.setDateOperation(savedTransaction.getDateOperations());
        echeanceLeasing.setLibelle(savedTransaction.getLibelle());
        echeanceLeasing.setObservation(savedTransaction.getObservation());
        echeanceLeasingRepository.save(echeanceLeasing);

        // Étape 6: Utiliser le mapper pour convertir l'entité sauvegardée en DTO de réponse.
        return leasingTransactionMapper.toResponseDto(savedTransaction);
    }


    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CmtTransactionResponseDto processCmtTransaction(CmtTransactionRequestDto requestDto) throws Exception {


        // Étape 1: Récupérer les entités liées avant le mappage
        CreditMoyenTerme creditMoyenTerme = creditMoyenTermeRepository.findById(requestDto.getCmtId())
                .orElseThrow(() -> new ResourceNotFoundException("Credit Moyen Terme introuvable avec l'ID: " + requestDto.getCmtId()));

        EcheanceCMT echeanceCMT = echeanceCMTRepository.findByIdAndCreditMoyenTermeId(requestDto.getEcheanceId(), requestDto.getCmtId())
                .orElseThrow(() -> new ResourceNotFoundException("Echeance cmt introuvable avec l'ID: " + requestDto.getEcheanceId() + " et credit id " + requestDto.getCmtId()));

        if( echeanceCMT.getStatutPaiement() == StatutEcheanceCMT.PAYE){
            throw new Exception("l echéance est deja payee");
        }

        // Étape 2: Utiliser le mapper pour convertir le DTO et les entités en une nouvelle entité Transaction.
        Transaction transactionEntity = cmtTransactionMapper.toEntity(requestDto, creditMoyenTerme, echeanceCMT);

        // Étape 3: Sauvegarder la nouvelle transaction.
        Transaction savedTransaction = transactionRepository.save(transactionEntity);

        // Étape 4: Mettre à jour le solde de la banque.
        Banque banque = creditMoyenTerme.getBanque();
        banque.setSoldeActuel(banque.getSoldeActuel().subtract(echeanceCMT.getMontant()));
        this.banqueRepository.save(banque);


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


    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ChequeTransactionResponseDto processChequeDecaissementTransaction(ChequeTransactionRequest requestDto) throws Exception{
            // Step 1: Fetch the Chèque entity from the database using the Chèque number from the request.
            // If the Chèque does not exist, throw a ResourceNotFoundException.
            Cheque cheque = chequeRepository.findById(requestDto.getIdCheque())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Cheque with id " + requestDto.getIdCheque() + " not found"));
    
            // Step 2: Check if the chèque is active.
            // If the Chèque is already used/paid (active = false), throw an exception.
            if (!cheque.isActive()) {
                throw new Exception("The cheque is not active, it has already been paid");
            }
    
            // Step 3: Use the mapper to convert the DTO and the fetched Chèque entity into a Transaction entity.
            Transaction transactionEntity = chequeTransactionMapper.toEntity(requestDto, cheque);
    
            // Step 4: Save the Transaction entity to the database.
            Transaction savedTransaction = transactionRepository.save(transactionEntity);
    
            // Step 5: Update the bank's current balance by subtracting the transaction debit amount.
            // Then save the updated bank entity.
            Banque banque = cheque.getBanque();
            banque.setSoldeActuel(banque.getSoldeActuel().subtract(cheque.getMontant()));
            this.banqueRepository.save(banque);
    
            // Step 6: Mark the Chèque as inactive (paid) and save the updated Chèque entity.
            cheque.setActive(false);
            cheque.setDateOperation(savedTransaction.getDateOperations());
            cheque.setDateValeur(savedTransaction.getDateValeur());
            cheque.setLibelle(savedTransaction.getLibelle());
            cheque.setObservation(savedTransaction.getObservation());
            chequeRepository.save(cheque);
    
            // Step 7: Convert the saved Transaction entity into a ChequeTransactionResponseDto
            // using the mapper and return it as the response.
            return chequeTransactionMapper.toResponseDto(savedTransaction);
        }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ChequeTransactionResponseDto processChequeEncaissementTransaction(ChequeTransactionRequest requestDto) throws Exception{
        // Step 1: Fetch the Chèque entity from the database using the Chèque number from the request.
        // If the Chèque does not exist, throw a ResourceNotFoundException.
        Cheque cheque = chequeRepository.findById(requestDto.getIdCheque())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cheque with id " + requestDto.getIdCheque() + " not found"));

        // Step 2: Check if the chèque is active.
        // If the Chèque is already used/paid (active = false), throw an exception.
        if (!cheque.isActive()) {
            throw new Exception("The cheque is not active, it has already been paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched Chèque entity into a Transaction entity.
        Transaction transactionEntity = chequeTransactionMapper.toEntity(requestDto, cheque);

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = transactionRepository.save(transactionEntity);

        // Step 5: Update the bank's current balance by subtracting the transaction debit amount.
        // Then save the updated bank entity.
        Banque banque = cheque.getBanque();
        banque.setSoldeActuel(banque.getSoldeActuel().add(cheque.getMontant()));
        this.banqueRepository.save(banque);

        // Step 6: Mark the Chèque as inactive (paid) and save the updated Chèque entity.
        cheque.setActive(false);
        cheque.setDateOperation(savedTransaction.getDateOperations());
        cheque.setDateValeur(savedTransaction.getDateValeur());
        cheque.setLibelle(savedTransaction.getLibelle());
        cheque.setObservation(savedTransaction.getObservation());
        chequeRepository.save(cheque);

        // Step 7: Convert the saved Transaction entity into a ChequeTransactionResponseDto
        // using the mapper and return it as the response.
        return chequeTransactionMapper.toResponseDto(savedTransaction);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public LcnTransactionResponse processDecaissementLcnTransaction(LcnTransactionRequest requestDto) throws Exception{
        // Step 1: Fetch the Lcn entity from the database using the Lcn number from the request.
        // If the Lcn does not exist, throw a ResourceNotFoundException.
        Lcn lcn = lcnRepository.findById(requestDto.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lcn with id " + requestDto.getId() + " not found"));

        // Step 2: Check if the Lcn is active.
        // If the Lcn is already used/paid (active = false), throw an exception.
        if (!lcn.isActive()) {
            throw new Exception("The lcn is not active, it has already been paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched lcn entity into a Transaction entity.
        Transaction transactionEntity = lcnTransactionMapper.toEntity(requestDto, lcn);

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = transactionRepository.save(transactionEntity);

        // Step 5: Update the bank's current balance by subtracting the transaction debit amount.
        // Then save the updated bank entity.
        Banque banque = lcn.getBanque();
        banque.setSoldeActuel(banque.getSoldeActuel().subtract(lcn.getMontant()));
        this.banqueRepository.save(banque);

        // Step 6: Mark the lcn as inactive (paid) and save the updated lcn entity.
        lcn.setActive(false);
        lcn.setDateOperation(savedTransaction.getDateOperations());
        lcn.setDateValeur(savedTransaction.getDateValeur());
        lcn.setLibelle(savedTransaction.getLibelle());
        lcn.setObservation(savedTransaction.getObservation());
        lcnRepository.save(lcn);

        // Step 7: Convert the saved Transaction entity into a LcnTransactionResponse
        // using the mapper and return it as the response.
        return lcnTransactionMapper.toResponseDto(savedTransaction);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public LcnTransactionResponse processEncaissementLcnTransaction(LcnTransactionRequest requestDto) throws Exception{
        // Step 1: Fetch the Lcn entity from the database using the Lcn number from the request.
        // If the Lcn does not exist, throw a ResourceNotFoundException.
        Lcn lcn = lcnRepository.findById(requestDto.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lcn with id " + requestDto.getId() + " not found"));

        // Step 2: Check if the Lcn is active.
        // If the Lcn is already used/paid (active = false), throw an exception.
        if (!lcn.isActive()) {
            throw new Exception("The lcn is not active, it has already been paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched lcn entity into a Transaction entity.
        Transaction transactionEntity = lcnTransactionMapper.toEntity(requestDto, lcn);

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = transactionRepository.save(transactionEntity);

        // Step 5: Update the bank's current balance by subtracting the transaction debit amount.
        // Then save the updated bank entity.
        Banque banque = lcn.getBanque();
        banque.setSoldeActuel(banque.getSoldeActuel().add(lcn.getMontant()));
        this.banqueRepository.save(banque);


        // Step 6: Mark the lcn as inactive (paid) and save the updated lcn entity.
        lcn.setActive(false);
        lcn.setDateOperation(savedTransaction.getDateOperations());
        lcn.setDateValeur(savedTransaction.getDateValeur());
        lcn.setLibelle(savedTransaction.getLibelle());
        lcn.setObservation(savedTransaction.getObservation());
        lcnRepository.save(lcn);

        // Step 7: Convert the saved Transaction entity into a LcnTransactionResponse
        // using the mapper and return it as the response.
        return lcnTransactionMapper.toResponseDto(savedTransaction);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public VirementTransactionResponseDto processVirementDecaissementTransaction(VirementTransactionRequestDto requestDto) throws Exception{
        // Step 1: Fetch virement entity from the database using it's number from the request.
        // If the virement does not exist, throw a ResourceNotFoundException.
        Virement virement = virementRepository.findById(requestDto.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "virement with id " + requestDto.getId() + " not found"));

        // Step 2: Check if the virement is active.
        // If the virement is already used/paid (active = false), throw an exception.
        if (!virement.isActive()) {
            throw new Exception("The virement is not active, it has already been paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched virement entity into a Transaction entity.
        Transaction transactionEntity = virementTransactionMapper.toEntity(requestDto, virement);

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = transactionRepository.save(transactionEntity);

        // Step 5: Update the bank's current balance by subtracting the transaction debit amount.
        // Then save the updated bank entity.
        Banque banque = virement.getBanqueEmettrice();
        banque.setSoldeActuel(banque.getSoldeActuel().subtract(virement.getMontant()));
        this.banqueRepository.save(banque);

        // Step 6: Mark the virement as inactive (paid) and save the updated virement entity.
        virement.setActive(false);
        virement.setDateOperation(savedTransaction.getDateOperations());
        virement.setDateValeur(savedTransaction.getDateValeur());
        virement.setLibelle(savedTransaction.getLibelle());
        virement.setObservation(savedTransaction.getObservation());
        virementRepository.save(virement);

        // Step 7: Convert the saved Transaction entity into a VirementTransactionResponseDto
        // using the mapper and return it as the response.
        return virementTransactionMapper.toResponseDto(savedTransaction);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public VirementTransactionResponseDto processVirementEncaissementTransaction(VirementTransactionRequestDto requestDto) throws Exception{
        // Step 1: Fetch virement entity from the database using it's number from the request.
        // If the virement does not exist, throw a ResourceNotFoundException.
        Virement virement = virementRepository.findById(requestDto.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "virement with id " + requestDto.getId() + " not found"));

        // Step 2: Check if the virement is active.
        // If the virement is already used/paid (active = false), throw an exception.
        if (!virement.isActive()) {
            throw new Exception("The virement is not active, it has already been paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched virement entity into a Transaction entity.
        Transaction transactionEntity = virementTransactionMapper.toEntity(requestDto, virement);

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = transactionRepository.save(transactionEntity);

        // Step 5: Update the bank's current balance by subtracting the transaction debit amount.
        // Then save the updated bank entity.
        Banque banque = virement.getBanqueBeneficiare();

        banque.setSoldeActuel(banque.getSoldeActuel().add(virement.getMontant()));
        this.banqueRepository.save(banque);

        // Step 6: Mark the virement as inactive (paid) and save the updated virement entity.
        virement.setActive(false);
        virement.setDateOperation(savedTransaction.getDateOperations());
        virement.setDateValeur(savedTransaction.getDateValeur());
        virement.setLibelle(savedTransaction.getLibelle());
        virement.setObservation(savedTransaction.getObservation());
        virementRepository.save(virement);

        // Step 7: Convert the saved Transaction entity into a VirementTransactionResponseDto
        // using the mapper and return it as the response.
        return virementTransactionMapper.toResponseDto(savedTransaction);
    }


    @Transactional(isolation = Isolation.SERIALIZABLE)
    public MiseAdispositionTransactionResponseDto processMiseAdispositionDecaissementTransaction(MiseAdispositionTransactionRequestDto requestDto) throws Exception{
        // Step 1: Fetch the MiseAdispo entity from the database.
        // If the MiseAdispo does not exist, throw a ResourceNotFoundException.
        MiseADisposition miseADisposition = miseADispositionRepository.findById(requestDto.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Mise à disposition  with id " + requestDto.getId() + " not found"));

        // Step 2: Check if the MiseAdispo is active.
        // If the MiseAdispo is already used/paid (active = false), throw an exception.
        if (!miseADisposition.isActive()) {
            throw new Exception("The miseADisposition is not active, it has already been paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched miseADisposition entity into a Transaction entity.
        Transaction transactionEntity = miseAdispositionTransactionMapper.toEntity(requestDto, miseADisposition);

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = transactionRepository.save(transactionEntity);

        // Step 5: Update the bank's current balance by subtracting the transaction debit amount.
        // Then save the updated bank entity.
        Banque banque = miseADisposition.getBanque();
        banque.setSoldeActuel(banque.getSoldeActuel().subtract(miseADisposition.getMontant()));
        this.banqueRepository.save(banque);

        // Step 6: Mark the miseADisposition as inactive (paid) and save the updated miseADisposition entity.
        miseADisposition.setActive(false);
        miseADisposition.setDateOperation(savedTransaction.getDateOperations());
        miseADisposition.setDateValeur(savedTransaction.getDateValeur());
        miseADisposition.setLibelle(savedTransaction.getLibelle());
        miseADisposition.setObservation(savedTransaction.getObservation());
        miseADispositionRepository.save(miseADisposition);

        // Step 7: Convert the saved Transaction entity into a MiseAdispositionTransactionResponseDto
        // using the mapper and return it as the response.
        return miseAdispositionTransactionMapper.toResponseDto(savedTransaction);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public MiseAdispositionTransactionResponseDto processMiseAdispositionEncaissementTransaction(MiseAdispositionTransactionRequestDto requestDto) throws Exception{
        // Step 1: Fetch the MiseAdispo entity from the database.
        // If the MiseAdispo does not exist, throw a ResourceNotFoundException.
        MiseADisposition miseADisposition = miseADispositionRepository.findById(requestDto.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Mise à disposition  with id " + requestDto.getId() + " not found"));

        // Step 2: Check if the MiseAdispo is active.
        // If the MiseAdispo is already used/paid (active = false), throw an exception.
        if (!miseADisposition.isActive()) {
            throw new Exception("The miseADisposition is not active, it has already been paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched miseADisposition entity into a Transaction entity.
        Transaction transactionEntity = miseAdispositionTransactionMapper.toEntity(requestDto, miseADisposition);

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = transactionRepository.save(transactionEntity);

        // Step 5: Update the bank's current balance by subtracting the transaction debit amount.
        // Then save the updated bank entity.
        Banque banque = miseADisposition.getBanque();
        banque.setSoldeActuel(banque.getSoldeActuel().add(miseADisposition.getMontant()));
        this.banqueRepository.save(banque);

        // Step 6: Mark the miseADisposition as inactive (paid) and save the updated miseADisposition entity.
        miseADisposition.setActive(false);
        miseADisposition.setDateOperation(savedTransaction.getDateOperations());
        miseADisposition.setDateValeur(savedTransaction.getDateValeur());
        miseADisposition.setLibelle(savedTransaction.getLibelle());
        miseADisposition.setObservation(savedTransaction.getObservation());
        miseADispositionRepository.save(miseADisposition);

        // Step 7: Convert the saved Transaction entity into a MiseAdispositionTransactionResponseDto
        // using the mapper and return it as the response.
        return miseAdispositionTransactionMapper.toResponseDto(savedTransaction);
    }


    @Transactional(isolation = Isolation.SERIALIZABLE)
    public PrefinancementTransactionResponse processPrefinancementTransaction(PrefinancementTransactionRequest requestDto) throws Exception{
        // Step 1: Fetch prefi  from the database using it's id from the request.
        Prefinancement prefinancement=this.prefinancementRepository.findById(requestDto.getPrefinancementId())
                .orElseThrow(()->new ResourceNotFoundException("Prefinancement with id "+requestDto.getPrefinancementId()+" not found"));

        // Step 2: Check if the prefi is not already paid.
        // If it's already paid , throw an exception.
        if (prefinancement.getStatus()== Status.PAYE) {
            throw new Exception(" préfinancement is already paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched prefi entity into a Transaction entity.
        Transaction transactionEntity = prefinancementTransactionMapper.toEntity(requestDto);

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = transactionRepository.save(transactionEntity);

        // Step 5: Update the bank's current balance
        // Then save the updated bank entity.

        Banque banque = banqueRepository.findById(requestDto.getBanqueId()).orElseThrow(()->new ResourceNotFoundException("Banque with id "+requestDto.getBanqueId()+" not found"));

        if(prefinancement.getType()== Type.REMBOURSEMENT){
            banque.setSoldeActuel(banque.getSoldeActuel().subtract(prefinancement.getMontant()));
        }else{
            banque.setSoldeActuel(banque.getSoldeActuel().add(prefinancement.getMontant()));
        }

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

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public DroitConstateTransactionResponse processDroitConstateTransaction(DroitConstateTransactionRequest requestDto) throws Exception{
        // Step 1: Fetch dc  from the database using it's id from the request.
        DroitConstate droitConstate=this.droitConstateRepository.findById(requestDto.getDroitConstateId())
                .orElseThrow(()->new ResourceNotFoundException("Droit constate with id "+requestDto.getDroitConstateId()+" not found"));

        // Step 2: Check if the dc is not already paid.
        // If it's already paid , throw an exception.
        if (droitConstate.getStatus()== Status.PAYE) {
            throw new Exception(" droit constate is already paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched dc entity into a Transaction entity.
        Transaction transactionEntity = droitConstateTransactionMapper.toEntity(requestDto);

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = transactionRepository.save(transactionEntity);

        // Step 5: Update the bank's current balance
        // Then save the updated bank entity.

        Banque banque = banqueRepository.findById(requestDto.getBanqueId()).orElseThrow(()->new ResourceNotFoundException("Banque with id "+requestDto.getBanqueId()+" not found"));

        if(droitConstate.getType()== Type.REMBOURSEMENT){
            banque.setSoldeActuel(banque.getSoldeActuel().subtract(droitConstate.getMontant()));
        }else{
            banque.setSoldeActuel(banque.getSoldeActuel().add(droitConstate.getMontant()));
        }
        this.banqueRepository.save(banque);

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

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public FactoringTransactionResponse processFactoringTransaction(FactoringTransactionRequest requestDto) throws Exception{
        // Step 1: Fetch factoring  from the database using it's id from the request.
        Factoring factoring=this.factoringRepository.findById(requestDto.getFactoringId())
                .orElseThrow(()->new ResourceNotFoundException("Factoring with id "+requestDto.getFactoringId()+" not found"));

        // Step 2: Check if the factoring is not already paid.
        // If it's already paid , throw an exception.
        if (factoring.getStatus()== Status.PAYE) {
            throw new Exception(" factoring is already paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched dc entity into a Transaction entity.
        Transaction transactionEntity = factoringTransactionMapper.toEntity(requestDto);

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = transactionRepository.save(transactionEntity);

        // Step 5: Update the bank's current balance
        // Then save the updated bank entity.

        Banque banque = banqueRepository.findById(requestDto.getBanqueId()).orElseThrow(()->new ResourceNotFoundException("Banque with id "+requestDto.getBanqueId()+" not found"));

        if(factoring.getType()== Type.REMBOURSEMENT){
            banque.setSoldeActuel(banque.getSoldeActuel().subtract(factoring.getMontant()));
        }else{
            banque.setSoldeActuel(banque.getSoldeActuel().add(factoring.getMontant()));
        }
        this.banqueRepository.save(banque);

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


    @Transactional(isolation = Isolation.SERIALIZABLE)
    public DecompteTransactionResponseDto processDecompteToTransaction(DecompteTransactionRequestDto requestDto) throws Exception{
        // Step 1: Fetch decompte  from the database using it's id from the request.
        Decompte decompte=this.decompteRepository.findById(requestDto.getDecompteId())
                .orElseThrow(()->new ResourceNotFoundException("Décompte with id "+requestDto.getDecompteId()+" not found"));

        // Step 2: Check if the decompte is not already paid.
        // If it's already paid , throw an exception.
        if (decompte.getStatus()== Status.PAYE) {
            throw new Exception(" décompte is already paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched dc entity into a Transaction entity.
        Transaction transactionEntity = decompteTransactionMapper.toEntity(requestDto);

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = transactionRepository.save(transactionEntity);

        // Step 5: Update the bank's current balance
        // Then save the updated bank entity.

        Banque banque = banqueRepository.findById(requestDto.getBanqueId()).orElseThrow(()->new ResourceNotFoundException("Banque with id "+requestDto.getBanqueId()+" not found"));

        banque.setSoldeActuel(banque.getSoldeActuel().add(decompte.getMontant()));

        this.banqueRepository.save(banque);

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

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CommissionTransactionResponse processCommissionToTransaction(CommissionValidationDto requestDto) throws Exception{
        // Step 1: Fetch commission from the database using it's id from the request.
        Commission commission=this.commisionRepository.findById(requestDto.getIdCommission())
                .orElseThrow(()->new ResourceNotFoundException("Commission with id "+requestDto.getIdCommission()+" not found"));

        // Step 2: Check if the commission is not already paid.
        // If it's already paid , throw an exception.
        if (commission.getStatus()== Status.PAYE) {
            throw new Exception(" commission is already paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched dc entity into a Transaction entity.
        Transaction transactionEntity = commisionTransactionMapper.toEntity(requestDto,commission);

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = transactionRepository.save(transactionEntity);

        // Step 5: Update the bank's current balance
        // Then save the updated bank entity.

        Banque banque = banqueRepository.findById(commission.getBanque().getId()).orElseThrow(()->new ResourceNotFoundException("Banque with id "+commission.getBanque().getId()+" not found"));

        banque.setSoldeActuel(banque.getSoldeActuel().subtract(commission.getMontant()));

        this.banqueRepository.save(banque);

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

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public AgiosTransactionResponse processAgiosToTransaction(AgiosValidationRequest requestDto) throws Exception{
        // Step 1: Fetch agios from the database using it's id from the request.
        Agios agios=this.agiosRepository.findById(requestDto.getIdAgio())
                .orElseThrow(()->new ResourceNotFoundException("Agios with id "+requestDto.getIdAgio()+" not found"));

        // Step 2: Check if the agios is not already paid.
        // If it's already paid , throw an exception.
        if (agios.getStatus()== Status.PAYE) {
            throw new Exception(" agios is already paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched dc entity into a Transaction entity.
        Transaction transactionEntity = agiosTransactionMapper.toEntity(requestDto,agios);

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = transactionRepository.save(transactionEntity);

        // Step 5: Update the bank's current balance
        // Then save the updated bank entity.

        Banque banque = banqueRepository.findById(agios.getBanque().getId()).orElseThrow(()->new ResourceNotFoundException("Banque with id "+agios.getBanque().getId()+" not found"));

        banque.setSoldeActuel(banque.getSoldeActuel().subtract(agios.getMontant()));

        this.banqueRepository.save(banque);

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


    @Transactional(isolation = Isolation.SERIALIZABLE)
    public PrelevementTransactionResponse processPrelevementToTransaction(PrelevementValidationRequest requestDto) throws Exception{
        // Step 1: Fetch prelevement from the database using it's id from the request.
        Prelevement prelevement=this.prelevementRepository.findById(requestDto.getIdPrelevement())
                .orElseThrow(()->new ResourceNotFoundException("Prelevement with id "+requestDto.getIdPrelevement()+" not found"));

        // Step 2: Check if the prelevement is not already paid.
        // If it's already paid , throw an exception.
        if (prelevement.getStatus()== Status.PAYE) {
            throw new Exception(" prelevement is already paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched dc entity into a Transaction entity.
        Transaction transactionEntity = prelevementTransactionMapper.toEntity(requestDto,prelevement);

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = transactionRepository.save(transactionEntity);

        // Step 5: Update the bank's current balance
        // Then save the updated bank entity.

        Banque banque = banqueRepository.findById(prelevement.getBanque().getId()).orElseThrow(()->new ResourceNotFoundException("Banque with id "+prelevement.getBanque().getId()+" not found"));

        banque.setSoldeActuel(banque.getSoldeActuel().subtract(prelevement.getMontant()));

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

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public InteretTransactionResponse processInteretToTransaction(InteretValidationRequest requestDto) throws Exception{
        // Step 1: Fetch interet from the database using it's id from the request.
        Interet interet=this.interetRepository.findById(requestDto.getIdInteret())
                .orElseThrow(()->new ResourceNotFoundException("Interet with id "+requestDto.getIdInteret()+" not found"));

        // Step 2: Check if the interet is not already paid.
        // If it's already paid , throw an exception.
        if (interet.getStatus()== Status.PAYE) {
            throw new Exception(" interet is already paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched dc entity into a Transaction entity.
        Transaction transactionEntity = interetTransactionMapper.toEntity(requestDto,interet);

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = transactionRepository.save(transactionEntity);

        // Step 5: Update the bank's current balance
        // Then save the updated bank entity.

        Banque banque = banqueRepository.findById(interet.getBanque().getId()).orElseThrow(()->new ResourceNotFoundException("Banque with id "+interet.getBanque().getId()+" not found"));

        banque.setSoldeActuel(banque.getSoldeActuel().subtract(interet.getMontant()));

        this.banqueRepository.save(banque);

        // Step 6: Mark the interet as * (paid) and save the updated agios entity.
        interet.setStatus(Status.PAYE);
        interet.setObservation(savedTransaction.getObservation());
        interet.setLibelle(savedTransaction.getLibelle());
        interet.setDateValeur(savedTransaction.getDateValeur());
        interet.setDateOperation(savedTransaction.getDateOperations());
        interetRepository.save(interet);

        // Step 7: Convert the saved Transaction entity into a AgiosTransactionResponse
        // using the mapper and return it as the response.
        return interetTransactionMapper.toResponseDto(savedTransaction);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public FraisBancaireTransactionResponse processFraisBancaireToTransaction(FraisBanciareValidationRequest requestDto) throws Exception{
        // Step 1: Fetch frais bancaire from the database using it's id from the request.
        FraisBancaire fraisBancaire=this.fraisBancaireRepository.findById(requestDto.getIdFraisBancaire())
                .orElseThrow(()->new ResourceNotFoundException("Interet with id "+requestDto.getIdFraisBancaire()+" not found"));

        // Step 2: Check if the frais bancaire is not already paid.
        // If it's already paid , throw an exception.
        if (fraisBancaire.getStatus()== Status.PAYE) {
            throw new Exception(" frais bancaire is already paid");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched dc entity into a Transaction entity.
        Transaction transactionEntity = fraisBancaireTransactionMapper.toEntity(requestDto,fraisBancaire);

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = transactionRepository.save(transactionEntity);

        // Step 5: Update the bank's current balance
        // Then save the updated bank entity.

        Banque banque = banqueRepository.findById(fraisBancaire.getBanque().getId()).orElseThrow(()->new ResourceNotFoundException("Banque with id "+fraisBancaire.getBanque().getId()+" not found"));

        banque.setSoldeActuel(banque.getSoldeActuel().subtract(fraisBancaire.getMontant()));

        this.banqueRepository.save(banque);

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


    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ImpotEtTaxeTransactionResponse processImpotEtTaxeToTransaction(ImpotEtTaxeValidationRequest requestDto) throws Exception {
        // Step 1: Fetch impot et taxe from the database using its id from the request.
        ImpotEtTaxe impotEtTaxe = this.impotEtTaxeRepository.findById(requestDto.getIdImpotEtTaxe())
                .orElseThrow(() -> new ResourceNotFoundException("Impôt/Taxe with id " + requestDto.getIdImpotEtTaxe() + " not found"));

        // Step 2: Check if the impot et taxe is not already paid.
        // If it's already paid, throw an exception.
        if (impotEtTaxe.getStatus() == Status.PAYE) {
            throw new Exception("L'impôt/taxe est déjà payé(e)");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched entity into a Transaction entity.
        Transaction transactionEntity = impotEtTaxeTransactionMapper.toEntity(requestDto, impotEtTaxe);

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = transactionRepository.save(transactionEntity);

        // Step 5: Update the bank's current balance
        // Then save the updated bank entity.
        Banque banque = banqueRepository.findById(impotEtTaxe.getBanque().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Banque with id " + impotEtTaxe.getBanque().getId() + " not found"));

        banque.setSoldeActuel(banque.getSoldeActuel().subtract(impotEtTaxe.getMontant()));
        this.banqueRepository.save(banque);

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

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public RestitutionTransactionResponse processRestitutionToTransaction(RestitutionValidationRequest requestDto) throws Exception {
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
            throw new Exception("La restitution est déjà payée");
        }

        // Step 3: Mapper le DTO et l'entité restitution vers une Transaction
        Transaction transactionEntity = restitutionTransactionMapper.toEntity(requestDto, restitution);

        // Step 4: Sauvegarder la transaction
        Transaction savedTransaction = transactionRepository.save(transactionEntity);
        log.info("Transaction créée avec l'ID: {}", savedTransaction.getId());

        // Step 5: Mettre à jour le solde de la banque
        // Les restitutions sont des ENCAISSEMENTS (crédit) donc on AJOUTE au solde
        Banque banque = banqueRepository.findById(restitution.getBanque().getId())
                .orElseThrow(() -> {
                    log.error("Banque introuvable avec l'ID: {}", restitution.getBanque().getId());
                    return new ResourceNotFoundException("Banque introuvable avec l'ID: " + restitution.getBanque().getId());
                });

        banque.setSoldeActuel(banque.getSoldeActuel().add(restitution.getMontant()));
        this.banqueRepository.save(banque);
        log.info("Solde de la banque {} mis à jour: {}", banque.getNom(), banque.getSoldeActuel());

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

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public OperationErroneTransactionResponse processOperationErroneToTransaction(OperationErroneValidationRequest requestDto) throws Exception {
        // Step 1: Fetch operation erroné from the database using its id from the request.
        OperationErrone operationErrone = this.operationErroneRepository.findById(requestDto.getIdOperationErrone())
                .orElseThrow(() -> new ResourceNotFoundException("operation erronée with id " + requestDto.getIdOperationErrone() + " not found"));

        // Step 2: Check if the operation erroné is not already paid.
        // If it's already paid, throw an exception.
        if (operationErrone.getStatus() == Status.PAYE) {
            throw new Exception("L' operation erroné est déjà payé");
        }

        // Step 3: Use the mapper to convert the DTO and the fetched entity into a Transaction entity.
        Transaction transactionEntity = operationErroneTransactionMapper.toEntity(requestDto, operationErrone);

        // Step 4: Save the Transaction entity to the database.
        Transaction savedTransaction = transactionRepository.save(transactionEntity);

        // Step 5: Update the bank's current balance
        // Then save the updated bank entity.
        Banque banque = banqueRepository.findById(operationErrone.getBanque().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Banque with id " + operationErrone.getBanque().getId() + " not found"));

        banque.setSoldeActuel(banque.getSoldeActuel().subtract(operationErrone.getMontant()));
        this.banqueRepository.save(banque);

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
}
