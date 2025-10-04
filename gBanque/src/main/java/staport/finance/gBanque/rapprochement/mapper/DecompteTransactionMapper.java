package staport.finance.gBanque.rapprochement.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.rapprochement.dtos.decomptes.DecompteTransactionRequestDto;
import staport.finance.gBanque.rapprochement.dtos.decomptes.DecompteTransactionResponseDto;
import staport.finance.gBanque.decomptes.entities.Decompte;
import staport.finance.gBanque.decomptes.repositories.DecompteRepository;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Component
public class DecompteTransactionMapper {

    private final ObjectMapper objectMapper;
    private final DecompteRepository decompteRepository;
    private final BanqueRepository banqueRepository;


    public DecompteTransactionMapper(ObjectMapper objectMapper, DecompteRepository decompteRepository, BanqueRepository banqueRepository) {
        this.objectMapper = objectMapper;
        this.decompteRepository = decompteRepository;
        this.banqueRepository = banqueRepository;
    }



    /**
     * Maps DecompteTransactionRequestDto and its associated entities to a Transaction entity.
     *
     * @param requestDto The DTO containing the request data.
     * @return The Transaction entity, ready for persistence.
     * @throws Exception if an error occurs during JSON serialization.
     */
    public Transaction toEntity(DecompteTransactionRequestDto requestDto  ) throws Exception {

        if (requestDto == null  ) {
            return null;
        }

        Transaction transaction = new Transaction();

        Decompte decompte=this.decompteRepository.findById(requestDto.getDecompteId())
                .orElseThrow(()->new Exception("décompte  with id "+requestDto.getDecompteId()+" not found"));

        Marche marche=decompte.getMarche();

        if(marche==null){
            throw new Exception("Marche null for décompte with id "+requestDto.getDecompteId());
        }

        Banque banque=this.banqueRepository.findById(requestDto.getBanqueId()).orElseThrow(()->new Exception("Banque with id "+requestDto.getBanqueId()+" not found"));
        transaction.setLibelle(requestDto.getLibelle());
        transaction.setTransactionSource(TransactionSource.DECOMPTES);
        transaction.setDateEmission(decompte.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setBanqueDePrelevement(banque);
        transaction.setDebit(BigDecimal.ZERO);
        transaction.setCredit(decompte.getMontant());
        transaction.setObservation(requestDto.getObservation());

        BigDecimal ttc = decompte.getMontant();
        BigDecimal tauxTVA = BigDecimal.valueOf(requestDto.getTva());
        BigDecimal ht=ttc.divide(tauxTVA.divide(BigDecimal.valueOf(100)).add(BigDecimal.ONE), 2, RoundingMode.HALF_UP);

        BigDecimal tva = ttc.subtract(ht).setScale(2, RoundingMode.HALF_UP);
        transaction.setTvaCollectee(tva);

        // Mapping traces
        try {
            Map<String, Object> tracesMap = new HashMap<>();
            tracesMap.put("banqueId", requestDto.getBanqueId());
            tracesMap.put("decompteId", requestDto.getDecompteId());
            tracesMap.put("marcheNumero", marche.getNumero());
            String jsonTraces = objectMapper.writeValueAsString(tracesMap);
            transaction.setTraces(jsonTraces);
        } catch (Exception e) {
            throw new Exception("Erreur lors du mappage des traces", e);
        }

        return transaction;
    }

    public DecompteTransactionResponseDto toResponseDto(Transaction transaction) throws Exception {

        if (transaction == null) {
            return null;
        }

        DecompteTransactionResponseDto responseDto = new DecompteTransactionResponseDto();

        responseDto.setId(transaction.getId());
        responseDto.setBanqueName(transaction.getBanqueDePrelevement().getNom());
        responseDto.setLibelle(transaction.getLibelle());
        responseDto.setObservation(transaction.getObservation());
        responseDto.setDateEmission(transaction.getDateEmission());
        responseDto.setDateValeur(transaction.getDateValeur());
        responseDto.setDateOperations(transaction.getDateOperations());
        responseDto.setDebit(transaction.getDebit());
        responseDto.setCredit(transaction.getCredit());


        try {
            Map tracesMap = objectMapper.readValue(transaction.getTraces(), Map.class);
            responseDto.setMarcheNumero(String.valueOf(tracesMap.get("marcheNumero").toString()));
        } catch (Exception e) {
            throw new Exception("Erreur lors de la désérialisation des traces", e);
        }


        return responseDto;

    }
}
