package staport.finance.gBanque.validation.mapper;


import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.HashMap;
import java.util.Map;

@Component
public class ImpayeeTransactionMapper {

    private final ObjectMapper objectMapper;

    public ImpayeeTransactionMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Maps ImpayeeValidationRequest and its associated entities to a Transaction entity.
     * The service layer is responsible for fetching the entities.
     *
     * @param requestDto  The DTO containing the request data.
     * @param impayee     The fetched impayee entity.
     * @return The Transaction entity, ready for persistence.
     * @throws Exception if an error occurs during JSON serialization.
     */
    public Transaction toEntity(ImpayeeValidationRequest requestDto, Impayee impayee) throws Exception {

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

    /**
     * Maps a Transaction entity to an ImpayeeTransactionResponse.
     *
     * @param transaction The Transaction entity.
     * @return The ImpayeeTransactionResponse.
     * @throws Exception if an error occurs during JSON deserialization.
     */
    public ImpayeeTransactionResponse toResponseDto(Transaction transaction) throws Exception {
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