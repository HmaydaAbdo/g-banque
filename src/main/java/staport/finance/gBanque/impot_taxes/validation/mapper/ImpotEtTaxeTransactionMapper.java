package staport.finance.gBanque.impot_taxes.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.impot_taxes.entities.ImpotEtTaxe;
import staport.finance.gBanque.impot_taxes.validation.dto.ImpotEtTaxeTransactionRequestDto;
import staport.finance.gBanque.impot_taxes.validation.dto.ImpotEtTaxeTransactionResponseDto;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;

/**
 * Mapper de validation pour le module Impôts et Taxes (architecture.md, Règle 1).
 *
 * <p>Le mapper ne connaît aucun Repository : l'{@link ImpotEtTaxe} et la {@link Banque} sont
 * résolus et validés par {@code ImpotEtTaxeValidationService} avant l'appel à {@link #toEntity}.
 * Un impôt/taxe est toujours un décaissement (débit).
 */
@Component
public class ImpotEtTaxeTransactionMapper {

    public Transaction toEntity(ImpotEtTaxeTransactionRequestDto requestDto, ImpotEtTaxe impotEtTaxe, BigDecimal montantTva) {

        if (requestDto == null || impotEtTaxe == null) {
            return null;
        }

        Transaction transaction = new Transaction();

        // observation/dateEmission/libelle : fallback sur l'impôt/taxe d'origine si non fournis (lot).
        transaction.setReferent(impotEtTaxe.getId());
        transaction.setObservation(requestDto.observation() != null ? requestDto.observation() : impotEtTaxe.getObservation());
        transaction.setDateEmission(requestDto.dateEmission() != null ? requestDto.dateEmission() : impotEtTaxe.getDateEmission());
        transaction.setDateValeur(requestDto.dateValeur());
        transaction.setDateOperations(requestDto.dateOperation());
        transaction.setLibelle(requestDto.libelle() != null ? requestDto.libelle() : impotEtTaxe.getLibelle());
        transaction.setTransactionSource(TransactionSource.IMPOT_TAXE);
        transaction.setDebit(impotEtTaxe.getMontant());
        transaction.setCredit(BigDecimal.ZERO);
        transaction.setBanqueDePrelevement(impotEtTaxe.getBanque());

        transaction.setTvaDeductible(montantTva);

        return transaction;
    }

    public ImpotEtTaxeTransactionResponseDto toResponseDto(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        Banque banque = transaction.getBanqueDePrelevement();

        return new ImpotEtTaxeTransactionResponseDto(
                transaction.getId(),
                transaction.getReferent(),
                banque != null ? banque.getNom() : null,
                transaction.getLibelle(),
                transaction.getObservation(),
                transaction.getDateEmission(),
                transaction.getDateValeur(),
                transaction.getDateOperations(),
                transaction.getTvaDeductible(),
                transaction.getTvaCollectee(),
                transaction.getDebit(),
                transaction.getCredit(),
                TransactionSource.IMPOT_TAXE
        );
    }
}
