package staport.finance.gBanque.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.factoring.repository.FactoringRepository;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.enums.Type;
import staport.finance.gBanque.validation.dtos.factoring.FactoringTransactionRequest;
import staport.finance.gBanque.validation.dtos.factoring.FactoringTransactionResponse;
import staport.finance.gBanque.validation.utils.TVAUtils;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import java.math.BigDecimal;

@Component
public class FactoringTransactionMapper {

    private final FactoringRepository factoringRepository;
    private final BanqueRepository banqueRepository;

    public FactoringTransactionMapper( FactoringRepository factoringRepository, BanqueRepository banqueRepository) {
        this.factoringRepository = factoringRepository;
        this.banqueRepository = banqueRepository;
    }


    public Transaction toEntity(FactoringTransactionRequest requestDto  )   {


        if (requestDto == null  ) {
            return null;
        }

        Transaction transaction = new Transaction();

        Factoring factoring=this.factoringRepository.findById(requestDto.getFactoringId())
                .orElseThrow(()->new ResourceNotFoundException("Factoring  with id "+requestDto.getFactoringId()+" not found"));

        Marche marche=factoring.getMarche();

        if(marche==null){
            throw new ResourceNotFoundException("Marche null for factoring  with id "+requestDto.getFactoringId());
        }

        if (marche.getBanque() == null || !marche.getBanque().getId().equals(requestDto.getBanqueId())) {
            throw new ValidationException("Le banqueId fourni (" + requestDto.getBanqueId()
                    + ") ne correspond pas à la banque du marché associé au factoring "
                    + requestDto.getFactoringId() + ".");
        }

        Banque banque=this.banqueRepository.findById(requestDto.getBanqueId()).orElseThrow(()->new ResourceNotFoundException("Banque with id "+requestDto.getBanqueId()+" not found"));
        // libelle/observation/dateEmission : fallback sur le factoring d'origine si non fournis (lot).
        transaction.setReferent(factoring.getId());
        transaction.setLibelle(requestDto.getLibelle() != null ? requestDto.getLibelle() : factoring.getLibelle());
        transaction.setObservation(requestDto.getObservation() != null ? requestDto.getObservation() : factoring.getObservation());
        transaction.setTransactionSource(TransactionSource.FACTORING);
        transaction.setDateEmission(requestDto.getDateEmission() != null ? requestDto.getDateEmission() : factoring.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setBanqueDePrelevement(banque);

        if(factoring.getType()== Type.REMBOURSEMENT){
            transaction.setDebit(factoring.getMontant());
            transaction.setCredit(BigDecimal.ZERO);

            BigDecimal ttc = factoring.getMontant();
            BigDecimal tauxTVA = BigDecimal.valueOf(requestDto.getTva());

            BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTVA);

            transaction.setTvaDeductible(montantTva);

        }else {
            transaction.setDebit(BigDecimal.ZERO);
            transaction.setCredit(factoring.getMontant());
            BigDecimal ttc = factoring.getMontant();
            BigDecimal tauxTva = BigDecimal.valueOf(requestDto.getTva());
            BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTva);
            transaction.setTvaCollectee(montantTva);
        }

        return transaction;
    }

    public FactoringTransactionResponse toResponseDto(Transaction transaction)   {

        if (transaction == null) {
            return null;
        }

        FactoringTransactionResponse responseDto = new FactoringTransactionResponse();

        responseDto.setId(transaction.getId());
        responseDto.setBanqueName(transaction.getBanqueDePrelevement().getNom());
        responseDto.setLibelle(transaction.getLibelle());
        responseDto.setObservation(transaction.getObservation());
        responseDto.setDateEmission(transaction.getDateEmission());
        responseDto.setDateValeur(transaction.getDateValeur());
        responseDto.setDateOperations(transaction.getDateOperations());
        responseDto.setDebit(transaction.getDebit());
        responseDto.setCredit(transaction.getCredit());

        return responseDto;

    }

}
