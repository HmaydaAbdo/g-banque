package staport.finance.gBanque.Agios.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de requête pour la validation (rapprochement) d'un agios.
 */
public record AgiosValidationRequest(

        @NotNull(message = "L'id de l'agio est obligatoire")
        Long idAgio,

        @NotNull(message = "La TVA est obligatoire")
        BigDecimal tva,

        @NotNull(message = "La date d'émission est obligatoire")
        LocalDate dateEmission,

        @NotNull(message = "La date de valeur est obligatoire")
        LocalDate dateValeur,

        @NotNull(message = "La date d'opération est obligatoire")
        LocalDate dateOperation,

        @NotBlank(message = "L'observation est obligatoire")
        String observation,

        @NotBlank(message = "Le libellé est obligatoire")
        String libelle,

        @NotNull(message = "Le rapprochementId est obligatoire")
        Long rapprochementId

) {}
