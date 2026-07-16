package staport.finance.gBanque.impayees.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de requête (création/modification) pour une impayée.
 */
public record ImpayeeRequestDto(

        @NotNull(message = "La banque  est obligatoire")
        Long banqueId,

        @NotNull(message = "La date d'émission est obligatoire")
        LocalDate dateEmission,

        @NotNull(message = "Le montant est obligatoire")
        @Positive(message = "Le montant doit être positif")
        BigDecimal montant,

        @NotNull(message = "Le type est obligatoire")
        Type type,

        @NotNull(message = "Le motif (source de transaction) est obligatoire")
        TransactionSource motif,

        @NotBlank(message = "Le libellé est obligatoire")
        String libelle,

        @NotBlank(message = "L'observation est obligatoire")
        String observation

) {}
