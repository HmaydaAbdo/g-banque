package staport.finance.gBanque.prefinancement.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Type;
import staport.finance.gBanque.prefinancement.validation.dto.PrefinancementTransactionRequest;
import staport.finance.gBanque.prefinancement.validation.dto.PrefinancementTransactionResponse;
import staport.finance.gBanque.validation.utils.TVAUtils;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;

/**
 * Mapper de validation pour le module Préfinancement (architecture.md, Règle 1).
 *
 * <p>Le mapper ne connaît aucun Repository : le {@link Prefinancement} et la {@link Banque} sont
 * résolus et validés par {@code PrefinancementValidationService} avant l'appel à {@link #toEntity}.
 */
@Component
public class PrefinancementTransactionMapper {

    public Transaction toEntity(PrefinancementTransactionRequest requestDto, Prefinancement prefinancement, Banque banque) {

        if (requestDto == null || prefinancement == null) {
            return null;
        }

        Transaction transaction = new Transaction();

        // libelle/observation/dateEmission : fallback sur le préfinancement d'origine si non fournis (lot).
        transaction.setReferent(prefinancement.getId());
        transaction.setLibelle(requestDto.getLibelle() != null ? requestDto.getLibelle() : prefinancement.getLibelle());
        transaction.setDateEmission(requestDto.getDateEmission() != null ? requestDto.getDateEmission() : prefinancement.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setBanqueDePrelevement(banque);
        transaction.setObservation(requestDto.getObservation() != null ? requestDto.getObservation() : prefinancement.getObservation());
        transaction.setTransactionSource(TransactionSource.PREFINANCEMENT);

        if (prefinancement.getType() == Type.REMBOURSEMENT) {
            transaction.setDebit(prefinancement.getMontant());
            transaction.setCredit(BigDecimal.ZERO);

            BigDecimal ttc = prefinancement.getMontant();
            BigDecimal tauxTva = BigDecimal.valueOf(requestDto.getTva());
            BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTva);
            transaction.setTvaDeductible(montantTva);

        } else {
            transaction.setDebit(BigDecimal.ZERO);
            transaction.setCredit(prefinancement.getMontant());

            BigDecimal ttc = prefinancement.getMontant();
            BigDecimal tauxTva = BigDecimal.valueOf(requestDto.getTva());
            BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTva);
            transaction.setTvaCollectee(montantTva);
        }

        return transaction;
    }

    public PrefinancementTransactionResponse toResponseDto(Transaction transaction) {

        if (transaction == null) {
            return null;
        }

        PrefinancementTransactionResponse responseDto = new PrefinancementTransactionResponse();

        responseDto.setId(transaction.getId());
        responseDto.setPrefinancementId(transaction.getReferent());
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
