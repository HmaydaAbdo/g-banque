package staport.finance.gBanque.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.validation.dtos.miseAdispo.MiseAdispositionTransactionRequestDto;
import staport.finance.gBanque.validation.dtos.miseAdispo.MiseAdispositionTransactionResponseDto;
import staport.finance.gBanque.validation.utils.TVAUtils;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;

@Component
public class MiseAdispositionTransactionMapper {

    public MiseAdispositionTransactionMapper() {
    }

    /**
     * Maps MiseAdispositionTransactionRequestDto and its associated entities to a Transaction entity.
     * The service layer is responsible for fetching the entities.
     *
     * @param requestDto The DTO containing the request data.
     * @param miseADispo  The fetched miseADispo entity.
     * @return The Transaction entity, ready for persistence.
     */
    public Transaction toEntity(MiseAdispositionTransactionRequestDto requestDto, MiseADisposition miseADispo )   {


        if (requestDto == null || miseADispo == null ) {
            return null;
        }

        Transaction transaction = new Transaction();

        // Mapping simple fields
        // observation/dateEmission/libelle : fallback sur la mise à disposition d'origine si non fournis (lot).
        transaction.setReferent(miseADispo.getId());
        transaction.setObservation(requestDto.getObservation() != null ? requestDto.getObservation() : miseADispo.getObservation());
        transaction.setDateEmission(requestDto.getDateEmission() != null ? requestDto.getDateEmission() : miseADispo.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setLibelle(requestDto.getLibelle() != null ? requestDto.getLibelle() : miseADispo.getLibelle());
        transaction.setTransactionSource(TransactionSource.MiseADisposition);
        transaction.setBanqueDePrelevement(miseADispo.getBanque());

        BigDecimal ttc = miseADispo.getMontant();
        BigDecimal tauxTva = BigDecimal.valueOf(requestDto.getTvaDeductible());
        BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTva);

        if(miseADispo.getType()== Type.DECAISSEMENT){
            transaction.setTvaDeductible(montantTva);
            transaction.setTvaCollectee(BigDecimal.ZERO);
            transaction.setDebit(miseADispo.getMontant());
            transaction.setCredit(BigDecimal.ZERO);
        }else{
            transaction.setTvaDeductible(BigDecimal.ZERO);
            transaction.setTvaCollectee(montantTva);
            transaction.setDebit(BigDecimal.ZERO);
            transaction.setCredit(miseADispo.getMontant());
        }


        return transaction;
    }


    /**
     * Maps a Transaction entity to a MiseAdispositionTransactionResponseDto.
     *
     * @param transaction The Transaction entity.
     * @return The MiseAdispositionTransactionResponseDto.
     */
    public MiseAdispositionTransactionResponseDto toResponseDto(Transaction transaction)   {
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


        return responseDto;
    }
}
