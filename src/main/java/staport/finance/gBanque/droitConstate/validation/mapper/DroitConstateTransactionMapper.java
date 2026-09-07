package staport.finance.gBanque.droitConstate.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.droitConstate.validation.dto.DroitConstateTransactionRequest;
import staport.finance.gBanque.droitConstate.validation.dto.DroitConstateTransactionResponse;
import staport.finance.gBanque.prefinancement.enums.Type;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;

/**
 * Mapper de validation pour le module Droit Constaté (architecture.md, Règle 1).
 *
 * <p>Le mapper ne connaît aucun Repository : le {@link DroitConstate} et la {@link Banque} sont
 * résolus et validés par {@code DroitConstateValidationService} avant l'appel à
 * {@link #toEntity}. La {@code banque} passée ici est celle du droit constaté (champ direct de
 * l'entité, indépendant de {@code marche.banque}), déjà résolue par le Service.
 */
@Component
public class DroitConstateTransactionMapper {

    public Transaction toEntity(DroitConstateTransactionRequest requestDto, DroitConstate droitConstate, Banque banque, BigDecimal montantTva) {

        if (requestDto == null || droitConstate == null) {
            return null;
        }

        Transaction transaction = new Transaction();

        // libelle/observation/dateEmission : fallback sur le droit constaté d'origine si non fournis (lot).
        transaction.setReferent(droitConstate.getId());
        transaction.setLibelle(requestDto.getLibelle() != null ? requestDto.getLibelle() : droitConstate.getLibelle());
        transaction.setObservation(requestDto.getObservation() != null ? requestDto.getObservation() : droitConstate.getObservation());
        transaction.setTransactionSource(TransactionSource.DROIT_CONSTATE);
        transaction.setDateEmission(requestDto.getDateEmission() != null ? requestDto.getDateEmission() : droitConstate.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setBanqueDePrelevement(banque);

        if (droitConstate.getType() == Type.REMBOURSEMENT) {
            transaction.setDebit(droitConstate.getMontant());
            transaction.setCredit(BigDecimal.ZERO);
            transaction.setTvaDeductible(montantTva);

        } else {
            transaction.setDebit(BigDecimal.ZERO);
            transaction.setCredit(droitConstate.getMontant());
            transaction.setTvaCollectee(montantTva);
        }

        return transaction;
    }

    public DroitConstateTransactionResponse toResponseDto(Transaction transaction) {

        if (transaction == null) {
            return null;
        }

        DroitConstateTransactionResponse responseDto = new DroitConstateTransactionResponse();

        responseDto.setId(transaction.getId());
        responseDto.setDroitConstateId(transaction.getReferent());
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
