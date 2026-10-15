package staport.finance.gBanque.recapitulatif.dtos.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Plan de trésorerie d'une banque, ou de toutes les banques consolidées ({@code banqueId} null).
 *
 * <p>Toutes les listes « par colonne » suivent l'ordre de {@link PlanTresorerieDto#colonnes()}.
 * {@code soldesFin} est le solde prévu à la fin de chaque colonne (cumulé depuis le solde
 * actuel) ; {@code disponibilites} = {@code soldesFin} + plafond FC. {@code besoinFinancement}
 * est le montant qui manque au point bas une fois la facilité de caisse entièrement utilisée
 * (zéro si la disponibilité reste positive). {@code joursSoldeNegatif} compte les jours de la
 * courbe où le solde prévu est négatif. {@code plusGrosMouvements} liste, du plus gros au plus petit,
 * les cinq plus importants flux à venir (encaissements et décaissements confondus). {@code devise}
 * est celle de la banque, ou de toutes les banques d'un total ; {@code partEstimee} est la part, en
 * pourcentage, des flux qui sont des estimations plutôt que des montants certains.</p>
 */
public record PlanBanqueDto(
        Long banqueId,
        String banqueNom,
        String devise,
        BigDecimal soldeActuel,
        BigDecimal plafondFC,
        List<LignePlanDto> encaissements,
        List<LignePlanDto> decaissements,
        List<BigDecimal> totalEncaissements,
        List<BigDecimal> totalDecaissements,
        List<BigDecimal> soldesFin,
        List<BigDecimal> disponibilites,
        BigDecimal soldeFinal,
        BigDecimal disponibiliteFinale,
        PointBasDto pointBas,
        BigDecimal besoinFinancement,
        int joursSoldeNegatif,
        List<SoldeJourDto> courbe,
        List<MouvementDto> plusGrosMouvements,
        int partEstimee
) {
}
