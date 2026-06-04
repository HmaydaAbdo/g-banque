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
     * <p>Cette méthode ne tient <em>plus</em> compte du premier mois d'entrée :
     * le prorata d'entrée est désormais généré une seule fois à la création de la caution
     * via {@link #calculerProrataEntree(Caution)}.
     *
     * @param caution       caution active avec {@code banque} et {@code nature} chargées
     * @param moisReference mois X de référence
     * @return entité {@link Commission} non persistée
     */
    Commission calculer(Caution caution, YearMonth moisReference);

    /**
     * Calcule le prorata d'entrée à la <strong>création</strong> de la caution.
     *
     * <p>Appelé une seule fois par {@code CautionServiceImpl.creer()} pour les banques
     * dont {@link #supporteProrataEntree()} retourne {@code true}.
     *
     * <p>Comportement par défaut :
     * <ul>
     *   <li>{@code dateEmission = caution.getDateDePrise()}</li>
     *   <li>{@code nombreJours = DAYS.between(dateDePrise, finDuMoisEntrée + 1)}</li>
     *   <li>Même formule et minimum que {@link #calculer}</li>
     * </ul>
     *
     * <p>Les stratégies peuvent surcharger cette méthode si leur logique de prorata
     * diffère (ex. BMCI : minimum différent).
     *
     * @param caution caution nouvellement créée, avec {@code banque} et {@code nature} chargées
     * @return entité {@link Commission} non persistée représentant le prorata d'entrée
     */
    default Commission calculerProrataEntree(Caution caution) {
        // Implémentation par défaut — surchargée par chaque stratégie si besoin
        return calculer(caution, java.time.YearMonth.from(caution.getDateDePrise()));
    }

    /**
     * Indique si cette banque facture le mois <strong>suivant</strong> (M+1) plutôt que
     * le mois courant.
     *
     * <p>Retourne {@code true} pour BMCI et SAHAM BANK — leur commission mensuelle couvre
     * le mois M+1 en utilisant le nombre de jours de ce mois, bien que la {@code dateEmission}
     * reste le dernier jour du mois de référence M.
     *
     * <p>Cette information est utilisée par le batch pour décider si la garde
     * "prorata déjà créé ce mois" doit être appliquée : pour les banques M+1, le prorata
     * (dateEmission = dateDePrise, ex. 19/05) et la commission batch (dateEmission = 31/05)
     * ont des dates différentes — aucun conflit possible. Pour les banques mois courant
     * (AWB, CDM), les deux pourraient couvrir le même mois → la garde s'applique.
     */
    default boolean facturerMoisSuivant() {
        return false;
    }

    /**
     * Indique si une TVA doit être générée séparément pour les commissions de cette banque.
     *
     * <p>Retourne {@code true} pour  les banques génèrent une TVA à 10 %
     * en complément de la commission.
     *
     * <p>Retourner {@code false} pour les banques dont le montant de commission
     * <em>inclut déjà</em> la TVA
     * ne sera créée lors de la génération mensuelle.
     */
    default boolean genererTva() {
        return false;
    }

    /**
     * Indique si cette stratégie doit s'exécuter en début de mois (1er jour)
     * plutôt qu'en fin de mois (dernier jour).
     *
     * <p>Retourne {@code false} pour les banques qui génèrent leurs commissions
     * en fin de mois. Retourner {@code true} pour les banques dont la commission
     * est émise au 1er jour du mois (ex. CDM).
     */
    default boolean executerEnDebutMois() {
        return false;
    }

    /**
     * Indique si cette banque applique un prorata temporis à l'entrée (premier mois).
     *
     * <p>Lorsque {@code true}, la commission du premier mois est calculée uniquement
     * sur les jours allant de la {@code dateDePrise} jusqu'à la fin de ce mois,
     * plutôt que sur le mois entier.
     *
     * <p>Banques concernées : BMCI, CDM, SAHAM BANK ({@code true}).
     * AWB gère le prorata d'entrée dans le batch (pas à la création) :
     * {@code supporteProrataEntree()} reste {@code false} pour AWB.
     */
    default boolean supporteProrataEntree() {
        return false;
    }

    /**
     * Indique si cette banque applique un prorata temporis à la sortie (clôture).
     *
     * <p>Lorsque {@code true}, la commission de clôture est calculée uniquement
     * sur les jours allant du 1er du mois de la main levée jusqu'à la
     * {@code dateDeMainLevee} incluse, plutôt que sur le mois entier.
     *
     * <p>Banques concernées : AWB uniquement ({@code true}).
     * CDM et BMCI facturent le mois entier à la clôture.
     */
    default boolean supporteProrataSortie() {
        return false;
    }

    /**
     * Calcule la commission de clôture (main levée) pour le mois partiel final.
     *
     * <p>Comportement par défaut : mois complet — délègue à {@link #calculer} avec
     * le {@link java.time.YearMonth} de {@code dateDeMainLevee}.
     * CDM et BMCI ({@code supporteProrataSortie = false}) n'ont pas besoin d'override.
     *
     * <p>AWB ({@code supporteProrataSortie = true}) override cette méthode pour
     * facturer uniquement les jours entre le 1er du mois et {@code dateDeMainLevee} inclus.
     *
     * @param caution          caution en cours de clôture, avec banque et nature chargées
     * @param dateDeMainLevee  date effective de la main levée
     * @return entité {@link Commission} non persistée
     */
    default Commission calculerMainLevee(Caution caution, java.time.LocalDate dateDeMainLevee) {
        return calculer(caution, java.time.YearMonth.from(dateDeMainLevee));
    }
}
