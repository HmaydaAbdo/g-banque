package staport.finance.gBanque.department.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Levée lorsqu'on tente de supprimer un {@code Department} auquel des utilisateurs
 * sont encore assignés (via {@code UserDepartmentRole}). L'administrateur doit
 * d'abord retirer ou réassigner ces utilisateurs.
 *
 * <p>Gérée par {@link DepartmentExceptionHandler#handleInUse} — retourne 409 Conflict
 * avec le format {@code ErrorResponse} unifié.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DepartmentInUseException extends RuntimeException {
    public DepartmentInUseException(String message) {
        super(message);
    }
}
