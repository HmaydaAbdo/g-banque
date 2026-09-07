package staport.finance.gBanque.provision.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.provision.entities.Provision;
import staport.finance.gBanque.provision.validation.dto.ProvisionTransactionRequestDto;
import staport.finance.gBanque.provision.validation.dto.ProvisionTransactionResponseDto;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.SensOperation;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;

/**
 * Mapper de validation pour le module Provision (architecture.md, Règle 1).
 *
 * <p>Le mapper ne connaît aucun Repository : la {@link Provision} est résolue et validée par
 * {@code ProvisionValidationService} avant l'appel à {@link #toEntity}.
 *
 * <p>Différence avec {@code FraisBancaireTransactionMapper}/{@code OperationErroneTransactionMapper}
 * (toujours un décaissement) : le sens de la provision détermine si le montant alimente le débit
 * (décaissement, TVA déductible) ou le crédit (encaissement, TVA collectée) de la transaction
 * générée — même calcul que {@code RecapitulatifEncoursMapper}.
 */
@Component
public class ProvisionTransactionMapper {

    public Transaction toEntity(ProvisionTransactionRequestDto requestDto, Provision provision, BigDecimal montantTva) {

        if (requestDto == null || provision == null) {
            return null;
        }

        Transaction transaction = new Transaction();

        transaction.setReferent(provision.getId());
        transaction.setObservation(requestDto.observation() != null ? requestDto.observation() : provision.getObservation());
        transaction.setDateEmission(requestDto.dateEmission() != null ? requestDto.dateEmission() : provision.getDateEmission());
        transaction.setDateValeur(requestDto.dateValeur());
        transaction.setDateOperations(requestDto.dateOperation());
        transaction.setLibelle(requestDto.libelle() != null ? requestDto.libelle() : provision.getLibelle());
        transaction.setTransactionSource(TransactionSource.PROVISION);
        transaction.setBanqueDePrelevement(provision.getBanque());

        if (provision.getSens() == SensOperation.DECAISSEMENT) {
            transaction.setDebit(provision.getMontant());
            transaction.setCredit(BigDecimal.ZERO);
            transaction.setTvaDeductible(montantTva);
            transaction.setTvaCollectee(BigDecimal.ZERO);
        } else {
            transaction.setCredit(provision.getMontant());
            transaction.setDebit(BigDecimal.ZERO);
            transaction.setTvaCollectee(montantTva);
            transaction.setTvaDeductible(BigDecimal.ZERO);
        }

        return transaction;
    }

    public ProvisionTransactionResponseDto toResponseDto(Transaction transaction, SensOperation sens) {
        if (transaction == null) {
            return null;
        }

        Banque banque = transaction.getBanqueDePrelevement();

        return new ProvisionTransactionResponseDto(
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
                TransactionSource.PROVISION,
                sens
        );
    }
}
