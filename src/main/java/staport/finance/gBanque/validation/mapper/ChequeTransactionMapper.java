package staport.finance.gBanque.validation.mapper;


import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.moyensDePaiements.cheques.entities.Cheque;
import staport.finance.gBanque.validation.dtos.cheques.ChequeTransactionRequest;
import staport.finance.gBanque.validation.dtos.cheques.ChequeTransactionResponseDto;
import staport.finance.gBanque.validation.utils.TVAUtils;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;

@Component
public  class  ChequeTransactionMapper {

    public ChequeTransactionMapper() {
    }

    public  Transaction toEntity(ChequeTransactionRequest requestDto, Cheque cheque )   {


        if (requestDto == null || cheque == null ) {
            return null;
        }

        Transaction transaction = new Transaction();
        transaction.setReferent(cheque.getId());
        transaction.setObservation(requestDto.getObservation());
        transaction.setLibelle(requestDto.getLibelle());
        transaction.setDateEmission(requestDto.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setTransactionSource(TransactionSource.CHEQUE);
        transaction.setBanqueDePrelevement(cheque.getBanque());

        // Calculer  la TVA avec utilitaire
        BigDecimal ttc = cheque.getMontant();
        BigDecimal tauxTva = BigDecimal.valueOf(requestDto.getTvaDeductible());
        BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTva);

        if(cheque.getType()== Type.DECAISSEMENT){
            transaction.setDebit(cheque.getMontant());
            transaction.setCredit(BigDecimal.ZERO);
            transaction.setTvaDeductible(montantTva);
        }else {
            transaction.setDebit(BigDecimal.ZERO);
            transaction.setCredit(cheque.getMontant());
            transaction.setTvaCollectee(montantTva);
        }



        return transaction;
    }


    /**
     * Maps a Transaction entity to a ChequeTransactionResponseDto.
     *
     * @param transaction The Transaction entity.
     * @return The ChequeTransactionResponseDto.
     */
    public ChequeTransactionResponseDto toResponseDto(Transaction transaction)   {
        if (transaction == null) {
            return null;
        }

        ChequeTransactionResponseDto responseDto = new ChequeTransactionResponseDto();

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
