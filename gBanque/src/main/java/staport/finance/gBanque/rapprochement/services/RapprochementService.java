package staport.finance.gBanque.rapprochement.services;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;
import staport.finance.gBanque.cmt.echeances.enums.StatutEcheanceCMT;
import staport.finance.gBanque.cmt.echeances.repositories.EcheanceCMTRepository;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;
import staport.finance.gBanque.cmt.repositories.CreditMoyenTermeRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.contratLeasing.repositories.ContratLeasingRepository;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.repositories.EcheanceLeasingRepository;
import staport.finance.gBanque.rapprochement.dtos.CmtTransactionRequestDto;
import staport.finance.gBanque.rapprochement.dtos.CmtTransactionResponseDto;
import staport.finance.gBanque.rapprochement.dtos.LeasingTransactionRequestDto;
import staport.finance.gBanque.rapprochement.dtos.LeasingTransactionResponseDto;
import staport.finance.gBanque.rapprochement.mapper.CmtTransactionMapper;
import staport.finance.gBanque.rapprochement.mapper.LeasingTransactionMapper;
import staport.finance.gBanque.rapprochement.entities.Transaction;
import staport.finance.gBanque.rapprochement.repositories.TransactionRepository;


@Service
public class RapprochementService {

    private final LeasingTransactionMapper leasingTransactionMapper;
    private final CmtTransactionMapper cmtTransactionMapper;
    private final TransactionRepository transactionRepository;
    private final EcheanceLeasingRepository echeanceLeasingRepository;
    private final ContratLeasingRepository contratLeasingRepository;
    private final BanqueRepository banqueRepository;
    private final CreditMoyenTermeRepository creditMoyenTermeRepository;
    private final EcheanceCMTRepository echeanceCMTRepository;

    public RapprochementService(LeasingTransactionMapper mapper, CmtTransactionMapper cmtMapper, TransactionRepository transactionRepository, EcheanceLeasingRepository echeanceLeasingRepository, ContratLeasingRepository contratLeasingRepository, BanqueRepository banqueRepository, CreditMoyenTermeRepository creditMoyenTermeRepository, EcheanceCMTRepository echeanceCMTRepository) {
        this.leasingTransactionMapper = mapper;
        this.cmtTransactionMapper = cmtMapper;
        this.transactionRepository = transactionRepository;
        this.echeanceLeasingRepository = echeanceLeasingRepository;
        this.contratLeasingRepository = contratLeasingRepository;
        this.banqueRepository = banqueRepository;
        this.creditMoyenTermeRepository = creditMoyenTermeRepository;
        this.echeanceCMTRepository = echeanceCMTRepository;
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






}
