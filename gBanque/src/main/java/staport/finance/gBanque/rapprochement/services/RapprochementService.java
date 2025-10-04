package staport.finance.gBanque.rapprochement.services;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.cheques.entities.Cheque;
import staport.finance.gBanque.cheques.repositories.ChequeRepository;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;
import staport.finance.gBanque.cmt.echeances.enums.StatutEcheanceCMT;
import staport.finance.gBanque.cmt.echeances.repositories.EcheanceCMTRepository;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;
import staport.finance.gBanque.cmt.repositories.CreditMoyenTermeRepository;
import staport.finance.gBanque.commision.entities.Commission;
import staport.finance.gBanque.commision.repository.CommisionRepository;
import staport.finance.gBanque.rapprochement.dtos.commision.CommissionTransactionResponse;
import staport.finance.gBanque.rapprochement.dtos.commision.CommissionValidationDto;
import staport.finance.gBanque.rapprochement.dtos.decomptes.DecompteTransactionRequestDto;
import staport.finance.gBanque.rapprochement.dtos.decomptes.DecompteTransactionResponseDto;
import staport.finance.gBanque.decomptes.entities.Decompte;
import staport.finance.gBanque.decomptes.repositories.DecompteRepository;
import staport.finance.gBanque.rapprochement.dtos.droitConstate.DroitConstateTransactionRequest;
import staport.finance.gBanque.rapprochement.dtos.droitConstate.DroitConstateTransactionResponse;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.droitConstate.repositories.DroitConstateRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.lcn.entities.Lcn;
import staport.finance.gBanque.lcn.repositories.LcnRepository;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.contratLeasing.repositories.ContratLeasingRepository;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.repositories.EcheanceLeasingRepository;
import staport.finance.gBanque.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.miseADisposition.repositories.MiseADispositionRepository;
import staport.finance.gBanque.rapprochement.dtos.prefinancements.PrefinancementTransactionRequest;
import staport.finance.gBanque.rapprochement.dtos.prefinancements.PrefinancementTransactionResponse;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;
import staport.finance.gBanque.prefinancement.repository.PrefinancementRepository;
import staport.finance.gBanque.rapprochement.dtos.cheques.ChequeTransactionRequest;
import staport.finance.gBanque.rapprochement.dtos.cheques.ChequeTransactionResponseDto;
import staport.finance.gBanque.rapprochement.dtos.cmt.CmtTransactionRequestDto;
import staport.finance.gBanque.rapprochement.dtos.cmt.CmtTransactionResponseDto;
import staport.finance.gBanque.rapprochement.dtos.lcn.LcnTransactionRequest;
import staport.finance.gBanque.rapprochement.dtos.lcn.LcnTransactionResponse;
import staport.finance.gBanque.rapprochement.dtos.leasing.LeasingTransactionRequestDto;
import staport.finance.gBanque.rapprochement.dtos.leasing.LeasingTransactionResponseDto;
import staport.finance.gBanque.rapprochement.dtos.miseAdispo.MiseAdispositionTransactionRequestDto;
import staport.finance.gBanque.rapprochement.dtos.miseAdispo.MiseAdispositionTransactionResponseDto;
import staport.finance.gBanque.rapprochement.dtos.virements.VirementTransactionRequestDto;
import staport.finance.gBanque.rapprochement.dtos.virements.VirementTransactionResponseDto;
import staport.finance.gBanque.rapprochement.mapper.*;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.repositories.TransactionRepository;
import staport.finance.gBanque.virements.entities.Virement;
import staport.finance.gBanque.virements.repositories.VirementRepository;


@Service
public class RapprochementService {

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

    private final TransactionRepository transactionRepository;
    private final BanqueRepository banqueRepository;



    public RapprochementService(LeasingTransactionMapper mapper, CmtTransactionMapper cmtMapper, TransactionRepository transactionRepository, EcheanceLeasingRepository echeanceLeasingRepository, ContratLeasingRepository contratLeasingRepository, BanqueRepository banqueRepository, CreditMoyenTermeRepository creditMoyenTermeRepository, EcheanceCMTRepository echeanceCMTRepository, ChequeRepository chequeRepository, ChequeTransactionMapper chequeTransactionMapper, LcnRepository lcnRepository, LcnTransactionMapper lcnTransactionMapper, VirementTransactionMapper virementTransactionMapper, VirementRepository virementRepository, MiseADispositionRepository miseADispositionRepository, MiseAdispositionTransactionMapper miseAdispositionTransactionMapper, PrefinancementRepository prefinancementRepository, PrefinancementTransactionMapper prefinancementTransactionMapper, DroitConstateRepository droitConstateRepository, DroitConstateTransactionMapper droitConstateTransactionMapper, DecompteRepository decompteRepository, DecompteTransactionMapper decompteTransactionMapper, CommisionRepository commisionRepository, CommisionTransactionMapper commisionTransactionMapper) {
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
    }

    @Transactional
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

        banque.setSoldeActuel(banque.getSoldeActuel().subtract(savedTransaction.getDebit()));
        this.banqueRepository.save(banque);


        // Étape 5: Marquer l'échéance comme payée.
        echeanceLeasing.setStatutPaiement(StatutEcheanceLeasing.PAYE);
        echeanceLeasing.setDateValeur(savedTransaction.getDateValeur());
        echeanceLeasing.setDateOperation(savedTransaction.getDateOperations());
        echeanceLeasingRepository.save(echeanceLeasing);

        // Étape 6: Utiliser le mapper pour convertir l'entité sauvegardée en DTO de réponse.
        return leasingTransactionMapper.toResponseDto(savedTransaction);
    }


    @Transactional
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
        banque.setSoldeActuel(banque.getSoldeActuel().subtract(savedTransaction.getDebit()));
        this.banqueRepository.save(banque);


        // Étape 5 : Marquer l'échéance comme payée et enregistrer les dates rélatives au rapprochement .
        echeanceCMT.setStatutPaiement(StatutEcheanceCMT.PAYE);
        echeanceCMT.setDateValeur(savedTransaction.getDateValeur());
        echeanceCMT.setDateOperation(savedTransaction.getDateOperations());
        echeanceCMTRepository.save(echeanceCMT);

        // Étape 6: Utiliser le mapper pour convertir l'entité sauvegardée en DTO de réponse.
        return cmtTransactionMapper.toResponseDto(savedTransaction);
    }


    @Transactional
    public ChequeTransactionResponseDto processChequeTransaction(ChequeTransactionRequest requestDto) throws Exception{
        // Step 1: Fetch the Chèque entity from the database using the Chèque number from the request.
        // If the Chèque does not exist, throw a ResourceNotFoundException.
        Cheque cheque = chequeRepository.findByNumero(requestDto.getNumeroCheque())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cheque with numero " + requestDto.getNumeroCheque() + " not found"));

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
        banque.setSoldeActuel(banque.getSoldeActuel().subtract(savedTransaction.getDebit()));
        this.banqueRepository.save(banque);

        // Step 6: Mark the Chèque as inactive (paid) and save the updated Chèque entity.
        cheque.setActive(false);
        cheque.setDateOperation(savedTransaction.getDateOperations());
        cheque.setDateValeur(savedTransaction.getDateValeur());
        chequeRepository.save(cheque);

        // Step 7: Convert the saved Transaction entity into a ChequeTransactionResponseDto
        // using the mapper and return it as the response.
        return chequeTransactionMapper.toResponseDto(savedTransaction);
    }


    @Transactional
    public LcnTransactionResponse processLcnTransaction(LcnTransactionRequest requestDto) throws Exception{
        // Step 1: Fetch the Lcn entity from the database using the Lcn number from the request.
        // If the Lcn does not exist, throw a ResourceNotFoundException.
        Lcn lcn = lcnRepository.findByNumero(requestDto.getNumeroLcn())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lcn with numero " + requestDto.getNumeroLcn() + " not found"));

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
        banque.setSoldeActuel(banque.getSoldeActuel().subtract(savedTransaction.getDebit()));
        this.banqueRepository.save(banque);

        // Step 6: Mark the lcn as inactive (paid) and save the updated lcn entity.
        lcn.setActive(false);
        lcn.setDateOperation(savedTransaction.getDateOperations());
        lcn.setDateValeur(savedTransaction.getDateValeur());
        lcnRepository.save(lcn);

        // Step 7: Convert the saved Transaction entity into a LcnTransactionResponse
        // using the mapper and return it as the response.
        return lcnTransactionMapper.toResponseDto(savedTransaction);
    }

    @Transactional
    public VirementTransactionResponseDto processVirementTransaction(VirementTransactionRequestDto requestDto) throws Exception{
        // Step 1: Fetch virement entity from the database using it's number from the request.
        // If the virement does not exist, throw a ResourceNotFoundException.
        Virement virement = virementRepository.findById(requestDto.getNumeroVirement())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "virement with numero " + requestDto.getNumeroVirement() + " not found"));

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
        Banque banque = virement.getBanque();
        banque.setSoldeActuel(banque.getSoldeActuel().subtract(savedTransaction.getDebit()));
        this.banqueRepository.save(banque);

        // Step 6: Mark the virement as inactive (paid) and save the updated virement entity.
        virement.setActive(false);
        virement.setDateOperation(savedTransaction.getDateOperations());
        virement.setDateValeur(savedTransaction.getDateValeur());
        virementRepository.save(virement);

        // Step 7: Convert the saved Transaction entity into a VirementTransactionResponseDto
        // using the mapper and return it as the response.
        return virementTransactionMapper.toResponseDto(savedTransaction);
    }

    @Transactional
    public MiseAdispositionTransactionResponseDto processMiseAdispoTransaction(MiseAdispositionTransactionRequestDto requestDto) throws Exception{
        // Step 1: Fetch the MiseAdispo entity from the database using it's number from the request.
        // If the MiseAdispo does not exist, throw a ResourceNotFoundException.
        MiseADisposition miseADisposition = miseADispositionRepository.findById(requestDto.getNumeroMiseAdisposition())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Mise à disposition  with numero " + requestDto.getNumeroMiseAdisposition() + " not found"));

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
        banque.setSoldeActuel(banque.getSoldeActuel().subtract(savedTransaction.getDebit()));
        this.banqueRepository.save(banque);

        // Step 6: Mark the miseADisposition as inactive (paid) and save the updated miseADisposition entity.
        miseADisposition.setActive(false);
        miseADisposition.setDateOperation(savedTransaction.getDateOperations());
        miseADisposition.setDateValeur(savedTransaction.getDateValeur());
        miseADispositionRepository.save(miseADisposition);

        // Step 7: Convert the saved Transaction entity into a MiseAdispositionTransactionResponseDto
        // using the mapper and return it as the response.
        return miseAdispositionTransactionMapper.toResponseDto(savedTransaction);
    }


    @Transactional
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
            banque.setSoldeActuel(banque.getSoldeActuel().subtract(savedTransaction.getDebit()));
        }else{
            banque.setSoldeActuel(banque.getSoldeActuel().add(savedTransaction.getCredit()));
        }

        this.banqueRepository.save(banque);

        // Step 6: Mark the prefi as * (paid) and save the updated prefi entity.
        prefinancement.setStatus(Status.PAYE);
        prefinancement.setDateOperation(savedTransaction.getDateOperations());
        prefinancement.setDateValeur(savedTransaction.getDateValeur());
        prefinancement.setObservation(savedTransaction.getObservation());
        prefinancementRepository.save(prefinancement);

        // Step 7: Convert the saved Transaction entity into a PrefinancementTransactionResponse
        // using the mapper and return it as the response.
        return prefinancementTransactionMapper.toResponseDto(savedTransaction);
    }

    @Transactional
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
            banque.setSoldeActuel(banque.getSoldeActuel().subtract(savedTransaction.getDebit()));
        }else{
            banque.setSoldeActuel(banque.getSoldeActuel().add(savedTransaction.getCredit()));
        }
        this.banqueRepository.save(banque);

        // Step 6: Mark the prefi as * (paid) and save the updated prefi entity.
        droitConstate.setStatus(Status.PAYE);
        droitConstate.setDateOperation(savedTransaction.getDateOperations());
        droitConstate.setDateValeur(savedTransaction.getDateValeur());
        droitConstate.setObservation(savedTransaction.getObservation());

        droitConstateRepository.save(droitConstate);

        // Step 7: Convert the saved Transaction entity into a DroitConstateTransactionResponse
        // using the mapper and return it as the response.
        return droitConstateTransactionMapper.toResponseDto(savedTransaction);
    }

    @Transactional
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

        banque.setSoldeActuel(banque.getSoldeActuel().add(savedTransaction.getCredit()));

        this.banqueRepository.save(banque);

        // Step 6: Mark the decompte as * (paid) and save the updated decompte entity.
        decompte.setStatus(Status.PAYE);
        decompte.setDateOperation(savedTransaction.getDateOperations());
        decompte.setDateValeur(savedTransaction.getDateValeur());
        decompte.setObservation(savedTransaction.getObservation());
        decompteRepository.save(decompte);

        // Step 7: Convert the saved Transaction entity into a DroitConstateTransactionResponse
        // using the mapper and return it as the response.
        return decompteTransactionMapper.toResponseDto(savedTransaction);
    }

    @Transactional
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

        banque.setSoldeActuel(banque.getSoldeActuel().subtract(savedTransaction.getDebit()));

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
}
