package staport.finance.gBanque.rapprochement.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.factoring.repository.FactoringRepository;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.enums.Type;
import staport.finance.gBanque.rapprochement.dtos.factoring.FactoringTransactionRequest;
import staport.finance.gBanque.rapprochement.dtos.factoring.FactoringTransactionResponse;
import staport.finance.gBanque.rapprochement.utils.TVAUtils;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
public class FactoringTransactionMapper {

    private final ObjectMapper objectMapper;
    private final FactoringRepository factoringRepository;
    private final BanqueRepository banqueRepository;

    public FactoringTransactionMapper(ObjectMapper objectMapper, FactoringRepository factoringRepository, BanqueRepository banqueRepository) {
        this.objectMapper = objectMapper;
        this.factoringRepository = factoringRepository;
        this.banqueRepository = banqueRepository;
    }


    public Transaction toEntity(FactoringTransactionRequest requestDto  ) throws Exception {


        if (requestDto == null  ) {
            return null;
        }

        Transaction transaction = new Transaction();

        Factoring factoring=this.factoringRepository.findById(requestDto.getFactoringId())
                .orElseThrow(()->new Exception("Factoring  with id "+requestDto.getFactoringId()+" not found"));

        Marche marche=factoring.getMarche();

        if(marche==null){
            throw new Exception("Marche null for factoring  with id "+requestDto.getFactoringId());
        }

        Banque banque=this.banqueRepository.findById(requestDto.getBanqueId()).orElseThrow(()->new Exception("Banque with id "+requestDto.getBanqueId()+" not found"));
        transaction.setLibelle(requestDto.getLibelle());
        transaction.setObservation(requestDto.getObservation());
        transaction.setTransactionSource(TransactionSource.FACTORING);
        transaction.setDateEmission(requestDto.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setBanqueDePrelevement(banque);

        if(factoring.getType()== Type.REMBOURSEMENT){
            transaction.setDebit(factoring.getMontant());
            transaction.setCredit(BigDecimal.ZERO);

            BigDecimal ttc = factoring.getMontant();
            BigDecimal tauxTVA = BigDecimal.valueOf(requestDto.getTva());

            BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTVA);

            transaction.setTvaDeductible(montantTva);

        }else {
            transaction.setDebit(BigDecimal.ZERO);
            transaction.setCredit(factoring.getMontant());
            BigDecimal ttc = factoring.getMontant();
            BigDecimal tauxTva = BigDecimal.valueOf(requestDto.getTva());
            BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTva);
            transaction.setTvaCollectee(montantTva);
        }



        // Mapping traces
        try {
            Map<String, Object> tracesMap = new HashMap<>();
            tracesMap.put("banqueId", requestDto.getBanqueId());
            tracesMap.put("factoringId", requestDto.getFactoringId());
            tracesMap.put("marcheNumero", marche.getNumero());
            String jsonTraces = objectMapper.writeValueAsString(tracesMap);
            transaction.setTraces(jsonTraces);
        } catch (Exception e) {
            throw new Exception("Erreur lors du mappage des traces", e);
        }


        return transaction;
    }

    public FactoringTransactionResponse toResponseDto(Transaction transaction) throws Exception {

        if (transaction == null) {
            return null;
        }

        FactoringTransactionResponse responseDto = new FactoringTransactionResponse();

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
