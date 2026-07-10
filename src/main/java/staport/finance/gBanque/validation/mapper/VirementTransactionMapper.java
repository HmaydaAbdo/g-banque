package staport.finance.gBanque.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
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

    public VirementTransactionMapper() {
    }

    /**
     * Maps VirementTransactionRequestDto and its associated entities to a Transaction entity.
     * The service layer is responsible for fetching the entities.
     *
     * @param requestDto The DTO containing the request data.
     * @param virement  The fetched virement entity.
     * @return The Transaction entity, ready for persistence.
     */
    public Transaction toEntity(VirementTransactionRequestDto requestDto, Virement virement )   {


        if (requestDto == null || virement == null ) {
            return null;
        }

        Transaction transaction = new Transaction();

        // Mapping simple fields
        // observation/dateEmission/libelle : si non fournis (validation en lot), on conserve
        // ceux du virement d'origine plutôt que de les écraser (voir BatchValidationRequestDto).
        transaction.setReferent(virement.getId());
        transaction.setObservation(requestDto.getObservation() != null ? requestDto.getObservation() : virement.getObservation());
        transaction.setDateEmission(requestDto.getDateEmission() != null ? requestDto.getDateEmission() : virement.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setLibelle(requestDto.getLibelle() != null ? requestDto.getLibelle() : virement.getLibelle());
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
     */
    public VirementTransactionResponseDto toResponseDto(Transaction transaction)   {
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
