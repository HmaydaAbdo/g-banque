package staport.finance.gBanque.commision.enums;

/**
 * Catégorie d'une commission générée automatiquement depuis une caution — permet de
 * retrouver de façon fiable la commission de clôture d'une caution (nécessaire pour la
 * recalculer si la caution est corrigée après sa clôture), sans dépendre d'une analyse du
 * libellé ou de la date d'émission, qui peuvent coïncider avec une commission mensuelle du
 * même mois (cf. {@code CommissionCautionStrategie.calculerMainLevee} — par défaut identique
 * à une commission mensuelle pour les banques sans prorata de sortie).
 *
 * <p>{@code null} pour toute commission non générée depuis une caution (virement, impôt/taxe,
 * saisie manuelle) ou générée avant l'introduction de ce champ.
 */
public enum TypeCommission {

    /** Prorata calculé à la création de la caution (premier mois partiel), une seule fois. */
    PRORATA_ENTREE,

    /** Commission mensuelle récurrente générée par le batch tant que la caution est active. */
    MENSUELLE,

    /** Commission générée à la clôture (main levée) de la caution — au plus une par caution. */
    CLOTURE
}
