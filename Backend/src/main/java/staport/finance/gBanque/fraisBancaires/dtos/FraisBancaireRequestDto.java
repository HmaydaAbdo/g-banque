package staport.finance.gBanque.fraisBancaires.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de requête (création/modification) pour un frais bancaire.
 */
public record FraisBancaireRequestDto(

        @NotBlank(message = "Le libellé est obligatoire")
        String libelle,

        @NotBlank(message = "L'observation est obligatoire")
        String observation,

        @NotNull(message = "Le montant est obligatoire")
        @Positive(message = "Le montant doit être positif")
        BigDecimal montant,

        @NotNull(message = "La banque est obligatoire")
        Long banqueId,

        @NotNull(message = "La date d'émission est obligatoire")
        LocalDate dateEmission

) {}
