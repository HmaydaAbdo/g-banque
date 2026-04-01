package staport.finance.gBanque.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.validation.dtos.lcn.LcnTransactionRequest;
import staport.finance.gBanque.validation.dtos.lcn.LcnTransactionResponse;
import staport.finance.gBanque.validation.utils.TVAUtils;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;

@Component
public class LcnTransactionMapper {

    public LcnTransactionMapper() {
    }

    /**
     * Maps LcnTransactionRequestDto and its associated entities to a Transaction entity.
     * The service layer is responsible for fetching the entities.
     *
     * @param requestDto The DTO containing the request data.
     * @param lcn  The fetched lcn entity.
     * @return The Transaction entity, ready for persistence.
     */
    public Transaction toEntity(LcnTransactionRequest requestDto, Lcn lcn )   {


        if (requestDto == null || lcn == null ) {
            return null;
        }

        Transaction transaction = new Transaction();

        // Mapping simple fields
        transaction.setReferent(lcn.getId());
        transaction.setObservation(requestDto.getObservation());
        transaction.setLibelle(requestDto.getLibelle());
        transaction.setDateEmission(requestDto.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setTransactionSource(TransactionSource.LCN);

        transaction.setBanqueDePrelevement(lcn.getBanque());

        BigDecimal ttc = lcn.getMontant();
        BigDecimal tauxTva = BigDecimal.valueOf(requestDto.getTvaDeductible());
        BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTva);

        if(lcn.getType()== Type.DECAISSEMENT){
            transaction.setTvaDeductible(montantTva);
            transaction.setDebit(lcn.getMontant());
            transaction.setCredit(BigDecimal.ZERO);
        }else {
            transaction.setTvaCollectee(montantTva);
            transaction.setDebit(BigDecimal.ZERO);
            transaction.setCredit(lcn.getMontant());
        }

        return transaction;
    }


    public LcnTransactionResponse toResponseDto(Transaction transaction)   {
        if (transaction == null) {
            return null;
        }

        LcnTransactionResponse responseDto = new LcnTransactionResponse();

        // Mapping fields from the entity to the DTO
        responseDto.setId(transaction.getId());
        // Lazy loading might require calling getBanqueDePrelevement() before getName()
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
