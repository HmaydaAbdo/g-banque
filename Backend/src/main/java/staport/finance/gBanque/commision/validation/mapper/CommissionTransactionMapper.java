package staport.finance.gBanque.commision.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.commision.entities.Commission;
import staport.finance.gBanque.commision.validation.dto.CommissionTransactionRequestDto;
import staport.finance.gBanque.commision.validation.dto.CommissionTransactionResponseDto;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;

/**
 * Mapper de validation pour le module Commission (architecture.md, Règle 1).
 *
 * <p>Le mapper ne connaît aucun Repository : la {@link Commission} et la {@link Banque} sont
 * résolues et validées par {@code CommissionValidationService} avant l'appel à {@link #toEntity}.
 * Une commission est toujours un décaissement (débit).
 */
@Component
public class CommissionTransactionMapper {

    public Transaction toEntity(CommissionTransactionRequestDto requestDto, Commission commission, BigDecimal montantTva) {

        if (requestDto == null || commission == null) {
            return null;
        }

        Transaction transaction = new Transaction();

        // observation/dateEmission/libelle : fallback sur la commission d'origine si non fournis (lot).
        transaction.setReferent(commission.getId());
        transaction.setObservation(requestDto.observation() != null ? requestDto.observation() : commission.getObservation());
        transaction.setDateEmission(requestDto.dateEmission() != null ? requestDto.dateEmission() : commission.getDateEmission());
        transaction.setDateValeur(requestDto.dateValeur());
        transaction.setDateOperations(requestDto.dateOperation());
        transaction.setLibelle(requestDto.libelle() != null ? requestDto.libelle() : commission.getLibelle());
        transaction.setTransactionSource(TransactionSource.COMMISSION);
        transaction.setDebit(commission.getMontant());
        transaction.setCredit(BigDecimal.ZERO);
        transaction.setBanqueDePrelevement(commission.getBanque());

        transaction.setTvaDeductible(montantTva);

        return transaction;
    }

    public CommissionTransactionResponseDto toResponseDto(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        Banque banque = transaction.getBanqueDePrelevement();

        return new CommissionTransactionResponseDto(
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
                TransactionSource.COMMISSION
        );
    }
}
