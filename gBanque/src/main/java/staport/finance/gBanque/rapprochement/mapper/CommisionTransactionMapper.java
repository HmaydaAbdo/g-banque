package staport.finance.gBanque.rapprochement.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.rapprochement.dtos.commision.CommissionTransactionResponse;
import staport.finance.gBanque.rapprochement.dtos.commision.CommissionValidationDto;
import staport.finance.gBanque.commision.entities.Commission;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Component
public class CommisionTransactionMapper {

    private final ObjectMapper objectMapper;

    public CommisionTransactionMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Maps CommissionValidationDto and its associated entities to a Transaction entity.
     * The service layer is responsible for fetching the entities.
     *
     * @param requestDto      The DTO containing the request data.
     * @param commission  The fetched commission entity.
     * @return The Transaction entity, ready for persistence.
     * @throws Exception if an error occurs during JSON serialization.
     */
    public Transaction toEntity(CommissionValidationDto requestDto, Commission commission ) throws Exception {


        if (requestDto == null || commission == null ) {
            return null;
        }

        Transaction transaction = new Transaction();

        // Mapping simple fields
        transaction.setObservation(requestDto.getObservation());
        transaction.setTvaDeductible(
                commission.getMontant()
                        .multiply(BigDecimal.valueOf(commission.getTvaDeductible()))   // montant * taux
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)     // /100 arrondi à 2 décimales
                        .setScale(2, RoundingMode.HALF_UP)                            // force toujours 2 décimales
        );

        // Mapping dates (ensuring consistency with OffsetDateTime)
        transaction.setDateEmission(commission.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());

        // Mapping complex fields using the fetched entities
        transaction.setLibelle(commission.getLibelle());
        transaction.setTransactionSource(TransactionSource.COMMISSION);
        transaction.setDebit(commission.getMontant());
        transaction.setCredit(BigDecimal.ZERO);
        transaction.setBanqueDePrelevement(commission.getBanque());

        // Mapping traces
        try {
            Map<String, Object> tracesMap = new HashMap<>();
            tracesMap.put("banqueId", commission.getBanque().getId());
            String jsonTraces = objectMapper.writeValueAsString(tracesMap);
            transaction.setTraces(jsonTraces);
        } catch (Exception e) {
            throw new Exception("Erreur lors du mappage des traces", e);
        }

        return transaction;
    }


    /**
     * Maps a Transaction entity to a CommissionTransactionResponse.
     *
     * @param transaction The Transaction entity.
     * @return The CommissionTransactionResponse.
     * @throws Exception if an error occurs during JSON deserialization.
     */
    public CommissionTransactionResponse toResponseDto(Transaction transaction) throws Exception {
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
