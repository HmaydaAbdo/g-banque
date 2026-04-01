package staport.finance.gBanque.validation.mapper;


import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.impayees.entities.Impayee;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.validation.dtos.impayee.ImpayeeTransactionResponse;
import staport.finance.gBanque.validation.dtos.impayee.ImpayeeValidationRequest;
import staport.finance.gBanque.validation.utils.TVAUtils;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;


@Component
public class ImpayeeTransactionMapper {


    public ImpayeeTransactionMapper() {
    }

    /**
     * Maps ImpayeeValidationRequest and its associated entities to a Transaction entity.
     * The service layer is responsible for fetching the entities.
     *
     * @param requestDto  The DTO containing the request data.
     * @param impayee     The fetched impayee entity.
     * @return The Transaction entity, ready for persistence.
     */
    public Transaction toEntity(ImpayeeValidationRequest requestDto, Impayee impayee)  {

        if (requestDto == null || impayee == null) {
            return null;
        }

        Transaction transaction = new Transaction();

        // Mapping simple fields
        transaction.setReferent(impayee.getId());
        transaction.setObservation(requestDto.getObservation());
        transaction.setDateEmission(requestDto.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setLibelle(requestDto.getLibelle());
        transaction.setTransactionSource(TransactionSource.IMPAYEE);
        transaction.setBanqueDePrelevement(impayee.getBanque());
        BigDecimal ttc = impayee.getMontant();
        BigDecimal tauxTva = BigDecimal.valueOf(requestDto.getTva());
        BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTva);


        if(impayee.getType()== Type.DECAISSEMENT){
            transaction.setDebit(impayee.getMontant());
            transaction.setCredit(BigDecimal.ZERO);
            transaction.setTvaDeductible(montantTva);
        }else {
            transaction.setDebit(BigDecimal.ZERO);
            transaction.setCredit(impayee.getMontant());
            transaction.setTvaCollectee(montantTva);
        }


        return transaction;
    }


    public ImpayeeTransactionResponse toResponseDto(Transaction transaction)  {
        if (transaction == null) {
            return null;
        }

        ImpayeeTransactionResponse responseDto = new ImpayeeTransactionResponse();

        // Mapping fields from the entity to the DTO
        responseDto.setId(transaction.getId());

        // Lazy loading might require calling getBanqueDePrelevement() before getNom()
        Banque banque = transaction.getBanqueDePrelevement();
        if (banque != null) {
            responseDto.setBanqueName(banque.getNom());
        }

        responseDto.setLibelle(transaction.getLibelle());
        responseDto.setObservation(transaction.getObservation());
        responseDto.setDateEmission(transaction.getDateEmission());
        responseDto.setDateValeur(transaction.getDateValeur());
        responseDto.setDateOperations(transaction.getDateOperations());
        responseDto.setTvaDeductible(transaction.getTvaDeductible());
        responseDto.setTvaCollectee(transaction.getTvaCollectee());
        responseDto.setDebit(transaction.getDebit());
        responseDto.setCredit(transaction.getCredit());

        return responseDto;
    }
}