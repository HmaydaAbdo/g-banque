package staport.finance.gBanque.validation.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.validation.dtos.prefinancements.PrefinancementTransactionRequest;
import staport.finance.gBanque.validation.dtos.prefinancements.PrefinancementTransactionResponse;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Type;
import staport.finance.gBanque.prefinancement.repository.PrefinancementRepository;
import staport.finance.gBanque.validation.utils.TVAUtils;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
public class PrefinancementTransactionMapper {

    private final ObjectMapper objectMapper;
    private final PrefinancementRepository prefinancementRepository;
    private final BanqueRepository  banqueRepository;

    public PrefinancementTransactionMapper(ObjectMapper objectMapper, PrefinancementRepository prefinancementRepository, BanqueRepository banqueRepository) {
        this.objectMapper = objectMapper;
        this.prefinancementRepository = prefinancementRepository;
        this.banqueRepository = banqueRepository;
    }

    /**
     * Maps PrefinancementTransactionRequest and its associated entities to a Transaction entity.
     *
     * @param requestDto The DTO containing the request data.
     * @return The Transaction entity, ready for persistence.
     * @throws Exception if an error occurs during JSON serialization.
     */
    public Transaction toEntity(PrefinancementTransactionRequest requestDto  ) throws Exception {


        if (requestDto == null  ) {
            return null;
        }

        Transaction transaction = new Transaction();

        Prefinancement prefinancement=this.prefinancementRepository.findById(requestDto.getPrefinancementId())
                .orElseThrow(()->new Exception("Prefinancement with id "+requestDto.getPrefinancementId()+" not found"));

        Marche marche=prefinancement.getMarche();

        if(marche==null){
            throw new Exception("Marche null for prefinancement with id "+requestDto.getPrefinancementId());
        }

        Banque banque=this.banqueRepository.findById(requestDto.getBanqueId()).orElseThrow(()->new Exception("Banque with id "+requestDto.getBanqueId()+" not found"));

        transaction.setReferent(prefinancement.getId());
        transaction.setLibelle(requestDto.getLibelle());
        transaction.setDateEmission(requestDto.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setBanqueDePrelevement(banque);
        transaction.setObservation(requestDto.getObservation());
        transaction.setTransactionSource(TransactionSource.PREFINANCEMENT);

        if(prefinancement.getType()== Type.REMBOURSEMENT){
            transaction.setDebit(prefinancement.getMontant());
            transaction.setCredit(BigDecimal.ZERO);

            BigDecimal ttc = prefinancement.getMontant();
            BigDecimal tauxTva = BigDecimal.valueOf(requestDto.getTva());
            BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTva);
            transaction.setTvaDeductible(montantTva);

        }else {
            transaction.setDebit(BigDecimal.ZERO);
            transaction.setCredit(prefinancement.getMontant());

            BigDecimal ttc = prefinancement.getMontant();
            BigDecimal tauxTva = BigDecimal.valueOf(requestDto.getTva());
            BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTva);
            transaction.setTvaCollectee(montantTva);
        }



        // Mapping traces
        try {
            Map<String, Object> tracesMap = new HashMap<>();
            tracesMap.put("banqueId", requestDto.getBanqueId());
            tracesMap.put("prefiId", requestDto.getPrefinancementId());
            tracesMap.put("marcheNumero", marche.getNumero());
            String jsonTraces = objectMapper.writeValueAsString(tracesMap);
            transaction.setTraces(jsonTraces);
        } catch (Exception e) {
            throw new Exception("Erreur lors du mappage des traces", e);
        }

        return transaction;
    }

    public PrefinancementTransactionResponse toResponseDto(Transaction transaction) throws Exception {

        if (transaction == null) {
            return null;
        }

        PrefinancementTransactionResponse responseDto = new PrefinancementTransactionResponse();

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
