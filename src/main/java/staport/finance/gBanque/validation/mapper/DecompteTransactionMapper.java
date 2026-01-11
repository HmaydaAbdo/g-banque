package staport.finance.gBanque.validation.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.validation.dtos.decomptes.DecompteTransactionRequestDto;
import staport.finance.gBanque.validation.dtos.decomptes.DecompteTransactionResponseDto;
import staport.finance.gBanque.decomptes.entities.Decompte;
import staport.finance.gBanque.decomptes.repositories.DecompteRepository;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.validation.utils.TVAUtils;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
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

        Banque banque=this.banqueRepository.findById(requestDto.getBanqueId()).orElseThrow(()->new Exception("Banque with id "+requestDto.getBanqueId()+" not found"));

        transaction.setReferent(decompte.getId());
        transaction.setLibelle(requestDto.getLibelle());
        transaction.setObservation(requestDto.getObservation());
        transaction.setDateEmission(requestDto.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setTransactionSource(TransactionSource.DECOMPTES);
        transaction.setBanqueDePrelevement(banque);
        transaction.setDebit(BigDecimal.ZERO);
        transaction.setCredit(decompte.getMontant());


        BigDecimal ttc = decompte.getMontant();
        BigDecimal tauxTva = BigDecimal.valueOf(requestDto.getTva());
        BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTva);
        transaction.setTvaCollectee(montantTva);



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
