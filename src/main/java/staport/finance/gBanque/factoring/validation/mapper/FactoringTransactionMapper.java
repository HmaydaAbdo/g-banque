package staport.finance.gBanque.factoring.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.factoring.validation.dto.FactoringTransactionRequest;
import staport.finance.gBanque.factoring.validation.dto.FactoringTransactionResponse;
import staport.finance.gBanque.prefinancement.enums.Type;
import staport.finance.gBanque.validation.utils.TVAUtils;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;

/**
 * Mapper de validation pour le module Factoring (architecture.md, Règle 1).
 *
 * <p>Le mapper ne connaît aucun Repository : le {@link Factoring} et la {@link Banque} sont
 * résolus et validés par {@code FactoringValidationService} avant l'appel à {@link #toEntity}.
 */
@Component
public class FactoringTransactionMapper {

    public Transaction toEntity(FactoringTransactionRequest requestDto, Factoring factoring, Banque banque) {

        if (requestDto == null || factoring == null) {
            return null;
        }

        Transaction transaction = new Transaction();

        // libelle/observation/dateEmission : fallback sur le factoring d'origine si non fournis (lot).
        transaction.setReferent(factoring.getId());
        transaction.setLibelle(requestDto.getLibelle() != null ? requestDto.getLibelle() : factoring.getLibelle());
        transaction.setObservation(requestDto.getObservation() != null ? requestDto.getObservation() : factoring.getObservation());
        transaction.setTransactionSource(TransactionSource.FACTORING);
        transaction.setDateEmission(requestDto.getDateEmission() != null ? requestDto.getDateEmission() : factoring.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setBanqueDePrelevement(banque);

        if (factoring.getType() == Type.REMBOURSEMENT) {
            transaction.setDebit(factoring.getMontant());
            transaction.setCredit(BigDecimal.ZERO);

            BigDecimal ttc = factoring.getMontant();
            BigDecimal tauxTVA = BigDecimal.valueOf(requestDto.getTva());
            BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTVA);
            transaction.setTvaDeductible(montantTva);

        } else {
            transaction.setDebit(BigDecimal.ZERO);
            transaction.setCredit(factoring.getMontant());
            BigDecimal ttc = factoring.getMontant();
            BigDecimal tauxTva = BigDecimal.valueOf(requestDto.getTva());
            BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTva);
            transaction.setTvaCollectee(montantTva);
        }

        return transaction;
    }

    public FactoringTransactionResponse toResponseDto(Transaction transaction) {

        if (transaction == null) {
            return null;
        }

        FactoringTransactionResponse responseDto = new FactoringTransactionResponse();

        responseDto.setId(transaction.getId());
        responseDto.setFactoringId(transaction.getReferent());
        responseDto.setBanqueName(transaction.getBanqueDePrelevement().getNom());
        responseDto.setLibelle(transaction.getLibelle());
        responseDto.setObservation(transaction.getObservation());
        responseDto.setDateEmission(transaction.getDateEmission());
        responseDto.setDateValeur(transaction.getDateValeur());
        responseDto.setDateOperations(transaction.getDateOperations());
        responseDto.setDebit(transaction.getDebit());
        responseDto.setCredit(transaction.getCredit());

        return responseDto;
    }
}
