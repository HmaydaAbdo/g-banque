package staport.finance.gBanque.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Agios.entities.Agios;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.validation.dtos.agios.AgiosTransactionResponse;
import staport.finance.gBanque.validation.dtos.agios.AgiosValidationRequest;
import staport.finance.gBanque.validation.utils.TVAUtils;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;


@Component
public class AgiosTransactionMapper {


    public AgiosTransactionMapper() {
    }

    public Transaction toEntity(AgiosValidationRequest requestDto, Agios agios )  {


        if (requestDto == null || agios == null ) {
            return null;
        }

        Transaction transaction = new Transaction();

        // Mapping simple fields
        transaction.setReferent(agios.getId());
        transaction.setObservation(requestDto.getObservation());
        transaction.setDateEmission(requestDto.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setLibelle(requestDto.getLibelle());
        transaction.setTransactionSource(TransactionSource.AGIOS);
        transaction.setDebit(agios.getMontant());
        transaction.setCredit(BigDecimal.ZERO);
        transaction.setBanqueDePrelevement(agios.getBanque());

        BigDecimal ttc = agios.getMontant();
        BigDecimal tauxTVA = BigDecimal.valueOf(requestDto.getTva());

        // Calculer uniquement le montant de la TVA
        BigDecimal tva = TVAUtils.calculerTVA(ttc, tauxTVA);

        // Stocker la TVA dans la transaction
        transaction.setTvaDeductible(tva);


        return transaction;
    }


    public AgiosTransactionResponse toResponseDto(Transaction transaction)   {
        if (transaction == null) {
            return null;
        }

        AgiosTransactionResponse responseDto = new AgiosTransactionResponse();

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
