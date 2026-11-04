package staport.finance.gBanque.Agios.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import staport.finance.gBanque.Agios.enums.TypeAgios;

/**
 * DTO de création / modification d'un agios.
 */
public record AgiosRequestDto(

        @NotBlank(message = "Le libellé est obligatoire")
        String libelle,

        @NotEmpty(message = "L'observation est obligatoire")
        String observation,

        @NotNull(message = "Le montant est obligatoire")
        @Positive(message = "Le montant doit être positif")
        BigDecimal montant,

        @NotNull(message = "La banque est obligatoire")
        Long banqueId,

        @NotNull(message = "La date d'émission est obligatoire")
        LocalDate dateEmission,
        TypeAgios typeAgios,
        String periodicite,
        Integer periodeNumero,
        Integer annee,
        BigDecimal montantCalcule

) {
    /** Compatibilité des créations ponctuelles et des charges fixes. */
    public AgiosRequestDto(String libelle, String observation, BigDecimal montant,
                           Long banqueId, LocalDate dateEmission) {
        this(libelle, observation, montant, banqueId, dateEmission, null, null, null, null, null);
    }
}
