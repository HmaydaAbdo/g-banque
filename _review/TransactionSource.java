package staport.finance.gBanque.transactions.enums;

/**
 * Origine d'une opération. Cet enum joue trois rôles distincts, ce qui explique qu'il contienne
 * des valeurs qui n'ont pas de sens dans tous les contextes :
 * <ol>
 *   <li>source d'une {@code Transaction} et {@code source_type} de la vue
 *       {@code v_recapitulatif_encours} ;</li>
 *   <li>motif d'une {@code Provision}, d'un {@code Impayee} ou d'une {@code OperationErrone} ;</li>
 *   <li>opération rattachée à une {@code Commission}.</li>
 * </ol>
 *
 * <p>Le partage entre ces trois rôles est un choix assumé (07/09/2026) : il évite une migration
 * sur trois entités, au prix de listes de motifs qui proposent des valeurs sans objet pour elles.
 * Le jour où cela gêne, la sortie est un enum {@code Motif} dédié.</p>
 *
 * <p><b>Toute valeur ajoutée ici doit l'être aussi dans {@code TransactionSource.ts} côté Angular</b>,
 * qui est le miroir de ce fichier — ainsi que dans les deux {@code Record<TransactionSource, string>}
 * du frontend ({@code TransactionSourceLabels} et {@code SOURCE_COLOR}), que TypeScript refuse de
 * compiler s'ils ne sont pas exhaustifs. Les deux enums doivent rester identiques, constante pour
 * constante.</p>
 */
public enum TransactionSource {
    LEASING,
    CMT,
    CHEQUE,
    /**
     * Valeur générique héritée, jamais écrite par l'application : les LCN réelles passent par
     * {@link #LCN_DECAISSEMENT}, {@link #LCN_ENCAISSEMENT} et {@link #LCN_ESCOMPTE}. Conservée
     * délibérément (décision du 07/09/2026) pour rester capable de relire d'anciennes lignes —
     * la supprimer ferait échouer Hibernate sur toute ligne historique qui la porterait.
     * Ne pas retirer sans avoir vérifié en base qu'aucune ligne ne l'utilise.
     */
    LCN,
    LCN_DECAISSEMENT,
    LCN_ENCAISSEMENT,
    LCN_ESCOMPTE,
    Virement,
    MiseADisposition,
    PREFINANCEMENT,
    DROIT_CONSTATE,
    DECOMPTES,
    COMMISSION,
    AGIOS,
    FRAIS_BANCAIRE,
    FACTORING,
    PRELEVEMENT,
    CAUTION,
    IMPOT_TAXE,
    RESTITUTION,
    OPERATION_ERRONE,
    VIREMENT_INTERNATIONAL,
    IMPAYEE,
    PROVISION,
    Importation
}