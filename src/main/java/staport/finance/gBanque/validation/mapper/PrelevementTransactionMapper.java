package staport.finance.gBanque.validation.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.prelevements.entitie.Prelevement;
import staport.finance.gBanque.validation.dtos.prelevements.PrelevementTransactionResponse;
import staport.finance.gBanque.validation.dtos.prelevements.PrelevementValidationRequest;
import staport.finance.gBanque.validation.utils.TVAUtils;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
public class PrelevementTransactionMapper {

    private final ObjectMapper objectMapper;

    public PrelevementTransactionMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }



    public Transaction toEntity(PrelevementValidationRequest requestDto, Prelevement prelevement ) throws Exception {


        if (requestDto == null || prelevement == null ) {
            return null;
        }

        Transaction transaction = new Transaction();

        // Mapping simple fields
        transaction.setObservation(requestDto.getObservation());
        transaction.setDateEmission(requestDto.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setLibelle(requestDto.getLibelle());
        transaction.setTransactionSource(TransactionSource.PRELEVEMENT);
        transaction.setDebit(prelevement.getMontant());
        transaction.setCredit(BigDecimal.ZERO);
        transaction.setBanqueDePrelevement(prelevement.getBanque());

        BigDecimal ttc = prelevement.getMontant();
        BigDecimal tauxTVA = BigDecimal.valueOf(requestDto.getTva());

        // Calculer uniquement le montant de la TVA
        BigDecimal tva = TVAUtils.calculerTVA(ttc, tauxTVA);

        // Stocker la TVA dans la transaction
        transaction.setTvaDeductible(tva);

        // Mapping traces
        try {
            Map<String, Object> tracesMap = new HashMap<>();
            tracesMap.put("banqueId", prelevement.getBanque().getId());
            String jsonTraces = objectMapper.writeValueAsString(tracesMap);
            transaction.setTraces(jsonTraces);
        } catch (Exception e) {
            throw new Exception("Erreur lors du mappage des traces", e);
        }

        return transaction;
    }


    public PrelevementTransactionResponse toResponseDto(Transaction transaction) throws Exception {
        if (transaction == null) {
            return null;
        }

        PrelevementTransactionResponse responseDto = new PrelevementTransactionResponse();

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
