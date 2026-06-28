import { Routes } from '@angular/router';

/**
 * Routes du module Administration — départements et utilisateurs.
 * Protégées au niveau parent par `systemAdminGuard` (voir portail.routes.ts),
 * pas besoin de le répéter sur chaque route enfant.
 */
export const ADMIN_ROUTES: Routes = [
  { path: '', redirectTo: 'departements', pathMatch: 'full' },
  {
    path: 'departements',
    loadComponent: () => import('./departements/views/departements-list/departements-list.component')
      .then(m => m.DepartementsListComponent)
  },
  {
    path: 'utilisateurs',
    loadComponent: () => import('./utilisateurs/views/utilisateurs-list/utilisateurs-list.component')
      .then(m => m.UtilisateursListComponent)
  },
  {
    path: 'utilisateurs/:id',
    loadComponent: () => import('./utilisateurs/views/utilisateur-detail/utilisateur-detail.component')
      .then(m => m.UtilisateurDetailComponent)
  },
  {
    path: 'commission-config',
    loadComponent: () => import('./commission-config/views/commission-config-list/commission-config-list.component')
      .then(m => m.CommissionConfigListComponent)
  }
];
