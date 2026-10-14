package staport.finance.gBanque.Bank.dtos.response;

import staport.finance.gBanque.Bank.enums.Devise;
import staport.finance.gBanque.recapitulatif.enums.PeriodeRecap;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Réponse agrégée pour le tableau de bord Finance (accueil).
 *
 * <p>{@code disponibiliteFC} = soldeActuel + plafondFC (situation du jour, pas de projection)
 * — même formule que {@code ResumeSectionDto.disponibilite()} (RecapGeneralDetailleServiceImpl).
 * Toujours renseignée (jamais null), même quand {@code plafondFC} est null (aucune ligne FC
 * active) — traitée alors comme 0.</p>
 *
 * <p>Projection à l'horizon {@code periode} (fin de mois par défaut, ou fin de trimestre, choisi
 * sur l'accueil depuis le 14/10/2026) : {@code encoursPlus}/{@code encoursMoins} = encours non
 * validés dont la date de référence est au plus tard {@code dateHorizon} ;
 * {@code soldePrevu} = soldeActuel + encoursPlus − encoursMoins ;
 * {@code disponibilitePrevue} = soldePrevu + plafond FC.</p>
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
        PeriodeRecap periode,
        LocalDate  dateHorizon,
        BigDecimal encoursPlus,
        BigDecimal encoursMoins,
        BigDecimal soldePrevu,
        BigDecimal disponibilitePrevue
) {}
