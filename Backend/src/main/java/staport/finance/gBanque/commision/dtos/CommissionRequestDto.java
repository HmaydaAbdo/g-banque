package staport.finance.gBanque.commision.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de requête (création/modification) pour une commission.
 */
public record CommissionRequestDto(

        @NotBlank(message = "Le libellé est obligatoire")
        String libelle,

        @NotEmpty(message = "L'observation est obligatoire")
        String observation,

        @NotNull(message = "Le montant est obligatoire")
        @Positive(message = "Le montant doit être positif")
        BigDecimal montant,

        @NotNull(message = "La banque est obligatoire")
        Long banqueId,

        @NotNull(message = "L’opération est obligatoire")
        TransactionSource operation,

        @NotNull(message = "La date d’émission est obligatoire")
        LocalDate dateEmission

) {}
