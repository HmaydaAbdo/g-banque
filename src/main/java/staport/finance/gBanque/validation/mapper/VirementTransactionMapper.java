package staport.finance.gBanque.validation.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.validation.dtos.virements.VirementTransactionRequestDto;
import staport.finance.gBanque.validation.dtos.virements.VirementTransactionResponseDto;
import staport.finance.gBanque.validation.utils.TVAUtils;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;

@Component
public class VirementTransactionMapper {

    private final ObjectMapper objectMapper;
    private final BanqueRepository banqueRepository;

    public VirementTransactionMapper(ObjectMapper objectMapper, BanqueRepository banqueRepository) {
        this.objectMapper = objectMapper;
        this.banqueRepository = banqueRepository;
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
        transaction.setDateEmission(requestDto.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setLibelle(requestDto.getLibelle());
        transaction.setTransactionSource(TransactionSource.Virement);

        BigDecimal ttc = virement.getMontant();
        BigDecimal tauxTva = BigDecimal.valueOf(requestDto.getTvaDeductible());
        BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTva);

        if(virement.getType()== Type.DECAISSEMENT){
            transaction.setDebit(virement.getMontant());
            transaction.setCredit(BigDecimal.ZERO);
            transaction.setTvaDeductible(montantTva);
            transaction.setBanqueDePrelevement(virement.getBanqueEmettrice());

        }else {
            transaction.setDebit(BigDecimal.ZERO);
            transaction.setCredit(virement.getMontant());
            transaction.setTvaCollectee(montantTva);
            transaction.setBanqueDePrelevement(virement.getBanqueBeneficiare());

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

        return responseDto;
    }
}
