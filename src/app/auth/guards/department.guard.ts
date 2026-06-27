import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { DepartmentRoleType } from '../types/DepartmentRoleType';

/**
 * Factory de guard pour une route réservée à un département donné.
 *
 * - Sans `minRole` : accessible à tout membre du département (MEMBER ou DIRECTOR).
 * - Avec `minRole: 'DIRECTOR'` : réservé aux DIRECTOR de ce département.
 * - SYSTEM_ADMIN passe toujours (accès transversal).
 *
 * Vérifie la validité du token (expiration comprise) avant le contrôle d'autorité.
 *
 * Usage : `canActivate: [departmentGuard('TRESORERIE')]` ou
 * `canActivate: [departmentGuard('TRESORERIE', 'DIRECTOR')]`.
 */
export function departmentGuard(department: string, minRole?: DepartmentRoleType): CanActivateFn {
  return (route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (!authService.isTokenValid()) {
      router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
      return false;
    }

    if (authService.isSystemAdmin()) {
      return true;
    }

    const hasAccess = minRole
      ? authService.hasDepartmentRole(department, minRole)
      : authService.belongsToDepartment(department);

    if (!hasAccess) {
      router.navigate(['/portail/unauthorized']);
    }
    return hasAccess;
  };
}
