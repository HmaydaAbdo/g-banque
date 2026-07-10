package staport.finance.gBanque.validation.mapper;


import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.operationErrone.entities.OperationErrone;
import staport.finance.gBanque.validation.dtos.operationErrone.OperationErroneTransactionResponse;
import staport.finance.gBanque.validation.dtos.operationErrone.OperationErroneValidationRequest;
import staport.finance.gBanque.validation.utils.TVAUtils;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;

@Component
public class OperationErroneTransactionMapper {

    public OperationErroneTransactionMapper() {
    }

    public Transaction toEntity(OperationErroneValidationRequest requestDto, OperationErrone operationErrone) {
        if (requestDto == null || operationErrone == null) {
            return null;
        }

        Transaction transaction = new Transaction();

        // Mapping simple fields
        // observation/dateEmission/libelle : fallback sur l'opération erronée d'origine si non fournis (lot).
        transaction.setReferent(operationErrone.getId());
        transaction.setObservation(requestDto.getObservation() != null ? requestDto.getObservation() : operationErrone.getObservation());
        transaction.setDateEmission(requestDto.getDateEmission() != null ? requestDto.getDateEmission() : operationErrone.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setLibelle(requestDto.getLibelle() != null ? requestDto.getLibelle() : operationErrone.getLibelle());
        transaction.setTransactionSource(TransactionSource.OPERATION_ERRONE);
        transaction.setDebit(operationErrone.getMontant());
        transaction.setCredit(BigDecimal.ZERO);
        transaction.setBanqueDePrelevement(operationErrone.getBanque());

        BigDecimal ttc = operationErrone.getMontant();
        BigDecimal tauxTVA = BigDecimal.valueOf(requestDto.getTva());

        // Calculer uniquement le montant de la TVA
        BigDecimal tva = TVAUtils.calculerTVA(ttc, tauxTVA);

        // Stocker la TVA dans la transaction
        transaction.setTvaDeductible(tva);

        return transaction;
    }

    public OperationErroneTransactionResponse toResponseDto(Transaction transaction){
        if (transaction == null) {
            return null;
        }

        OperationErroneTransactionResponse responseDto = new OperationErroneTransactionResponse();

        // Mapping fields from the entity to the DTO
        responseDto.setId(transaction.getId());

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