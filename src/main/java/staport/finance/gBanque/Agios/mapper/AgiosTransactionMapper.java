package staport.finance.gBanque.Agios.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Agios.dtos.AgiosTransactionResponse;
import staport.finance.gBanque.Agios.dtos.AgiosValidationRequest;
import staport.finance.gBanque.Agios.entities.Agios;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.utils.TVAUtils;

import java.math.BigDecimal;

@Component
public class AgiosTransactionMapper {

    public Transaction toEntity(AgiosValidationRequest dto, Agios agios) {
        if (dto == null || agios == null) {
            return null;
        }

        Transaction transaction = new Transaction();
        // observation/dateEmission/libelle : fallback sur l'agios d'origine si non fournis (lot).
        transaction.setReferent(agios.getId());
        transaction.setObservation(dto.observation() != null ? dto.observation() : agios.getObservation());
        transaction.setDateEmission(dto.dateEmission() != null ? dto.dateEmission() : agios.getDateEmission());
        transaction.setDateValeur(dto.dateValeur());
        transaction.setDateOperations(dto.dateOperation());
        transaction.setLibelle(dto.libelle() != null ? dto.libelle() : agios.getLibelle());
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
