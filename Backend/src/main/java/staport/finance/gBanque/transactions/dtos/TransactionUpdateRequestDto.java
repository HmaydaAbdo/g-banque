package staport.finance.gBanque.transactions.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Requête de modification d'une transaction déjà validée — mêmes champs, et la même politique
 * TVA (taux OU montant, résolu serveur via {@code TvaResolver}), que le dialogue de validation
 * générique (architecture.md, Règle 2) : {@code dateEmission} et la banque de prélèvement ne sont
 * volontairement pas éditables ici. La propagation vers l'entité source (dates, libellé,
 * observation) est automatique — voir {@code TransactionSyncHandler}.
 */
public record TransactionUpdateRequestDto (

    @NotNull(message = "La date de valeur est obligatoire")
    LocalDate dateValeur,

    @NotNull(message = "La date d'opération est obligatoire")
    LocalDate dateOperation,

    @NotBlank(message = "Le libellé est obligatoire")
    String libelle,

    @NotBlank(message = "L'observation est obligatoire")
    String observation,

    Double tauxTva,

    BigDecimal montantTva

) {
    /**
     * Exactement l'un des deux — {@code tauxTva} ou {@code montantTva} — doit être renseigné,
     * jamais les deux ni aucun. Même contrat que {@code TvaResolver}/les DTO de validation.
     */
    @JsonIgnore
    @AssertTrue(message = "Renseigner soit un taux de TVA, soit un montant de TVA, mais pas les deux")
    public boolean isSaisieTvaCoherente() {
        return (tauxTva == null) ^ (montantTva == null);
    }
}
