package staport.finance.gBanque.operationErrone.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de requête (création/modification) pour une opération erronée.
 */
public record OperationErroneRequestDto(

        @NotNull(message = "Le montant est obligatoire")
        @Positive(message = "Le montant doit être positif")
        BigDecimal montant,

        @NotNull(message = "La banque est obligatoire")
        Long banqueId,

        @NotNull(message = "La date d'émission est obligatoire")
        LocalDate dateEmission,

        @NotNull(message = "Le motif est obligatoire")
        TransactionSource motif,

        @NotBlank(message = "Le libellé est obligatoire")
        String libelle,

        @NotBlank(message = "L'observation est obligatoire")
        String observation

) {}
