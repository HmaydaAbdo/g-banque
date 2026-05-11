package staport.finance.gBanque.cautions.commissions.strategy;

import staport.finance.gBanque.cautions.entities.Caution;
import staport.finance.gBanque.commision.entities.Commission;

import java.time.YearMonth;

/**
 * Contrat Strategy pour le calcul des commissions sur cautions.
 *
 * <p>Chaque banque peut avoir sa propre logique de calcul — taux, base de calcul,
 * arrondi, etc. L'ajout d'une nouvelle banque se fait en créant une nouvelle
 * implémentation de cette interface annotée {@code @Component}, sans modifier
 * le code existant (Open/Closed Principle).
 *
 * l'implémentation correcte à partir du nom de la banque portée par la caution.
 *
 * <p>Contrat :
 * <ul>
 *   <li>La caution passée en paramètre est active ({@code actif = true})</li>
 *   <li>La caution est chargée avec ses associations {@code banque} et {@code nature}</li>
 *   <li>La méthode retourne une entité {@link Commission} non persistée,
 *       prête à être sauvegardée par le service appelant</li>
 * </ul>
 */
public interface CommissionCautionStrategie {

    /**
     * Retourne le nom exact de la banque pour laquelle cette stratégie s'applique.
     *
     * <p>La comparaison dans la factory est insensible à la casse.
     * Exemples valides : {@code "BMCI"}, {@code "CIH"}, {@code "SGMB"}.
     *
     * @return nom de la banque (non nul, non vide)
     */
    String nomBanque();

    /**
     * Calcule et construit la commission mensuelle pour une caution active.
     *
     * <p>La commission générée cible le mois {@code moisReference + 1} :
     * <ul>
     *   <li>date d'émission = dernier jour du mois cible</li>
     *   <li>libellé = nature.label + " " + numeroCaution</li>
     *   <li>motif (observation) = "CAUTION"</li>
     * </ul>
     *
     * @param caution       caution active avec {@code banque} et {@code nature} chargées
     * @param moisReference mois X de référence — la commission porte sur le mois X+1
     * @return entité {@link Commission} non persistée
     */
    Commission calculer(Caution caution, YearMonth moisReference);

    /**
     * Indique si une TVA doit être générée séparément pour les commissions de cette banque.
     *
     * <p>Retourne {@code true} par défaut — toutes les banques génèrent une TVA à 10 %
     * en complément de la commission.
     *
     * <p>Retourner {@code false} pour les banques dont le montant de commission
     * <em>inclut déjà</em> la TVA (ex. CDM) : aucune entrée {@code ImpotEtTaxe}
     * ne sera créée lors de la génération mensuelle.
     */
    default boolean genererTva() {
        return true;
    }

    /**
     * Indique si cette stratégie doit s'exécuter en début de mois (1er jour)
     * plutôt qu'en fin de mois (dernier jour).
     *
     * <p>Retourne {@code false} par défaut — la plupart des banques (AWB, BMCI)
     * génèrent leurs commissions en fin de mois.
     *
     * <p>Retourner {@code true} pour les banques dont la commission est émise
     * au 1er jour du mois (ex. CDM).
     */
    default boolean executerEnDebutMois() {
        return false;
    }
}
