package staport.finance.gBanque.leasing.contratCadre.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de mise à jour d'un contrat cadre.
 * L'organisme ne peut pas être modifié après création.
 */
public record ContratCadreUpdateDTO(
        @NotNull @DecimalMin("0.01") BigDecimal plafond,
        @NotNull LocalDate dateDebut,
        @NotNull LocalDate dateFin
) {}
