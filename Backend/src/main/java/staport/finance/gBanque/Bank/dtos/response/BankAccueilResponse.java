package staport.finance.gBanque.Bank.dtos.response;

import staport.finance.gBanque.Bank.enums.Devise;
import java.math.BigDecimal;

/**
 * Réponse agrégée pour le tableau de bord Finance (accueil).
 *
 * <p>{@code disponibiliteFC} = soldeActuel + plafondFC (situation du jour, pas de projection
 * fin de mois) — même formule que {@code ResumeSectionDto.disponibilite()}
 * (RecapGeneralDetailleServiceImpl), calculée ici séparément car cette carte n'interroge pas
 * les encours prévisionnels (choix utilisateur du 31/08/2026 : disponibilité "actuelle" sur la
 * carte, pas "fin de mois" comme dans le récapitulatif global). Toujours renseignée (jamais
 * null), même quand {@code plafondFC} est null (aucune ligne FC active) — traitée alors comme 0.
 */
public record BankAccueilResponse(
        Long       banqueId,
        String     logoBase64,
        String     nom,
        BigDecimal soldeActuel,
        BigDecimal soldeDebut,
        BigDecimal plafondFC,
        BigDecimal disponibiliteFC,
        Devise     devise,
        long       lcnsEchusCount,
        long       virementsEchusCount,
        BigDecimal encoursPlus,
        BigDecimal encoursMoins,
        BigDecimal soldeFinDeMois,
        BigDecimal disponibiliteFinDeMois
) {}
