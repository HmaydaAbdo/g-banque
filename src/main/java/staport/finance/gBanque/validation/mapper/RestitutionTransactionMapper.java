package staport.finance.gBanque.validation.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.restitution.entities.Restitution;
import staport.finance.gBanque.validation.dtos.restitutions.RestitutionTransactionResponse;
import staport.finance.gBanque.validation.dtos.restitutions.RestitutionValidationRequest;
import staport.finance.gBanque.validation.utils.TVAUtils;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
public class RestitutionTransactionMapper {

    private final ObjectMapper objectMapper;

    public RestitutionTransactionMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Convertit le DTO de validation et l'entité Restitution en Transaction
     */
    public Transaction toEntity(RestitutionValidationRequest requestDto, Restitution restitution) throws Exception {

        if (requestDto == null || restitution == null) {
            return null;
        }

        Transaction transaction = new Transaction();

        // Mapping des champs simples
        transaction.setReferent(restitution.getId());
        transaction.setObservation(requestDto.getObservation());
        transaction.setDateEmission(requestDto.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setLibelle(requestDto.getLibelle());
        transaction.setTransactionSource(TransactionSource.RESTITUTION);

        // Les restitutions sont des ENCAISSEMENTS (crédit)
        transaction.setCredit(restitution.getMontant());
        transaction.setDebit(BigDecimal.ZERO);

        transaction.setBanqueDePrelevement(restitution.getBanque());

        // Calcul de la TVA
        BigDecimal ttc = restitution.getMontant();
        BigDecimal tauxTVA = BigDecimal.valueOf(requestDto.getTva());

        // Calculer uniquement le montant de la TVA
        BigDecimal tva = TVAUtils.calculerTVA(ttc, tauxTVA);

        // Stocker la TVA dans la transaction (TVA collectée pour les encaissements)
        transaction.setTvaCollectee(tva);

        // Mapping des traces
        try {
            Map<String, Object> tracesMap = new HashMap<>();
            tracesMap.put("banqueId", restitution.getBanque().getId());
            tracesMap.put("restitutionId", restitution.getId());
            String jsonTraces = objectMapper.writeValueAsString(tracesMap);
            transaction.setTraces(jsonTraces);
        } catch (Exception e) {
            throw new Exception("Erreur lors du mappage des traces", e);
        }

        return transaction;
    }

    /**
     * Convertit une Transaction en DTO de réponse
     */
    public RestitutionTransactionResponse toResponseDto(Transaction transaction) throws Exception {
        if (transaction == null) {
            return null;
        }

        RestitutionTransactionResponse responseDto = new RestitutionTransactionResponse();

        // Mapping des champs de l'entité vers le DTO
        responseDto.setId(transaction.getId());

        // Gestion du lazy loading de la banque
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