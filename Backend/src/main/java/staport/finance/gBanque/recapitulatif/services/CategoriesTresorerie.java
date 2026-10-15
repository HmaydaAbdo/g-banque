package staport.finance.gBanque.recapitulatif.services;

import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static staport.finance.gBanque.transactions.enums.TransactionSource.*;

/**
 * Regroupement des sources d'encours en catégories de trésorerie (ex. « REGLEMENT FRNS » =
 * Chèque + LCN + Virement + Mise à disposition), par sens.
 *
 * <p>Partagé par le Récapitulatif général détaillé et la Trésorerie prévisionnelle : les deux
 * présentent les mêmes lignes, dans le même ordre, pour qu'un montant se retrouve sous le même
 * libellé d'un rapport à l'autre.</p>
 */
final class CategoriesTresorerie {

    /** Ordre d'affichage des catégories de décaissement. */
    static final List<String> DECAISSEMENT_ORDER = List.of(
            "Leasing", "CMT", "REGLEMENT FRNS", "Virement International", "Préfinancement",
            "Factoring", "Droit Constaté", "Commission", "Agios", "Prélèvement",
            "Frais Bancaires", "Impôts & Taxes", "Impayée", "Opération Erronée", "Provision"
    );

    /** Ordre d'affichage des catégories d'encaissement. */
    static final List<String> ENCAISSEMENT_ORDER = List.of(
            "REGLEMENT FRNS", "Virement International", "Préfinancement", "Factoring",
            "Droit Constaté", "Décompte", "Restitution", "Impayée", "Provision"
    );

    private static final Map<TransactionSource, String> DECAISSEMENT = new EnumMap<>(TransactionSource.class);
    private static final Map<TransactionSource, String> ENCAISSEMENT = new EnumMap<>(TransactionSource.class);

    static {
        DECAISSEMENT.put(LEASING, "Leasing");
        DECAISSEMENT.put(CMT, "CMT");
        DECAISSEMENT.put(CHEQUE, "REGLEMENT FRNS");
        DECAISSEMENT.put(LCN_DECAISSEMENT, "REGLEMENT FRNS");
        DECAISSEMENT.put(Virement, "REGLEMENT FRNS");
        DECAISSEMENT.put(MiseADisposition, "REGLEMENT FRNS");
        DECAISSEMENT.put(VIREMENT_INTERNATIONAL, "Virement International");
        DECAISSEMENT.put(PREFINANCEMENT, "Préfinancement");
        DECAISSEMENT.put(FACTORING, "Factoring");
        DECAISSEMENT.put(DROIT_CONSTATE, "Droit Constaté");
        DECAISSEMENT.put(COMMISSION, "Commission");
        DECAISSEMENT.put(AGIOS, "Agios");
        DECAISSEMENT.put(PRELEVEMENT, "Prélèvement");
        DECAISSEMENT.put(FRAIS_BANCAIRE, "Frais Bancaires");
        DECAISSEMENT.put(IMPOT_TAXE, "Impôts & Taxes");
        DECAISSEMENT.put(IMPAYEE, "Impayée");
        DECAISSEMENT.put(OPERATION_ERRONE, "Opération Erronée");
        DECAISSEMENT.put(PROVISION, "Provision");

        ENCAISSEMENT.put(CHEQUE, "REGLEMENT FRNS");
        ENCAISSEMENT.put(LCN_ENCAISSEMENT, "REGLEMENT FRNS");
        ENCAISSEMENT.put(LCN_ESCOMPTE, "REGLEMENT FRNS");
        ENCAISSEMENT.put(Virement, "REGLEMENT FRNS");
        ENCAISSEMENT.put(MiseADisposition, "REGLEMENT FRNS");
        ENCAISSEMENT.put(VIREMENT_INTERNATIONAL, "Virement International");
        ENCAISSEMENT.put(PREFINANCEMENT, "Préfinancement");
        ENCAISSEMENT.put(FACTORING, "Factoring");
        ENCAISSEMENT.put(DROIT_CONSTATE, "Droit Constaté");
        ENCAISSEMENT.put(DECOMPTES, "Décompte");
        ENCAISSEMENT.put(RESTITUTION, "Restitution");
        ENCAISSEMENT.put(IMPAYEE, "Impayée");
        ENCAISSEMENT.put(PROVISION, "Provision");
    }

    static final String FAMILLE_REGLEMENTS = "Règlements";
    static final String FAMILLE_FINANCEMENTS = "Financements";
    static final String FAMILLE_CHARGES = "Charges bancaires";
    static final String FAMILLE_AUTRES = "Autres";

    private static final Map<String, String> FAMILLES = Map.ofEntries(
            Map.entry("REGLEMENT FRNS", FAMILLE_REGLEMENTS),
            Map.entry("Virement International", FAMILLE_REGLEMENTS),
            Map.entry("Leasing", FAMILLE_FINANCEMENTS),
            Map.entry("CMT", FAMILLE_FINANCEMENTS),
            Map.entry("Préfinancement", FAMILLE_FINANCEMENTS),
            Map.entry("Factoring", FAMILLE_FINANCEMENTS),
            Map.entry("Droit Constaté", FAMILLE_FINANCEMENTS),
            Map.entry("Agios", FAMILLE_FINANCEMENTS),
            Map.entry("Commission", FAMILLE_CHARGES),
            Map.entry("Prélèvement", FAMILLE_CHARGES),
            Map.entry("Frais Bancaires", FAMILLE_CHARGES),
            Map.entry("Impôts & Taxes", FAMILLE_CHARGES));

    /**
     * Catégories dont les montants sont **certains** : échéanciers contractuels (leasing, crédits moyen
     * terme) et moyens de paiement déjà émis (chèques, LCN, virements, mises à disposition,
     * prélèvements). Tout le reste (agios, commissions, frais, impôts, préfinancement, factoring, droits
     * constatés, décomptes, restitutions, impayées, provisions, opérations erronées) est une
     * **estimation** : calculé, anticipé ou encore susceptible de changer.
     */
    private static final Set<String> CERTAINES = Set.of(
            "Leasing", "CMT", "REGLEMENT FRNS", "Virement International", "Prélèvement");

    private CategoriesTresorerie() {
    }

    /** Vrai si les montants de la catégorie sont certains (voir {@link #CERTAINES}). */
    static boolean certain(String categorie) {
        return CERTAINES.contains(categorie);
    }

    /** Famille d'une catégorie ; tout ce qui n'est pas listé (décomptes, anomalies, provisions…) va dans « Autres ». */
    static String famille(String categorie) {
        return FAMILLES.getOrDefault(categorie, FAMILLE_AUTRES);
    }

    /**
     * Catégorie d'une source dans un sens donné, ou {@code null} si ce couple n'appartient à
     * aucune catégorie (il n'est alors pas compté dans les totaux par catégorie).
     */
    static String categorie(TransactionSource source, Type sens) {
        return (sens == Type.ENCAISSEMENT ? ENCAISSEMENT : DECAISSEMENT).get(source);
    }

    static List<String> ordre(Type sens) {
        return sens == Type.ENCAISSEMENT ? ENCAISSEMENT_ORDER : DECAISSEMENT_ORDER;
    }
}
