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

    private final TransactionRepository transactionRepository;
    private final BanqueRepository banqueRepository;



    public RapprochementService(LeasingTransactionMapper mapper, CmtTransactionMapper cmtMapper, TransactionRepository transactionRepository, EcheanceLeasingRepository echeanceLeasingRepository, ContratLeasingRepository contratLeasingRepository, BanqueRepository banqueRepository, CreditMoyenTermeRepository creditMoyenTermeRepository, EcheanceCMTRepository echeanceCMTRepository, ChequeRepository chequeRepository, ChequeTransactionMapper chequeTransactionMapper, LcnRepository lcnRepository, LcnTransactionMapper lcnTransactionMapper, VirementTransactionMapper virementTransactionMapper, VirementRepository virementRepository, MiseADispositionRepository miseADispositionRepository, MiseAdispositionTransactionMapper miseAdispositionTransactionMapper) {
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


        // Étape 5: Marquer l'échéance comme payée.
        echeanceCMT.setStatutPaiement(StatutEcheanceCMT.PAYE);
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
        lcnRepository.save(lcn);

        // Step 7: Convert the saved Transaction entity into a LcnTransactionResponse
        // using the mapper and return it as the response.
        return lcnTransactionMapper.toResponseDto(savedTransaction);
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
        miseADispositionRepository.save(miseADisposition);

        // Step 7: Convert the saved Transaction entity into a MiseAdispositionTransactionResponseDto
        // using the mapper and return it as the response.
        return miseAdispositionTransactionMapper.toResponseDto(savedTransaction);
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
        virementRepository.save(virement);

        // Step 7: Convert the saved Transaction entity into a VirementTransactionResponseDto
        // using the mapper and return it as the response.
        return virementTransactionMapper.toResponseDto(savedTransaction);
    }


}
