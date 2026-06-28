package staport.finance.gBanque.leasing.contratCadre.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContratCadreRequestDTO(

        @NotNull(message = "L'ID de l'organisme est obligatoire")
        Long organismeId,

        @NotNull(message = "Le plafond est obligatoire")
        @DecimalMin(value = "0.01", message = "Le plafond doit être supérieur à zéro")
        BigDecimal plafond,

        @NotNull(message = "La date de début est obligatoire")
        LocalDate dateDebut,

        @NotNull(message = "La date de fin est obligatoire")
        LocalDate dateFin

) {}
