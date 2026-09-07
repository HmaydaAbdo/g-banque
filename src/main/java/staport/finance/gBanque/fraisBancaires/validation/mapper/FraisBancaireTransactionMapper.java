package staport.finance.gBanque.fraisBancaires.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.fraisBancaires.entities.FraisBancaire;
import staport.finance.gBanque.fraisBancaires.validation.dto.FraisBancaireTransactionRequestDto;
import staport.finance.gBanque.fraisBancaires.validation.dto.FraisBancaireTransactionResponseDto;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;

/**
 * Mapper de validation pour le module Frais bancaires (architecture.md, Règle 1).
 *
 * <p>Le mapper ne connaît aucun Repository : le {@link FraisBancaire} est résolu et validé par
 * {@code FraisBancaireValidationService} avant l'appel à {@link #toEntity}. Un frais bancaire est
 * toujours un décaissement (débit).
 */
@Component
public class FraisBancaireTransactionMapper {

    public Transaction toEntity(FraisBancaireTransactionRequestDto requestDto, FraisBancaire fraisBancaire, BigDecimal montantTva) {

        if (requestDto == null || fraisBancaire == null) {
            return null;
        }

        Transaction transaction = new Transaction();

        // observation/dateEmission/libelle : fallback sur le frais bancaire d'origine si non fournis (lot).
        transaction.setReferent(fraisBancaire.getId());
        transaction.setObservation(requestDto.observation() != null ? requestDto.observation() : fraisBancaire.getObservation());
        transaction.setDateEmission(requestDto.dateEmission() != null ? requestDto.dateEmission() : fraisBancaire.getDateEmission());
        transaction.setDateValeur(requestDto.dateValeur());
        transaction.setDateOperations(requestDto.dateOperation());
        transaction.setLibelle(requestDto.libelle() != null ? requestDto.libelle() : fraisBancaire.getLibelle());
        transaction.setTransactionSource(TransactionSource.FRAIS_BANCAIRE);
        transaction.setDebit(fraisBancaire.getMontant());
        transaction.setCredit(BigDecimal.ZERO);
        transaction.setBanqueDePrelevement(fraisBancaire.getBanque());

        transaction.setTvaDeductible(montantTva);

        return transaction;
    }

    public FraisBancaireTransactionResponseDto toResponseDto(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        Banque banque = transaction.getBanqueDePrelevement();

        return new FraisBancaireTransactionResponseDto(
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
                TransactionSource.FRAIS_BANCAIRE
        );
    }
}
