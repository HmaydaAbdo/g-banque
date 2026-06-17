package staport.finance.gBanque.chargesFixes;

import staport.finance.gBanque.impot_taxes.enums.Motif;

import java.math.BigDecimal;

/**
 * Représente la définition d'une charge fixe bancaire.
 *
 * @param libelle        Libellé de base (sans période).
 * @param montant        Montant de la charge.
 * @param type           FRAIS_BANCAIRE | IMPOT_TAXE | AGIOS | PRELEVEMENT
 * @param motif          Obligatoire si type = IMPOT_TAXE, null sinon.
 * @param jourEmission   > 0 → jour fixe ; -1 → fin de mois ; -2 → fin de trimestre.
 * @param appendPeriode  Si true, le scheduler ajoute la période au libellé.
 * @param moisPrecedent  Si true (+ appendPeriode=true), la période affichée est le mois
 *                       précédent. Ex : scheduler tourne le 30/06 → affiche "05/2026".
 *                       Utilisé pour les charges de type "M-1" (TVA, IR...).
 */
public record ChargeFixeDefinition(
        String libelle,
        BigDecimal montant,
        TypeChargeFixe type,
        Motif motif,
        int jourEmission,
        boolean appendPeriode,
        boolean moisPrecedent
) {
    /** Frais bancaire — libellé fixe */
    public ChargeFixeDefinition(String libelle, BigDecimal montant, int jourEmission) {
        this(libelle, montant, TypeChargeFixe.FRAIS_BANCAIRE, null, jourEmission, false, false);
    }

    /** Frais bancaire — libellé avec période courante */
    public ChargeFixeDefinition(String libelle, BigDecimal montant, int jourEmission, boolean appendPeriode) {
        this(libelle, montant, TypeChargeFixe.FRAIS_BANCAIRE, null, jourEmission, appendPeriode, false);
    }

    /** Impôt/taxe — libellé fixe */
    public ChargeFixeDefinition(String libelle, BigDecimal montant, Motif motif, int jourEmission) {
        this(libelle, montant, TypeChargeFixe.IMPOT_TAXE, motif, jourEmission, false, false);
    }

    /** Impôt/taxe — libellé avec période (courante ou précédente) */
    public ChargeFixeDefinition(String libelle, BigDecimal montant, Motif motif, int jourEmission,
                                boolean appendPeriode) {
        this(libelle, montant, TypeChargeFixe.IMPOT_TAXE, motif, jourEmission, appendPeriode, false);
    }

    /** Impôt/taxe — libellé avec période précédente (cas M-1) */
    public ChargeFixeDefinition(String libelle, BigDecimal montant, Motif motif, int jourEmission,
                                boolean appendPeriode, boolean moisPrecedent) {
        this(libelle, montant, TypeChargeFixe.IMPOT_TAXE, motif, jourEmission, appendPeriode, moisPrecedent);
    }

    /** Agios ou Prélèvement — libellé fixe */
    public ChargeFixeDefinition(String libelle, BigDecimal montant, TypeChargeFixe type, int jourEmission) {
        this(libelle, montant, type, null, jourEmission, false, false);
    }

    /** Agios ou Prélèvement — libellé avec période courante */
    public ChargeFixeDefinition(String libelle, BigDecimal montant, TypeChargeFixe type,
                                int jourEmission, boolean appendPeriode) {
        this(libelle, montant, type, null, jourEmission, appendPeriode, false);
    }
}
