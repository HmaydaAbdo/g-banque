package staport.finance.gBanque.user.dtos.request;

/**
 * Critères de recherche pour {@code GET /users} — tous les champs sont optionnels.
 */
public record UserSearchCriteria(
        String username,
        Boolean enabled,
        Boolean systemAdmin,
        Long departmentId
) {}
