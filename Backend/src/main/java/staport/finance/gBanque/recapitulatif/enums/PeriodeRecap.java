package staport.finance.gBanque.recapitulatif.enums;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;

/**
 * Horizon de projection des encours prévisionnels : jusqu'à quelle date les encaissements et
 * décaissements non encore validés sont pris en compte.
 *
 * <p>Un seul horizon partagé par tous les écrans et rapports qui projettent la trésorerie : cartes
 * de l'accueil, écran Récapitulatifs, Récapitulatif global (PDF « Journal banque » et
 * multi-banques), Récapitulatif général détaillé et Trésorerie prévisionnelle. La fin de mois est
 * la valeur par défaut partout ; la fin de trimestre a été ajoutée le 14/10/2026 pour le plan de
 * trésorerie trimestriel. Le trimestre est le trimestre civil (janvier-mars, avril-juin,
 * juillet-septembre, octobre-décembre).</p>
 *
 * <p>Une date de borne est toujours calculée à partir d'un « aujourd'hui » passé en paramètre,
 * jamais figée : un service Spring est un singleton, une borne calculée une seule fois resterait
 * bloquée sur la date de démarrage du serveur (bug du 28/07/2026).</p>
 */
public enum PeriodeRecap {

    /** N'inclut que les encours dont la date de référence est aujourd'hui ou avant. */
    A_CE_JOUR("à ce jour", "à ce jour"),

    /** Jusqu'au dernier jour du mois en cours — valeur par défaut. */
    FIN_DE_MOIS("jusqu'à fin de mois", "fin du mois"),

    /** Jusqu'au dernier jour du trimestre civil en cours. */
    FIN_DE_TRIMESTRE("jusqu'à fin de trimestre", "fin du trimestre");

    private final String libelle;
    private final String libelleCourt;

    PeriodeRecap(String libelle, String libelleCourt) {
        this.libelle = libelle;
        this.libelleCourt = libelleCourt;
    }

    /** Dernier jour inclus dans l'horizon, à partir de la date du jour donnée. */
    public LocalDate horizon(LocalDate aujourdHui) {
        return switch (this) {
            case A_CE_JOUR -> aujourdHui;
            case FIN_DE_MOIS -> aujourdHui.with(TemporalAdjusters.lastDayOfMonth());
            case FIN_DE_TRIMESTRE -> {
                int dernierMois = aujourdHui.get(IsoFields.QUARTER_OF_YEAR) * 3;
                yield aujourdHui.withMonth(dernierMois).with(TemporalAdjusters.lastDayOfMonth());
            }
        };
    }

    /** Libellé des titres de rapport (ex. « jusqu'à fin de trimestre »). */
    public String libelle() {
        return libelle;
    }

    /** Libellé des en-têtes de colonne (ex. « Solde fin du trimestre »). */
    public String libelleCourt() {
        return libelleCourt;
    }
}
