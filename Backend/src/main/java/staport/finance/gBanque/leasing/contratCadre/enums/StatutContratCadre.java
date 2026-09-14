package staport.finance.gBanque.leasing.contratCadre.enums;

/**
 * Statut du cycle de vie d'un contrat cadre.
 * <ul>
 *   <li>{@code ACTIF}   — conditions en vigueur, utilisables lors de la création d'un contrat leasing.</li>
 *   <li>{@code EXPIRE}  — date de fin dépassée, mis à jour en lecture (lazy expiration).</li>
 *   <li>{@code ARCHIVE} — archivé manuellement par un administrateur ; n'est plus utilisable.</li>
 * </ul>
 */
public enum StatutContratCadre {
    ACTIF,
    EXPIRE,
    ARCHIVE
}
