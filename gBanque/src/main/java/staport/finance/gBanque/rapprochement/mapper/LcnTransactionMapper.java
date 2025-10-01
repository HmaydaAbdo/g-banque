package staport.finance.gBanque.rapprochement.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.lcn.entities.Lcn;
import staport.finance.gBanque.rapprochement.dtos.lcn.LcnTransactionRequest;
import staport.finance.gBanque.rapprochement.dtos.lcn.LcnTransactionResponse;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Component
public class LcnTransactionMapper {

    private final ObjectMapper objectMapper;

    public LcnTransactionMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Maps LcnTransactionRequestDto and its associated entities to a Transaction entity.
     * The service layer is responsible for fetching the entities.
     *
     * @param requestDto The DTO containing the request data.
     * @param lcn  The fetched lcn entity.
     * @return The Transaction entity, ready for persistence.
     * @throws Exception if an error occurs during JSON serialization.
     */
    public Transaction toEntity(LcnTransactionRequest requestDto, Lcn lcn ) throws Exception {


        if (requestDto == null || lcn == null ) {
            return null;
        }

        Transaction transaction = new Transaction();

        // Mapping simple fields
        transaction.setObservation(requestDto.getObservation());
        transaction.setLibelle(requestDto.getLibelle());
        transaction.setDateEmission(requestDto.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setTransactionSource(TransactionSource.LCN);
        transaction.setDebit(lcn.getMontant());
        transaction.setCredit(BigDecimal.ZERO);
        transaction.setBanqueDePrelevement(lcn.getBanque());

        BigDecimal ttc = lcn.getMontant();
        BigDecimal tauxTva = BigDecimal.valueOf(requestDto.getTvaDeductible());

        // HT = TTC / (1 + taux/100)
        BigDecimal ht = ttc
                .divide(BigDecimal.ONE.add(tauxTva.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)),
                        2, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);

        // TVA = TTC - HT
        BigDecimal montantTva = ttc.subtract(ht).setScale(2, RoundingMode.HALF_UP);

        transaction.setTvaDeductible(montantTva);

        // Mapping traces
        try {
            Map<String, Object> tracesMap = new HashMap<>();
            tracesMap.put("banqueId", lcn.getBanque().getId());
            tracesMap.put("numeroLcn", lcn.getNumero());
            String jsonTraces = objectMapper.writeValueAsString(tracesMap);
            transaction.setTraces(jsonTraces);
        } catch (Exception e) {
            throw new Exception("Erreur lors du mappage des traces", e);
        }

        return transaction;
    }


    /**
     * Maps a Transaction entity to a LcnTransactionResponse.
     *
     * @param transaction The Transaction entity.
     * @return The LcnTransactionResponse.
     * @throws Exception if an error occurs during JSON deserialization.
     */
    public LcnTransactionResponse toResponseDto(Transaction transaction) throws Exception {
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


        // Retrieving IDs from traces
        try {
            Map tracesMap = objectMapper.readValue(transaction.getTraces(), Map.class);
            responseDto.setLcnNumber(String.valueOf(tracesMap.get("numeroLcn").toString()));

        } catch (Exception e) {
            throw new Exception("Erreur lors de la désérialisation des traces", e);
        }

        return responseDto;
    }
}
