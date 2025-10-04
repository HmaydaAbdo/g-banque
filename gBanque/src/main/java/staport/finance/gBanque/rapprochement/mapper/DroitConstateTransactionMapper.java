package staport.finance.gBanque.rapprochement.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.rapprochement.dtos.droitConstate.DroitConstateTransactionRequest;
import staport.finance.gBanque.rapprochement.dtos.droitConstate.DroitConstateTransactionResponse;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.droitConstate.repositories.DroitConstateRepository;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.enums.Type;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Component
public class DroitConstateTransactionMapper {

    private final ObjectMapper objectMapper;
    private final DroitConstateRepository droitConstateRepository;
    private final BanqueRepository banqueRepository;

    public DroitConstateTransactionMapper(ObjectMapper objectMapper, DroitConstateRepository droitConstateRepository, BanqueRepository banqueRepository) {
        this.objectMapper = objectMapper;
        this.droitConstateRepository = droitConstateRepository;
        this.banqueRepository = banqueRepository;
    }


    /**
     * Maps DroitConstateTransactionRequest and its associated entities to a Transaction entity.
     *
     * @param requestDto The DTO containing the request data.
     * @return The Transaction entity, ready for persistence.
     * @throws Exception if an error occurs during JSON serialization.
     */
    public Transaction toEntity(DroitConstateTransactionRequest requestDto  ) throws Exception {


        if (requestDto == null  ) {
            return null;
        }

        Transaction transaction = new Transaction();

        DroitConstate droitConstate=this.droitConstateRepository.findById(requestDto.getDroitConstateId())
                .orElseThrow(()->new Exception("Droits constaté with id "+requestDto.getDroitConstateId()+" not found"));

        Marche marche=droitConstate.getMarche();

        if(marche==null){
            throw new Exception("Marche null for droit constaté with id "+requestDto.getDroitConstateId());
        }

        Banque banque=this.banqueRepository.findById(requestDto.getBanqueId()).orElseThrow(()->new Exception("Banque with id "+requestDto.getBanqueId()+" not found"));
        transaction.setLibelle(requestDto.getLibelle());
        transaction.setTransactionSource(TransactionSource.DROIT_CONSTATE);
        transaction.setDateEmission(requestDto.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setBanqueDePrelevement(banque);

        if(droitConstate.getType()== Type.REMBOURSEMENT){
            transaction.setDebit(droitConstate.getMontant());
            transaction.setCredit(BigDecimal.ZERO);
            transaction.setObservation(requestDto.getObservation());

            BigDecimal ttc = droitConstate.getMontant();
            BigDecimal tauxTVA = BigDecimal.valueOf(requestDto.getTva());
            BigDecimal ht=ttc.divide(tauxTVA.divide(BigDecimal.valueOf(100)).add(BigDecimal.ONE), 2, RoundingMode.HALF_UP);

            BigDecimal tva = ttc.subtract(ht).setScale(2, RoundingMode.HALF_UP);
            transaction.setTvaDeductible(tva);

        }else {
            transaction.setDebit(BigDecimal.ZERO);
            transaction.setCredit(droitConstate.getMontant());
            transaction.setObservation(requestDto.getObservation());

            BigDecimal ttc = droitConstate.getMontant();
            BigDecimal tauxTVA = BigDecimal.valueOf(requestDto.getTva());
            BigDecimal ht=ttc.divide(tauxTVA.divide(BigDecimal.valueOf(100)).add(BigDecimal.ONE), 2, RoundingMode.HALF_UP);

            BigDecimal tva = ttc.subtract(ht).setScale(2, RoundingMode.HALF_UP);
            transaction.setTvaCollectee(tva);
        }



        // Mapping traces
        try {
            Map<String, Object> tracesMap = new HashMap<>();
            tracesMap.put("banqueId", requestDto.getBanqueId());
            tracesMap.put("dcId", requestDto.getDroitConstateId());
            tracesMap.put("marcheNumero", marche.getNumero());
            String jsonTraces = objectMapper.writeValueAsString(tracesMap);
            transaction.setTraces(jsonTraces);
        } catch (Exception e) {
            throw new Exception("Erreur lors du mappage des traces", e);
        }


        droitConstateRepository.save(droitConstate);
        return transaction;
    }

    public DroitConstateTransactionResponse toResponseDto(Transaction transaction) throws Exception {

        if (transaction == null) {
            return null;
        }

        DroitConstateTransactionResponse responseDto = new DroitConstateTransactionResponse();

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
