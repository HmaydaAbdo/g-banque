package staport.finance.gBanque.decomptes.validation.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * DTO de requête pour la validation (rapprochement) d'un décompte.
 *
 * <p>Déplacé depuis {@code validation.dtos.decomptes} vers le module {@code decomptes} —
 * conforme à {@code architecture.md} Règle 1.
 */
public record DecompteTransactionRequestDto(

        @NotNull(message = "L'ID du décompte est obligatoire.")
        Long decompteId,

        @NotNull(message = "L'ID de la banque est obligatoire.")
        Long banqueId,

        @NotNull(message = "La date d'émission est obligatoire.")
        LocalDate dateEmission,

        @NotNull(message = "La date de valeur est obligatoire.")
        LocalDate dateValeur,

        @NotNull(message = "la date d'opération est obligatoire ")
        LocalDate dateOperation,

        @NotNull(message = "La TVA  est obligatoire.")
        Double tva,

        @NotNull(message = "libelle est obligatoire")
        String libelle,

        @NotNull(message = "L'observation est obligatoire")
        String observation,

        @NotNull(message = "rapprochementId est obligatoire")
        Long rapprochementId

) {}
