import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';
import { map, tap } from 'rxjs/operators';

/**
 * Réservé au SYSTEM_ADMIN (administrateur système transversal — n'appartient à
 * aucun département). Remplace `adminGuard` (basé sur l'ancien enum `Role`).
 */
export const systemAdminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.isAuthenticated$.pipe(
    tap(isAuthenticated => {
      if (!isAuthenticated) {
        router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
      }
    }),
    map(isAuthenticated => {
      if (!isAuthenticated) {
        return false;
      }
      const isSystemAdmin = authService.isSystemAdmin();
      if (!isSystemAdmin) {
        router.navigate(['/portail/unauthorized']);
      }
      return isSystemAdmin;
    })
  );
};
