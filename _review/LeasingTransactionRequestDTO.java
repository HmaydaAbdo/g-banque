package staport.finance.gBanque.leasing.echeancesLeasing.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de requête pour la validation (rapprochement) d'une échéance de leasing.
 * Crée la transaction bancaire associée et marque l'échéance comme PAYE.
 */
public record LeasingTransactionRequestDTO(

        @NotNull(message = "L'ID du contrat de leasing est obligatoire")
        Long contratLeasingId,

        @NotNull(message = "L'ID de l'échéance est obligatoire")
        Long echeanceId,

        LocalDate dateEmission,
        LocalDate dateValeur,
        LocalDate dateOperation,
        String observation,

        /** Taux de TVA choisi, en pourcentage. {@code null} si aucune TVA ou montant saisi directement. */
        @DecimalMin(value = "0", message = "Le taux de TVA ne peut pas être négatif")
        @DecimalMax(value = "100", message = "Le taux de TVA ne peut pas dépasser 100")
        Double tauxTva,

        /** Montant de TVA saisi directement, en MAD. {@code null} si aucune TVA ou taux choisi. */
        @DecimalMin(value = "0.00", message = "Le montant de TVA ne peut pas être négatif")
        BigDecimal montantTva,

        String libelle,

        @NotNull(message = "rapprochementId est obligatoire")
        Long rapprochementId

) {

    /**
     * La TVA est optionnelle pour ce module (contrairement aux autres) : les deux champs peuvent
     * être {@code null} (aucune TVA). En revanche les DEUX ne peuvent pas être renseignés à la fois.
     */
    @JsonIgnore
    @AssertTrue(message = "Renseigner soit un taux de TVA, soit un montant de TVA, jamais les deux")
    public boolean isSaisieTvaCoherente() {
        return tauxTva == null || montantTva == null;
    }
}
