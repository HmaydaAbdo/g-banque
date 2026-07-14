package staport.finance.gBanque.restitution.validation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * DTO de requête pour la validation (rapprochement) d'une restitution.
 *
 * <p>Déplacé depuis {@code validation.dtos.restitutions} vers le module {@code restitution} —
 * conforme à {@code architecture.md} Règle 1.
 */
public record RestitutionTransactionRequestDto(

        @NotNull(message = "L'id de la restitution est obligatoire")
        Long idRestitution,

        @NotNull(message = "La TVA est obligatoire")
        Double tva,

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

        @NotNull(message = "rapprochementId est obligatoire")
        Long rapprochementId

) {}
