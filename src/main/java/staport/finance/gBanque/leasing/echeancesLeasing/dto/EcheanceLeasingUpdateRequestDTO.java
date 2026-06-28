package staport.finance.gBanque.leasing.echeancesLeasing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de mise à jour d'une échéance de leasing.
 * Applicable uniquement aux échéances au statut {@code PREVU}.
 * Réservé aux administrateurs système.
 */
public record EcheanceLeasingUpdateRequestDTO(

        @NotNull(message = "La date d'échéance est obligatoire")
        LocalDate dateEcheance,

        @NotNull(message = "Le montant TTC est obligatoire")
        @DecimalMin(value = "0.01", message = "Le montant TTC doit être supérieur à zéro")
        BigDecimal montantTTC,

        LocalDate dateOperation,

        LocalDate dateValeur,

        String observation,

        String libelle
) {}
