package staport.finance.gBanque.security.enums;

/**
 * Rôle d'un utilisateur au sein d'un département.
 *
 * <ul>
 *   <li>{@link #MEMBER} — accès lecture + écriture standard sur le périmètre du département
 *       (saisie, validation des opérations courantes). Pas d'accès aux actions sensibles
 *       (clôture, annulation, suppression).</li>
 *   <li>{@link #DIRECTOR} — accès complet sur le département, incluant les actions sensibles
 *       (clôture de rapprochement, annulation de transaction, suppression).</li>
 * </ul>
 *
 * <p>Un utilisateur {@code SYSTEM_ADMIN} (voir {@code User.isSystemAdmin}) n'a pas besoin de
 * ces rôles : il a un accès transversal à tous les départements et n'appartient à aucun en
 * particulier.
 */
public enum DepartmentRoleType {
    MEMBER,
    DIRECTOR
}
