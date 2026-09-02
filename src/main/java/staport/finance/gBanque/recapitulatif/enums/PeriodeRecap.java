package staport.finance.gBanque.recapitulatif.enums;

/**
 * Horizon de calcul retenu pour le Récapitulatif global (mono-banque "Journal banque" et
 * multi-banques) — choisi par l'utilisateur au moment de l'export PDF (retour utilisateur du
 * 31/08/2026 : la version "fin de mois" existante ne permettait pas de voir la situation
 * strictement à date, sans les encaissements/décaissements prévus le reste du mois).
 *
 * <p>Ne s'applique qu'à ce rapport — le Récapitulatif général détaillé
 * ({@code RecapGeneralDetailleServiceImpl}) affiche déjà les deux horizons côte à côte
 * (résumé du jour + récapitulatif mensuel), pas besoin d'un choix là-bas.</p>
 */
public enum PeriodeRecap {

    /** N'inclut que les encours dont la date de référence est aujourd'hui ou avant. */
    A_CE_JOUR,

    /**
     * Inclut les encours dont la date de référence va jusqu'à la fin du mois en cours —
     * comportement historique de ce rapport, valeur par défaut si le paramètre est omis.
     */
    FIN_DE_MOIS
}
