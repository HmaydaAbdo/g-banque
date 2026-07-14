package staport.finance.gBanque.impayees.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.impayees.entities.Impayee;
import staport.finance.gBanque.impayees.validation.dto.ImpayeeTransactionRequestDto;
import staport.finance.gBanque.impayees.validation.dto.ImpayeeTransactionResponseDto;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.utils.TVAUtils;

import java.math.BigDecimal;

/**
 * Mapper de validation pour le module Impayées (architecture.md, Règle 1).
 *
 * <p>Le mapper ne connaît aucun Repository : l'{@link Impayee} est résolu et validé par
 * {@code ImpayeeValidationService} avant l'appel à {@link #toEntity}. Contrairement aux autres
 * modules de cette famille, un impayé peut être un encaissement ou un décaissement selon son
 * {@code Type} — comportement préexistant conservé.
 */
@Component
public class ImpayeeTransactionMapper {

    public Transaction toEntity(ImpayeeTransactionRequestDto requestDto, Impayee impayee) {

        if (requestDto == null || impayee == null) {
            return null;
        }

        Transaction transaction = new Transaction();

        // observation/dateEmission/libelle : fallback sur l'impayé d'origine si non fournis (lot).
        transaction.setReferent(impayee.getId());
        transaction.setObservation(requestDto.observation() != null ? requestDto.observation() : impayee.getObservation());
        transaction.setDateEmission(requestDto.dateEmission() != null ? requestDto.dateEmission() : impayee.getDateEmission());
        transaction.setDateValeur(requestDto.dateValeur());
        transaction.setDateOperations(requestDto.dateOperation());
        transaction.setLibelle(requestDto.libelle() != null ? requestDto.libelle() : impayee.getLibelle());
        transaction.setTransactionSource(TransactionSource.IMPAYEE);
        transaction.setBanqueDePrelevement(impayee.getBanque());

        BigDecimal ttc = impayee.getMontant();
        BigDecimal tauxTva = BigDecimal.valueOf(requestDto.tva());
        BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTva);

        if (impayee.getType() == Type.DECAISSEMENT) {
            transaction.setDebit(impayee.getMontant());
            transaction.setCredit(BigDecimal.ZERO);
            transaction.setTvaDeductible(montantTva);
        } else {
            transaction.setDebit(BigDecimal.ZERO);
            transaction.setCredit(impayee.getMontant());
            transaction.setTvaCollectee(montantTva);
        }

        return transaction;
    }

    public ImpayeeTransactionResponseDto toResponseDto(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        Banque banque = transaction.getBanqueDePrelevement();

        return new ImpayeeTransactionResponseDto(
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
                TransactionSource.IMPAYEE
        );
    }
}
