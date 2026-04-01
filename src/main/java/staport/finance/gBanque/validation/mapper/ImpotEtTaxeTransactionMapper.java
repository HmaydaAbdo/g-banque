package staport.finance.gBanque.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.impot_taxes.entities.ImpotEtTaxe;
import staport.finance.gBanque.validation.dtos.impotsEtTaxes.ImpotEtTaxeTransactionResponse;
import staport.finance.gBanque.validation.dtos.impotsEtTaxes.ImpotEtTaxeValidationRequest;
import staport.finance.gBanque.validation.utils.TVAUtils;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;


@Component
public class ImpotEtTaxeTransactionMapper {
    public ImpotEtTaxeTransactionMapper() {
    }

    /**
     * Maps ImpotEtTaxeValidationRequest and its associated entities to a Transaction entity.
     * The service layer is responsible for fetching the entities.
     *
     * @param requestDto      The DTO containing the request data.
     * @param impotEtTaxe     The fetched impot et taxe entity.
     * @return The Transaction entity, ready for persistence.
     */
    public Transaction toEntity(ImpotEtTaxeValidationRequest requestDto, ImpotEtTaxe impotEtTaxe)  {

        if (requestDto == null || impotEtTaxe == null) {
            return null;
        }

        Transaction transaction = new Transaction();

        // Mapping simple fields
        transaction.setReferent(impotEtTaxe.getId());
        transaction.setObservation(requestDto.getObservation());
        transaction.setDateEmission(requestDto.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setLibelle(requestDto.getLibelle());
        transaction.setTransactionSource(TransactionSource.IMPOT_TAXE);
        transaction.setDebit(impotEtTaxe.getMontant());
        transaction.setCredit(BigDecimal.ZERO);
        transaction.setBanqueDePrelevement(impotEtTaxe.getBanque());

        BigDecimal ttc = impotEtTaxe.getMontant();
        BigDecimal tauxTva = BigDecimal.valueOf(requestDto.getTva());
        BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTva);
        transaction.setTvaDeductible(montantTva);



        return transaction;
    }

    /**
     * Maps a Transaction entity to an ImpotEtTaxeTransactionResponse.
     *
     * @param transaction The Transaction entity.
     * @return The ImpotEtTaxeTransactionResponse.
     */
    public ImpotEtTaxeTransactionResponse toResponseDto(Transaction transaction)  {
        if (transaction == null) {
            return null;
        }

        ImpotEtTaxeTransactionResponse responseDto = new ImpotEtTaxeTransactionResponse();

        // Mapping fields from the entity to the DTO
        responseDto.setId(transaction.getId());

        // Lazy loading might require calling getBanqueDePrelevement() before getNom()
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