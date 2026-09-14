package staport.finance.gBanque.provision.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import staport.finance.gBanque.transactions.enums.SensOperation;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de requête (création/modification) pour une provision.
 */
public record ProvisionRequestDto(

        @NotNull(message = "Le montant est obligatoire")
        @Positive(message = "Le montant doit être positif")
        BigDecimal montant,

        @NotNull(message = "La banque est obligatoire")
        Long banqueId,

        @NotNull(message = "La date d'émission est obligatoire")
        LocalDate dateEmission,

        @NotNull(message = "Le motif est obligatoire")
        TransactionSource motif,

        @NotNull(message = "Le sens (ENCAISSEMENT/DECAISSEMENT) est obligatoire")
        SensOperation sens,

        @NotBlank(message = "Le libellé est obligatoire")
        String libelle,

        @NotBlank(message = "L'observation est obligatoire")
        String observation

) {}
