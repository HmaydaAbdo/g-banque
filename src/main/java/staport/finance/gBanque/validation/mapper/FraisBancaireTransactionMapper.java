package staport.finance.gBanque.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.fraisBancaires.entities.FraisBancaire;
import staport.finance.gBanque.validation.dtos.fraisBancaire.FraisBancaireTransactionResponse;
import staport.finance.gBanque.validation.dtos.fraisBancaire.FraisBanciareValidationRequest;
import staport.finance.gBanque.validation.utils.TVAUtils;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import java.math.BigDecimal;


@Component
public class FraisBancaireTransactionMapper {

    public FraisBancaireTransactionMapper() {
    }

    public Transaction toEntity(FraisBanciareValidationRequest requestDto, FraisBancaire fraisBancaire )   {


        if (requestDto == null || fraisBancaire == null ) {
            return null;
        }

        Transaction transaction = new Transaction();

        // Mapping simple fields
        // observation/dateEmission/libelle : fallback sur les frais bancaires d'origine si non fournis (lot).
        transaction.setReferent(fraisBancaire.getId());
        transaction.setObservation(requestDto.getObservation() != null ? requestDto.getObservation() : fraisBancaire.getObservation());
        transaction.setDateEmission(requestDto.getDateEmission() != null ? requestDto.getDateEmission() : fraisBancaire.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setLibelle(requestDto.getLibelle() != null ? requestDto.getLibelle() : fraisBancaire.getLibelle());
        transaction.setTransactionSource(TransactionSource.FRAIS_BANCAIRE);
        transaction.setDebit(fraisBancaire.getMontant());
        transaction.setCredit(BigDecimal.ZERO);
        transaction.setBanqueDePrelevement(fraisBancaire.getBanque());

        BigDecimal ttc = fraisBancaire.getMontant();
        BigDecimal tauxTVA = BigDecimal.valueOf(requestDto.getTva());

        // Calculer uniquement le montant de la TVA
        BigDecimal tva = TVAUtils.calculerTVA(ttc, tauxTVA);

        // Stocker la TVA dans la transaction
        transaction.setTvaDeductible(tva);



        return transaction;
    }


    public FraisBancaireTransactionResponse toResponseDto(Transaction transaction)   {
        if (transaction == null) {
            return null;
        }

        FraisBancaireTransactionResponse responseDto = new FraisBancaireTransactionResponse();

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
