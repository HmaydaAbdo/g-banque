package staport.finance.gBanque.Agios.enums;

/**
 * Périodicité du calcul automatique d'agios.
 *
 * <ul>
 *   <li>{@link #TRIMESTRIEL} — la période est un trimestre (1–4).
 *       S'applique à la Facilité de Caisse, au Préfinancement et aux Droits Constatés.</li>
 *   <li>{@link #MENSUEL} — la période est un mois (1–12).
 *       S'applique au Factoring.</li>
 * </ul>
 *
 * <p>Dans {@code AgiosTrimestriel}, le champ {@code trimestre} contient :</p>
 * <ul>
 *   <li>1–4  pour les types {@link #TRIMESTRIEL}</li>
 *   <li>1–12 pour les types {@link #MENSUEL}</li>
 * </ul>
 */
public enum PeriodeCalculAgios {
    TRIMESTRIEL,
    MENSUEL
}
