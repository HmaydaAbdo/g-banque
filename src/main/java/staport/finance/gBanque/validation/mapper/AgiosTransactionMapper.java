package staport.finance.gBanque.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Agios.entities.Agios;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.validation.dtos.agios.AgiosTransactionResponse;
import staport.finance.gBanque.validation.dtos.agios.AgiosValidationRequest;
import staport.finance.gBanque.validation.utils.TVAUtils;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;

@Component
public class AgiosTransactionMapper {

    public Transaction toEntity(AgiosValidationRequest dto, Agios agios) {
        if (dto == null || agios == null) {
            return null;
        }

        Transaction transaction = new Transaction();
        transaction.setReferent(agios.getId());
        transaction.setObservation(dto.observation());
        transaction.setDateEmission(dto.dateEmission());
        transaction.setDateValeur(dto.dateValeur());
        transaction.setDateOperations(dto.dateOperation());
        transaction.setLibelle(dto.libelle());
        transaction.setTransactionSource(TransactionSource.AGIOS);
        transaction.setDebit(agios.getMontant());
        transaction.setCredit(BigDecimal.ZERO);
        transaction.setBanqueDePrelevement(agios.getBanque());

        BigDecimal tva = TVAUtils.calculerTVA(agios.getMontant(), BigDecimal.valueOf(dto.tva().doubleValue()));
        transaction.setTvaDeductible(tva);

        return transaction;
    }

    public AgiosTransactionResponse toResponseDto(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        Banque banque = transaction.getBanqueDePrelevement();

        return new AgiosTransactionResponse(
                transaction.getId(),
                banque != null ? banque.getNom() : null,
                transaction.getLibelle(),
                transaction.getObservation(),
                transaction.getDateEmission(),
                transaction.getDateValeur(),
                transaction.getDateOperations(),
                transaction.getTvaDeductible(),
                transaction.getTvaCollectee(),
                transaction.getDebit(),
                transaction.getCredit()
        );
    }
}
