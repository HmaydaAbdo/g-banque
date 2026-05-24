import { inject } from '@angular/core';
import { CanActivateFn, Router, ActivatedRouteSnapshot } from '@angular/router';

export const rapprochementActifGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const router = inject(Router);

  const rapprochementId = route.queryParamMap.get('rapprochementId');

  if (rapprochementId) {
    return true;
  }

  // Remonter jusqu'au paramètre banqueId (présent dans la route parente)
  let banqueId: string | null = route.paramMap.get('banqueId');
  if (!banqueId) {
    let parent = route.parent;
    while (parent && !banqueId) {
      banqueId = parent.paramMap.get('banqueId');
      parent = parent.parent;
    }
  }

  if (banqueId) {
    return router.createUrlTree(['/portail/etats-des-encours', banqueId, 'select']);
  }

  // Fallback : accueil
  return router.createUrlTree(['/portail/accueil']);
};
