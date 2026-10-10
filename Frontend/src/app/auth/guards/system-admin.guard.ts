import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

/**
 * Réservé au SYSTEM_ADMIN (administrateur système transversal — n'appartient à
 * aucun département). Vérifie qu'une session est ouverte avant
 * de contrôler l'authority.
 */
export const systemAdminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
    return false;
  }

  if (!authService.isSystemAdmin()) {
    router.navigate(['/portail/unauthorized']);
    return false;
  }

  return true;
};
