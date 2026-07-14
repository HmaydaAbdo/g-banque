package staport.finance.gBanque.operationErrone.validation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * DTO de requête pour la validation (rapprochement) d'une opération erronée.
 *
 * <p>Déplacé depuis {@code validation.dtos.operationErrone} vers le module {@code operationErrone}
 * — conforme à {@code architecture.md} Règle 1.
 */
public record OperationErroneTransactionRequestDto(

        @NotNull(message = "L'id de l'opération erronée est obligatoire")
        Long idOperationErrone,

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
