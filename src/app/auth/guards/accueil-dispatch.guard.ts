import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { map } from 'rxjs/operators';

/**
 * Guard dispatcher pour l'accueil : redirige chaque utilisateur vers
 * la page d'accueil correspondant à son département.
 *
 * - SYSTEM_ADMIN ou Finance → /portail/accueil/finance
 * - Autre département       → /portail/accueil/bienvenue  (placeholder)
 *
 * Pour ajouter un accueil Marchés : ajouter un `else if` avant le fallback bienvenue.
 * Aucune autre modification nécessaire.
 */
export const accueilDispatchGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router      = inject(Router);

  return authService.isAuthenticated$.pipe(
    map(isAuthenticated => {
      if (!isAuthenticated) {
        router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
        return false;
      }

      if (authService.isSystemAdmin() || authService.belongsToDepartment('Finance')) {
        return router.createUrlTree(['/portail/accueil/finance']);
      }

      // Placeholder — remplacer par une redirection spécifique
      // quand l'accueil d'un autre département sera construit.
      return router.createUrlTree(['/portail/accueil/bienvenue']);
    })
  );
};
