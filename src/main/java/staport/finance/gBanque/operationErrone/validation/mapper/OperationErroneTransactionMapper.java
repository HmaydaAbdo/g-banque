package staport.finance.gBanque.operationErrone.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.operationErrone.entities.OperationErrone;
import staport.finance.gBanque.operationErrone.validation.dto.OperationErroneTransactionRequestDto;
import staport.finance.gBanque.operationErrone.validation.dto.OperationErroneTransactionResponseDto;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.utils.TVAUtils;

import java.math.BigDecimal;

/**
 * Mapper de validation pour le module Opérations erronées (architecture.md, Règle 1).
 *
 * <p>Le mapper ne connaît aucun Repository : l'{@link OperationErrone} est résolue et validée par
 * {@code OperationErroneValidationService} avant l'appel à {@link #toEntity}. Une opération
 * erronée est toujours un décaissement (débit).
 */
@Component
public class OperationErroneTransactionMapper {

    public Transaction toEntity(OperationErroneTransactionRequestDto requestDto, OperationErrone operationErrone) {

        if (requestDto == null || operationErrone == null) {
            return null;
        }

        Transaction transaction = new Transaction();

        // observation/dateEmission/libelle : fallback sur l'opération erronée d'origine si non fournis (lot).
        transaction.setReferent(operationErrone.getId());
        transaction.setObservation(requestDto.observation() != null ? requestDto.observation() : operationErrone.getObservation());
        transaction.setDateEmission(requestDto.dateEmission() != null ? requestDto.dateEmission() : operationErrone.getDateEmission());
        transaction.setDateValeur(requestDto.dateValeur());
        transaction.setDateOperations(requestDto.dateOperation());
        transaction.setLibelle(requestDto.libelle() != null ? requestDto.libelle() : operationErrone.getLibelle());
        transaction.setTransactionSource(TransactionSource.OPERATION_ERRONE);
        transaction.setDebit(operationErrone.getMontant());
        transaction.setCredit(BigDecimal.ZERO);
        transaction.setBanqueDePrelevement(operationErrone.getBanque());

        BigDecimal ttc = operationErrone.getMontant();
        BigDecimal tauxTva = BigDecimal.valueOf(requestDto.tva());
        BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTva);
        transaction.setTvaDeductible(montantTva);

        return transaction;
    }

    public OperationErroneTransactionResponseDto toResponseDto(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        Banque banque = transaction.getBanqueDePrelevement();

        return new OperationErroneTransactionResponseDto(
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
                TransactionSource.OPERATION_ERRONE
        );
    }
}
