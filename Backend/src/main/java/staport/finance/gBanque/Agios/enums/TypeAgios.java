package staport.finance.gBanque.Agios.enums;

/**
 * Types d'agios gérés par le moteur de calcul automatique.
 *
 * <p>Chaque type porte sa {@link PeriodeCalculAgios} qui détermine si
 * l'agrégat de référence est un <b>trimestre</b> (1–4) ou un <b>mois</b> (1–12).
 * L'orchestrateur l'interroge pour calculer les dates de début/fin de période
 * et pour identifier la « période courante » lors du calcul quotidien.</p>
 */
public enum TypeAgios {

    FACILITE_CAISSE("Facilité de Caisse",  PeriodeCalculAgios.TRIMESTRIEL),
    PREFINANCEMENT ("Préfinancement",       PeriodeCalculAgios.TRIMESTRIEL),
    DROIT_CONSTATE ("Droits Constatés",     PeriodeCalculAgios.TRIMESTRIEL),
    FACTORING      ("Factoring",            PeriodeCalculAgios.MENSUEL);

    private final String            libelle;
    private final PeriodeCalculAgios periodeCalcul;

    TypeAgios(String libelle, PeriodeCalculAgios periodeCalcul) {
        this.libelle       = libelle;
        this.periodeCalcul = periodeCalcul;
    }

    public String             getLibelle()       { return libelle; }
    public PeriodeCalculAgios getPeriodeCalcul() { return periodeCalcul; }
}