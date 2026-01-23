package staport.finance.gBanque.validation.mapper;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.moyensDePaiements.virements.virementErrone.entities.VirementErrone;
import staport.finance.gBanque.validation.dtos.virementErrone.VirementErroneTransactionResponse;
import staport.finance.gBanque.validation.dtos.virementErrone.VirementErroneValidationRequest;
import staport.finance.gBanque.validation.utils.TVAUtils;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
public class VirementErroneTransactionMapper {

    private final ObjectMapper objectMapper;

    public VirementErroneTransactionMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Transaction toEntity(VirementErroneValidationRequest requestDto, VirementErrone virementErrone) throws Exception {
        if (requestDto == null || virementErrone == null) {
            return null;
        }

        Transaction transaction = new Transaction();

        // Mapping simple fields
        transaction.setReferent(virementErrone.getId());
        transaction.setObservation(requestDto.getObservation());
        transaction.setDateEmission(requestDto.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setLibelle(requestDto.getLibelle());
        transaction.setTransactionSource(TransactionSource.VIREMENT_ERRONE);
        transaction.setDebit(virementErrone.getMontant());
        transaction.setCredit(BigDecimal.ZERO);
        transaction.setBanqueDePrelevement(virementErrone.getBanque());

        BigDecimal ttc = virementErrone.getMontant();
        BigDecimal tauxTVA = BigDecimal.valueOf(requestDto.getTva());

        // Calculer uniquement le montant de la TVA
        BigDecimal tva = TVAUtils.calculerTVA(ttc, tauxTVA);

        // Stocker la TVA dans la transaction
        transaction.setTvaDeductible(tva);

        return transaction;
    }

    public VirementErroneTransactionResponse toResponseDto(Transaction transaction) throws Exception {
        if (transaction == null) {
            return null;
        }

        VirementErroneTransactionResponse responseDto = new VirementErroneTransactionResponse();

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