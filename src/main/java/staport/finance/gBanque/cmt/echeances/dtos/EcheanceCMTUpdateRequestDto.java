package staport.finance.gBanque.cmt.echeances.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de mise à jour d'une échéance CMT.
 * Applicable uniquement aux échéances au statut {@code PREVU}.
 * Réservé aux administrateurs système.
 */
public record EcheanceCMTUpdateRequestDto(

        @NotNull(message = "La date d'échéance est obligatoire")
        LocalDate dateEcheance,

        @NotNull(message = "Le montant est obligatoire")
        @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à zéro")
        BigDecimal montant,

        LocalDate dateOperation,

        LocalDate dateValeur,

        String observation,

        String libelle
) {}
