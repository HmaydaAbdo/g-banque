package staport.finance.gBanque.decomptes.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.decomptes.entities.Decompte;
import staport.finance.gBanque.decomptes.validation.dto.DecompteTransactionRequestDto;
import staport.finance.gBanque.decomptes.validation.dto.DecompteTransactionResponseDto;
import staport.finance.gBanque.validation.utils.TVAUtils;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;

/**
 * Mapper de validation pour le module Décomptes (architecture.md, Règle 1).
 *
 * <p>Le mapper ne connaît aucun Repository : le {@link Decompte} et la {@link Banque} sont
 * résolus et validés par {@code DecompteValidationService} avant l'appel à {@link #toEntity}.
 * Un décompte est toujours un encaissement (crédit) — pas de distinction de {@code Type} comme
 * pour Préfinancement/Factoring/Droit Constaté.
 */
@Component
public class DecompteTransactionMapper {

    public Transaction toEntity(DecompteTransactionRequestDto requestDto, Decompte decompte, Banque banque) {

        if (requestDto == null || decompte == null) {
            return null;
        }

        Transaction transaction = new Transaction();

        // libelle/observation/dateEmission : fallback sur le décompte d'origine si non fournis (lot).
        transaction.setReferent(decompte.getId());
        transaction.setLibelle(requestDto.libelle() != null ? requestDto.libelle() : decompte.getLibelle());
        transaction.setObservation(requestDto.observation() != null ? requestDto.observation() : decompte.getObservation());
        transaction.setDateEmission(requestDto.dateEmission() != null ? requestDto.dateEmission() : decompte.getDateEmission());
        transaction.setDateValeur(requestDto.dateValeur());
        transaction.setDateOperations(requestDto.dateOperation());
        transaction.setTransactionSource(TransactionSource.DECOMPTES);
        transaction.setBanqueDePrelevement(banque);
        transaction.setDebit(BigDecimal.ZERO);
        transaction.setCredit(decompte.getMontant());

        BigDecimal ttc = decompte.getMontant();
        BigDecimal tauxTva = BigDecimal.valueOf(requestDto.tva());
        BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTva);
        transaction.setTvaCollectee(montantTva);

        return transaction;
    }

    public DecompteTransactionResponseDto toResponseDto(Transaction transaction) {

        if (transaction == null) {
            return null;
        }

        Banque banque = transaction.getBanqueDePrelevement();

        return new DecompteTransactionResponseDto(
                transaction.getId(),
                transaction.getReferent(),
                banque != null ? banque.getNom() : null,
                transaction.getLibelle(),
                transaction.getObservation(),
                transaction.getDateEmission(),
                transaction.getDateValeur(),
                transaction.getDateOperations(),
                transaction.getDebit(),
                transaction.getCredit(),
                TransactionSource.DECOMPTES
        );
    }
}
