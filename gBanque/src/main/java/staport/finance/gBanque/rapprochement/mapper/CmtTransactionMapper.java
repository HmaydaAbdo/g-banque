package staport.finance.gBanque.rapprochement.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;
import staport.finance.gBanque.rapprochement.dtos.cmt.CmtTransactionRequestDto;
import staport.finance.gBanque.rapprochement.dtos.cmt.CmtTransactionResponseDto;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Component
public class CmtTransactionMapper {

    private final ObjectMapper objectMapper;

    public CmtTransactionMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }



    /**
     * Maps a CmtTransactionRequestDto and its associated entities to a Transaction entity.
     * The service layer is responsible for fetching the entities.
     *
     * @param requestDto      The DTO containing the request data.
     * @param creditMoyenTerme  The fetched cmt entity.
     * @param echeanceCMT The fetched EcheanceCMT entity.
     * @return The Transaction entity, ready for persistence.
     * @throws Exception if an error occurs during JSON serialization.
     */
    public Transaction toEntity(CmtTransactionRequestDto requestDto, CreditMoyenTerme creditMoyenTerme, EcheanceCMT echeanceCMT) throws Exception {


        if (requestDto == null || creditMoyenTerme == null || echeanceCMT == null) {
            return null;
        }

        Transaction transaction = new Transaction();

        // Mapping simple fields
        transaction.setObservation(requestDto.getObservation());

        transaction.setTvaDeductible(
                creditMoyenTerme.getEcheance()
                        .multiply(BigDecimal.valueOf(requestDto.getTvaDeductible()))   // montant * taux
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)     // /100 arrondi à 2 décimales
                        .setScale(2, RoundingMode.HALF_UP)                            // force toujours 2 décimales
        );
        // Mapping dates (ensuring consistency with OffsetDateTime)
        transaction.setDateEmission(requestDto.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());

        // Mapping complex fields using the fetched entities
        transaction.setLibelle(" échéance CMT");

        transaction.setTransactionSource(TransactionSource.CMT);
        transaction.setDebit(echeanceCMT.getMontant());
        transaction.setCredit(BigDecimal.ZERO);
        transaction.setBanqueDePrelevement(creditMoyenTerme.getBanque());

        // Mapping traces
        try {
            Map<String, Object> tracesMap = new HashMap<>();
            tracesMap.put("banqueId", creditMoyenTerme.getBanque().getId());
            tracesMap.put("cmtId", creditMoyenTerme.getId());
            tracesMap.put("echeanceId", echeanceCMT.getId());
            String jsonTraces = objectMapper.writeValueAsString(tracesMap);
            transaction.setTraces(jsonTraces);
        } catch (Exception e) {
            throw new Exception("Erreur lors du mappage des traces", e);
        }

        return transaction;
    }

    /**
     * Maps a Transaction entity to a CmtTransactionResponseDto.
     *
     * @param transaction The Transaction entity.
     * @return The CmtTransactionResponseDto.
     * @throws Exception if an error occurs during JSON deserialization.
     */
    public CmtTransactionResponseDto toResponseDto(Transaction transaction) throws Exception {
        if (transaction == null) {
            return null;
        }

        CmtTransactionResponseDto responseDto = new CmtTransactionResponseDto();

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
        responseDto.setTransactionSource(transaction.getTransactionSource());

        // Retrieving IDs from traces
        try {
            Map tracesMap = objectMapper.readValue(transaction.getTraces(), Map.class);
            responseDto.setCreditoMoyenTermeId(Long.valueOf(tracesMap.get("cmtId").toString()));
            responseDto.setEcheanceId(Long.valueOf(tracesMap.get("echeanceId").toString()));
        } catch (Exception e) {
            throw new Exception("Erreur lors de la désérialisation des traces", e);
        }

        return responseDto;
    }
}