package staport.finance.gBanque.commision.enums;

/**
 * Condition d'application d'une règle de commission configurée.
 *
 * <ul>
 *   <li>{@code DEFAULT}      — s'applique quelle que soit l'organisme (aucune condition sur l'organisme).</li>
 *   <li>{@code SI_MATCH}     — s'applique uniquement si l'organisme du contrat correspond à {@code organismeNom}.</li>
 *   <li>{@code SI_NON_MATCH} — s'applique uniquement si l'organisme du contrat ne correspond pas à {@code organismeNom}.</li>
 * </ul>
 */
public enum CommissionCondition {
    DEFAULT,
    SI_MATCH,
    SI_NON_MATCH
}
