package staport.finance.gBanque.rapprochement.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.rapprochement.dtos.virements.VirementTransactionRequestDto;
import staport.finance.gBanque.rapprochement.dtos.virements.VirementTransactionResponseDto;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.virements.entities.Virement;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
public class VirementTransactionMapper {

    private final ObjectMapper objectMapper;

    public VirementTransactionMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    /**
     * Maps VirementTransactionRequestDto and its associated entities to a Transaction entity.
     * The service layer is responsible for fetching the entities.
     *
     * @param requestDto The DTO containing the request data.
     * @param virement  The fetched virement entity.
     * @return The Transaction entity, ready for persistence.
     * @throws Exception if an error occurs during JSON serialization.
     */
    public Transaction toEntity(VirementTransactionRequestDto requestDto, Virement virement ) throws Exception {


        if (requestDto == null || virement == null ) {
            return null;
        }

        Transaction transaction = new Transaction();

        // Mapping simple fields
        transaction.setObservation(requestDto.getObservation());
        transaction.setTvaDeductible(requestDto.getTvaDeductible() / 100);

        // Mapping dates (ensuring consistency with OffsetDateTime)
        transaction.setDateEmission(requestDto.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());

        // Mapping complex fields using the fetched entities
        transaction.setLibelle("Virement  N° "+virement.getNumero());
        transaction.setTransactionSource(TransactionSource.Virement);
        transaction.setDebit(virement.getMontant());
        transaction.setCredit(BigDecimal.ZERO);
        transaction.setBanqueDePrelevement(virement.getBanque());

        // Mapping traces
        try {
            Map<String, Object> tracesMap = new HashMap<>();
            tracesMap.put("banqueId", virement.getBanque().getId());
            tracesMap.put("numeroVirement", virement.getNumero());
            String jsonTraces = objectMapper.writeValueAsString(tracesMap);
            transaction.setTraces(jsonTraces);
        } catch (Exception e) {
            throw new Exception("Erreur lors du mappage des traces", e);
        }
        return transaction;
    }


    /**
     * Maps a Transaction entity to a VirementTransactionResponseDto.
     *
     * @param transaction The Transaction entity.
     * @return The VirementTransactionResponseDto.
     * @throws Exception if an error occurs during JSON deserialization.
     */
    public VirementTransactionResponseDto toResponseDto(Transaction transaction) throws Exception {
        if (transaction == null) {
            return null;
        }

        VirementTransactionResponseDto responseDto = new VirementTransactionResponseDto();

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
            responseDto.setVirementNumber(String.valueOf(tracesMap.get("numeroVirement").toString()));
        } catch (Exception e) {
            throw new Exception("Erreur lors de la désérialisation des traces", e);
        }

        return responseDto;
    }
}
