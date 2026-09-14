package staport.finance.gBanque.restitution.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.restitution.entities.Restitution;
import staport.finance.gBanque.restitution.validation.dto.RestitutionTransactionRequestDto;
import staport.finance.gBanque.restitution.validation.dto.RestitutionTransactionResponseDto;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;

/**
 * Mapper de validation pour le module Restitution (architecture.md, Règle 1).
 *
 * <p>Le mapper ne connaît aucun Repository : la {@link Restitution} est résolue et validée par
 * {@code RestitutionValidationService} avant l'appel à {@link #toEntity}. Une restitution est
 * toujours un encaissement (crédit).
 */
@Component
public class RestitutionTransactionMapper {

    public Transaction toEntity(RestitutionTransactionRequestDto requestDto, Restitution restitution, BigDecimal montantTva) {

        if (requestDto == null || restitution == null) {
            return null;
        }

        Transaction transaction = new Transaction();

        // observation/dateEmission/libelle : fallback sur la restitution d'origine si non fournis (lot).
        transaction.setReferent(restitution.getId());
        transaction.setObservation(requestDto.observation() != null ? requestDto.observation() : restitution.getObservation());
        transaction.setDateEmission(requestDto.dateEmission() != null ? requestDto.dateEmission() : restitution.getDateEmission());
        transaction.setDateValeur(requestDto.dateValeur());
        transaction.setDateOperations(requestDto.dateOperation());
        transaction.setLibelle(requestDto.libelle() != null ? requestDto.libelle() : restitution.getLibelle());
        transaction.setTransactionSource(TransactionSource.RESTITUTION);
        transaction.setCredit(restitution.getMontant());
        transaction.setDebit(BigDecimal.ZERO);
        transaction.setBanqueDePrelevement(restitution.getBanque());

        transaction.setTvaCollectee(montantTva);

        return transaction;
    }

    public RestitutionTransactionResponseDto toResponseDto(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        Banque banque = transaction.getBanqueDePrelevement();

        return new RestitutionTransactionResponseDto(
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
                TransactionSource.RESTITUTION
        );
    }
}
