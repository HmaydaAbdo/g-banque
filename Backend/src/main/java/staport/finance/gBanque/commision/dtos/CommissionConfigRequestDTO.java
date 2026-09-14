package staport.finance.gBanque.commision.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import staport.finance.gBanque.commision.enums.CommissionCondition;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;

public record CommissionConfigRequestDTO(

        @NotNull(message = "La banque est obligatoire")
        Long banqueId,

        /** Nom de l'organisme — obligatoire uniquement pour SI_MATCH / SI_NON_MATCH. */
        String organismeNom,

        @NotNull(message = "Le montant est obligatoire")
        @Positive(message = "Le montant doit être positif")
        BigDecimal montant,

        @NotNull(message = "Le type de transaction est obligatoire")
        TransactionSource type,

        @NotNull(message = "La condition est obligatoire")
        CommissionCondition condition
) {}
