package staport.finance.gBanque.rapprochement.mapper;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.cheques.entities.Cheque;
import staport.finance.gBanque.rapprochement.dtos.cheques.ChequeTransactionRequest;
import staport.finance.gBanque.rapprochement.dtos.cheques.ChequeTransactionResponseDto;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Component
public  class  ChequeTransactionMapper {

    private final ObjectMapper objectMapper;

    public ChequeTransactionMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    /**
     * Maps ChequeTransactionRequestDto and its associated entities to a Transaction entity.
     * The service layer is responsible for fetching the entities.
     *
     * @param requestDto      The DTO containing the request data.
     * @param cheque  The fetched cheque entity.
     * @return The Transaction entity, ready for persistence.
     * @throws Exception if an error occurs during JSON serialization.
     */
    public  Transaction toEntity(ChequeTransactionRequest requestDto, Cheque cheque ) throws Exception {


        if (requestDto == null || cheque == null ) {
            return null;
        }

        Transaction transaction = new Transaction();

        // Mapping simple fields
        transaction.setObservation(requestDto.getObservation());
        transaction.setTvaDeductible(
                cheque.getMontant()
                        .multiply(BigDecimal.valueOf(requestDto.getTvaDeductible()))   // montant * taux
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)     // /100 arrondi à 2 décimales
                        .setScale(2, RoundingMode.HALF_UP)                            // force toujours 2 décimales
        );

        // Mapping dates (ensuring consistency with OffsetDateTime)
        transaction.setDateEmission(requestDto.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());

        // Mapping complex fields using the fetched entities
        transaction.setLibelle("chèque N° "+cheque.getNumero());
        transaction.setTransactionSource(TransactionSource.CHEQUE);
        transaction.setDebit(cheque.getMontant());
        transaction.setCredit(BigDecimal.ZERO);
        transaction.setBanqueDePrelevement(cheque.getBanque());

        // Mapping traces
        try {
            Map<String, Object> tracesMap = new HashMap<>();
            tracesMap.put("banqueId", cheque.getBanque().getId());
            tracesMap.put("numeroCheque", cheque.getNumero());
            String jsonTraces = objectMapper.writeValueAsString(tracesMap);
            transaction.setTraces(jsonTraces);
        } catch (Exception e) {
            throw new Exception("Erreur lors du mappage des traces", e);
        }

        return transaction;
    }


    /**
     * Maps a Transaction entity to a ChequeTransactionResponseDto.
     *
     * @param transaction The Transaction entity.
     * @return The ChequeTransactionResponseDto.
     * @throws Exception if an error occurs during JSON deserialization.
     */
    public ChequeTransactionResponseDto toResponseDto(Transaction transaction) throws Exception {
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


        // Retrieving IDs from traces
        try {
            Map tracesMap = objectMapper.readValue(transaction.getTraces(), Map.class);
            responseDto.setChequeNumber(String.valueOf(tracesMap.get("numeroCheque").toString()));

        } catch (Exception e) {
            throw new Exception("Erreur lors de la désérialisation des traces", e);
        }

        return responseDto;
    }
}
