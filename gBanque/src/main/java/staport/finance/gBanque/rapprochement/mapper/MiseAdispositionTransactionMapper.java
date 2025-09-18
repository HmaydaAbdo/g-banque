package staport.finance.gBanque.rapprochement.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.rapprochement.dtos.miseAdispo.MiseAdispositionTransactionRequestDto;
import staport.finance.gBanque.rapprochement.dtos.miseAdispo.MiseAdispositionTransactionResponseDto;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Component
public class MiseAdispositionTransactionMapper {

    private final ObjectMapper objectMapper;

    public MiseAdispositionTransactionMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Maps MiseAdispositionTransactionRequestDto and its associated entities to a Transaction entity.
     * The service layer is responsible for fetching the entities.
     *
     * @param requestDto The DTO containing the request data.
     * @param miseADispo  The fetched miseADispo entity.
     * @return The Transaction entity, ready for persistence.
     * @throws Exception if an error occurs during JSON serialization.
     */
    public Transaction toEntity(MiseAdispositionTransactionRequestDto requestDto, MiseADisposition miseADispo ) throws Exception {


        if (requestDto == null || miseADispo == null ) {
            return null;
        }

        Transaction transaction = new Transaction();

        // Mapping simple fields
        transaction.setObservation(requestDto.getObservation());

        transaction.setTvaDeductible(
                miseADispo.getMontant()
                        .multiply(BigDecimal.valueOf(requestDto.getTvaDeductible()))   // montant * taux
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)     // /100 arrondi à 2 décimales
                        .setScale(2, RoundingMode.HALF_UP)                            // force toujours 2 décimales
        );
        // Mapping dates (ensuring consistency with OffsetDateTime)
        transaction.setDateEmission(requestDto.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());

        // Mapping complex fields using the fetched entities
        transaction.setLibelle("Mise à disposition  N° "+miseADispo.getNumero());
        transaction.setTransactionSource(TransactionSource.MiseADisposition);
        transaction.setDebit(miseADispo.getMontant());
        transaction.setCredit(BigDecimal.ZERO);
        transaction.setBanqueDePrelevement(miseADispo.getBanque());

        // Mapping traces
        try {
            Map<String, Object> tracesMap = new HashMap<>();
            tracesMap.put("banqueId", miseADispo.getBanque().getId());
            tracesMap.put("numeroMiseAdispo", miseADispo.getNumero());
            String jsonTraces = objectMapper.writeValueAsString(tracesMap);
            transaction.setTraces(jsonTraces);
        } catch (Exception e) {
            throw new Exception("Erreur lors du mappage des traces", e);
        }
        return transaction;
    }


    /**
     * Maps a Transaction entity to a MiseAdispositionTransactionResponseDto.
     *
     * @param transaction The Transaction entity.
     * @return The MiseAdispositionTransactionResponseDto.
     * @throws Exception if an error occurs during JSON deserialization.
     */
    public MiseAdispositionTransactionResponseDto toResponseDto(Transaction transaction) throws Exception {
        if (transaction == null) {
            return null;
        }

        MiseAdispositionTransactionResponseDto responseDto = new MiseAdispositionTransactionResponseDto();

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
            responseDto.setMiseADispositionNumber(String.valueOf(tracesMap.get("numeroMiseAdispo").toString()));
        } catch (Exception e) {
            throw new Exception("Erreur lors de la désérialisation des traces", e);
        }

        return responseDto;
    }
}
