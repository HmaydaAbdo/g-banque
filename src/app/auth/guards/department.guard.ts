import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { map, tap } from 'rxjs/operators';
import { DepartmentRoleType } from '../types/DepartmentRoleType';

/**
 * Factory de guard pour une route réservée à un département donné.
 *
 * - Sans `minRole` : accessible à tout membre du département (MEMBER ou DIRECTOR).
 * - Avec `minRole: 'DIRECTOR'` : réservé aux DIRECTOR de ce département.
 * - SYSTEM_ADMIN passe toujours (accès transversal).
 *
 * Usage : `canActivate: [departmentGuard('TRESORERIE')]` ou
 * `canActivate: [departmentGuard('TRESORERIE', 'DIRECTOR')]`.
 */
export function departmentGuard(department: string, minRole?: DepartmentRoleType): CanActivateFn {
  return (route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    return authService.isAuthenticated$.pipe(
      tap(isAuthenticated => {
        if (!isAuthenticated) {
          console.warn(`departmentGuard(${department}): utilisateur non authentifié. Redirection vers /login.`);
          router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
        }
      }),
      map(isAuthenticated => {
        if (!isAuthenticated) {
          return false;
        }

        if (authService.isSystemAdmin()) {
          return true;
        }

        const hasAccess = minRole
          ? authService.hasDepartmentRole(department, minRole)
          : authService.belongsToDepartment(department);

        if (!hasAccess) {
          console.warn(`departmentGuard(${department}): accès refusé. Redirection vers /portail/unauthorized.`);
          router.navigate(['/portail/unauthorized']);
        }
        return hasAccess;
      })
    );
  };
}
