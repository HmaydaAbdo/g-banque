package staport.finance.gBanque.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.validation.dtos.commision.CommissionTransactionResponse;
import staport.finance.gBanque.validation.dtos.commision.CommissionValidationDto;
import staport.finance.gBanque.commision.entities.Commission;
import staport.finance.gBanque.validation.utils.TVAUtils;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import java.math.BigDecimal;


@Component
public class CommisionTransactionMapper {

    public CommisionTransactionMapper() {
    }

    public Transaction toEntity(CommissionValidationDto requestDto, Commission commission )   {


        if (requestDto == null || commission == null ) {
            return null;
        }

        Transaction transaction = new Transaction();

        // Mapping simple fields
        // observation/dateEmission/libelle : fallback sur la commission d'origine si non fournis (lot).
        transaction.setReferent(commission.getId());
        transaction.setObservation(requestDto.getObservation() != null ? requestDto.getObservation() : commission.getObservation());
        transaction.setDateEmission(requestDto.getDateEmission() != null ? requestDto.getDateEmission() : commission.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setLibelle(requestDto.getLibelle() != null ? requestDto.getLibelle() : commission.getLibelle());
        transaction.setTransactionSource(TransactionSource.COMMISSION);
        transaction.setDebit(commission.getMontant());
        transaction.setCredit(BigDecimal.ZERO);
        transaction.setBanqueDePrelevement(commission.getBanque());

        BigDecimal ttc = commission.getMontant();
        BigDecimal tauxTva = BigDecimal.valueOf(requestDto.getTva());
        BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTva);
        transaction.setTvaDeductible(montantTva);


        return transaction;
    }


    /**
     * Maps a Transaction entity to a CommissionTransactionResponse.
     *
     * @param transaction The Transaction entity.
     * @return The CommissionTransactionResponse.
     * @throws Exception if an error occurs during JSON deserialization.
     */
    public CommissionTransactionResponse toResponseDto(Transaction transaction)   {
        if (transaction == null) {
            return null;
        }

        CommissionTransactionResponse responseDto = new CommissionTransactionResponse();

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
