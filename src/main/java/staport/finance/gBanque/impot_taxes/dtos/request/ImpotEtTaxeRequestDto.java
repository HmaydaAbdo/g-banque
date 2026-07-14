package staport.finance.gBanque.impot_taxes.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import staport.finance.gBanque.impot_taxes.enums.Motif;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de création ou modification d'un impôt/taxe.
 */
public record ImpotEtTaxeRequestDto(

        @NotBlank(message = "Le libellé est obligatoire")
        String libelle,

        @NotBlank(message = "observation est obligatoire")
        String observation,

        @NotNull(message = "Le montant est obligatoire")
        @Positive(message = "Le montant doit être positif")
        BigDecimal montant,

        @NotNull(message = "La banque est obligatoire")
        Long banqueId,

        @NotNull(message = "Le motif est obligatoire")
        Motif motif,

        @NotNull(message = "La date d'émission est obligatoire")
        LocalDate dateEmission

) {}
