package staport.finance.gBanque.security.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Levée lorsqu'on tente de supprimer un {@code Department} auquel des utilisateurs
 * sont encore assignés (via {@code UserDepartmentRole}). L'administrateur doit
 * d'abord retirer ou réassigner ces utilisateurs.
 *
 * <p>Annotée {@code @ResponseStatus} : capturée automatiquement par Spring même
 * sans {@code @ExceptionHandler} dédié (résolue en 409 Conflict avec le corps par
 * défaut de Spring). Un handler dédié pourra être ajouté plus tard si on veut
 * renvoyer le même format {@code ErrorResponse} unifié que les autres modules.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DepartmentInUseException extends RuntimeException {
    public DepartmentInUseException(String message) {
        super(message);
    }
}
