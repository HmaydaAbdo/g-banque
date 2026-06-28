package staport.finance.gBanque.leasing.contratLeasing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ContratLeasingPhaseRequestDTO(
        Long id,
        @NotNull(message = "Le numéro de phase est obligatoire")
        @Positive(message = "Le numéro de phase doit être un entier positif.")
        @Min(value = 1, message = "Le numéro de phase doit être supérieur ou égal à 1")
        Integer numeroPhase,
        @NotNull(message = "La durée de la phase en mois est obligatoire")
        @Positive(message = "La durée doit être un nombre de mois positif.")
        @Min(value = 1, message = "La durée de la phase doit être d'au moins 1 mois")
        Integer dureeMois,
        @NotNull(message = "Le montant de loyer TTC pour cette phase est obligatoire")
        @Positive(message = "La mensualité doit être positive.")
        @DecimalMin(value = "0.01", message = "Le montant de loyer TTC doit être supérieur à zéro")
        BigDecimal mensualiteTTC
) {}
