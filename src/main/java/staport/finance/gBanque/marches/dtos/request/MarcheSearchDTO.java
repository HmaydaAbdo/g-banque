package staport.finance.gBanque.marches.dtos.request;

import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Critères de recherche des marchés. Tous les champs sont optionnels.
 *
 * <p>Les 2 paires de dates (début/achèvement des travaux) permettent une recherche par période
 * indépendante sur chacune — bornes inclusives, combinables entre elles et avec les autres
 * critères.
 */
public record MarcheSearchDTO(

        @Size(max = 255, message = "Le nom doit avoir au maximum 255 caracteres")
        String nom,

        @Size(max = 50, message = "Le numero doit avoir au maximum 50 caracteres")
        String numero,

        @Size(max = 255, message = "Le maitre d'ouvrage doit avoir au maximum 255 caracteres")
        String maitreDouvrage,

        Long banqueId,

        /** null = tous statuts, true = actifs uniquement, false = archivés uniquement. */
        Boolean actif,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateDebutDesTravauxFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateDebutDesTravauxTo,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateDAchievementDesTravauxFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateDAchievementDesTravauxTo
) {
}
