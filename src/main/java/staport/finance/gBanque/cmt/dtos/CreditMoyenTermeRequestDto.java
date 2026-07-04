package staport.finance.gBanque.cmt.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO utilisé pour la création ou la mise à jour d'un crédit moyen terme.
 * Contient les champs nécessaires à la logique métier et les validations de base.
 */
public record CreditMoyenTermeRequestDto(

        @NotNull(message = "L'ID de la banque est obligatoire")
        Long banqueId,

        @NotBlank(message = "La désignation est obligatoire")
        String designation,

        @NotNull(message = "Le nominal est obligatoire")
        @DecimalMin(value = "0.01", message = "Le nominal doit être supérieur à zéro")
        BigDecimal nominal,

        @NotNull(message = "La périodicité est obligatoire")
        @Positive(message = "La périodicité doit être positive")
        Integer periodicite,

        @NotNull(message = "La durée est obligatoire")
        @Positive(message = "La durée doit être un entier positif")
        Integer duree,

        @NotNull(message = "Le montant de l'échéance est obligatoire")
        @DecimalMin(value = "0.01", message = "Le montant de l'échéance doit être supérieur à zéro")
        BigDecimal echeance,

        @NotNull(message = "Le taux est obligatoire")
        @DecimalMin(value = "0.01", message = "Le taux doit être supérieur à zéro")
        BigDecimal taux,

        @NotNull(message = "La date de début est obligatoire")
        LocalDate dateDebut
) {
}
