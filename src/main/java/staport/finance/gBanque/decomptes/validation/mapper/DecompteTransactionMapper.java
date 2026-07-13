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
        transaction.setLibelle(requestDto.getLibelle() != null ? requestDto.getLibelle() : decompte.getLibelle());
        transaction.setObservation(requestDto.getObservation() != null ? requestDto.getObservation() : decompte.getObservation());
        transaction.setDateEmission(requestDto.getDateEmission() != null ? requestDto.getDateEmission() : decompte.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setTransactionSource(TransactionSource.DECOMPTES);
        transaction.setBanqueDePrelevement(banque);
        transaction.setDebit(BigDecimal.ZERO);
        transaction.setCredit(decompte.getMontant());

        BigDecimal ttc = decompte.getMontant();
        BigDecimal tauxTva = BigDecimal.valueOf(requestDto.getTva());
        BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTva);
        transaction.setTvaCollectee(montantTva);

        return transaction;
    }

    public DecompteTransactionResponseDto toResponseDto(Transaction transaction) {

        if (transaction == null) {
            return null;
        }

        DecompteTransactionResponseDto responseDto = new DecompteTransactionResponseDto();

        responseDto.setId(transaction.getId());
        responseDto.setDecompteId(transaction.getReferent());
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
