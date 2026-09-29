package staport.finance.gBanque.LigneFonctionnelle.generalLignes.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Requête de création/mise à jour d'une ligne fonctionnelle générale (ligne mère ou indépendante).
 *
 * @param banqueId                      obligatoire.
 * @param natureLigneFonctionnelleId    obligatoire ; la nature doit avoir une portée GENERALE ou
 *                                      GENERALE_ET_SPECIFIQUE.
 * @param marcheIds                     facultatif — liste des marchés associés (ManyToMany) ; vide
 *                                      pour une ligne générale non liée à un marché précis.
 * @param plafond                       facultatif, doit être strictement positif si renseigné.
 * @param taux                          obligatoire, entre 0 et 100.
 * @param dateDepart                    obligatoire.
 * @param dateFin                       obligatoire, ne peut pas être antérieure à {@code dateDepart}.
 * @param sousLignes                    facultatif — fourni à la création d'une ligne mère avec des IDs
 *                                      nuls ; en modification, null conserve les enfants, tandis qu'une
 *                                      liste (même vide) remplace les enfants d'une nature qui les supporte.
 */
public record LigneFonctionnelleRequest(
        @NotNull(message = "L'ID de la banque est obligatoire.")
        Long banqueId,

        @NotNull(message = "L'ID de la nature de ligne fonctionnelle est obligatoire.")
        Long natureLigneFonctionnelleId,

        List<Long> marcheIds,

        @DecimalMin(value = "0.0", inclusive = false, message = "Le plafond doit être supérieur à zéro.")
        BigDecimal plafond,

        @NotNull(message = "Le taux est obligatoire.")
        @DecimalMin(value = "0.0", message = "Le taux ne peut pas être négatif.")
        @DecimalMax(value = "100.0", message = "Le taux ne peut pas dépasser 100.")
        BigDecimal taux,

        @NotNull(message = "La date de départ est obligatoire.")
        LocalDate dateDepart,

        @NotNull(message = "La date de fin est obligatoire.")
        LocalDate dateFin,

        @Valid
        List<SousLigneRequestDto> sousLignes
) {
    public LigneFonctionnelleRequest {
        if (marcheIds == null) {
            marcheIds = new ArrayList<>();
        }
    }
}
