package staport.finance.gBanque.commision.validation.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de requête pour la validation (rapprochement) d'une commission.
 *
 * <p>Déplacé depuis {@code validation.dtos.commision} vers le module {@code commision} —
 * conforme à {@code architecture.md} Règle 1.
 */
public record CommissionTransactionRequestDto(

        @NotNull(message = "L'id de la commission est obligatoire")
        Long idCommission,

        @DecimalMin(value = "0", message = "Le taux de TVA ne peut pas être négatif")
        @DecimalMax(value = "100", message = "Le taux de TVA ne peut pas dépasser 100")
        Double tauxTva,

        @DecimalMin(value = "0.00", message = "Le montant de TVA ne peut pas être négatif")
        BigDecimal montantTva,

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

) {

    /**
     * Exactement l'une des deux saisies de TVA doit être renseignée — vérifié dès la couche
     * Jakarta, avant d'atteindre le service. {@code TvaResolver} refait la vérification pour les
     * appels internes (validation en lot) qui ne passent pas par la validation du contrôleur.
     */
    @JsonIgnore
    @AssertTrue(message = "Renseigner soit un taux de TVA, soit un montant de TVA, mais pas les deux")
    public boolean isSaisieTvaCoherente() {
        return (tauxTva == null) ^ (montantTva == null);
    }
}
