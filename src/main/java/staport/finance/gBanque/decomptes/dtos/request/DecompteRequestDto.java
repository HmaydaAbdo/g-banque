package staport.finance.gBanque.decomptes.dtos.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de création ou modification d'un décompte.
 */
public record DecompteRequestDto(

        @NotNull(message = "Marche ID cannot be null")
        Long marcheId,

        @NotNull(message = "banqueEncaissementId cannot be null")
        Long banqueEncaissementId,

        @NotNull(message = "Date of emission cannot be null")
        LocalDate dateEmission,

        @NotNull(message = "Montant cannot be null")
        @DecimalMin(value = "0.01", message = "Montant must be a positive value")
        BigDecimal montant,

        @NotNull(message = "Libelle cannot be null")
        @NotBlank(message = "Libelle cannot be empty")
        String libelle

) {}
