package staport.finance.gBanque.audit;

/**
 * Type d'opération métier tracée dans {@code Log.operationType}.
 *
 * <p>Pour une méthode "upsert" (ex. assignation d'un rôle département qui crée
 * ou met à jour selon le cas), la convention est {@link #UPDATE} par défaut —
 * voir Backend/CLAUDE.md, section "Audit".
 */
public enum AuditOperation {
    CREATE,
    UPDATE,
    DELETE
}
