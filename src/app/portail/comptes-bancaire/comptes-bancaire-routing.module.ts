import { Routes } from '@angular/router';
import { departmentGuard } from '../../auth/guards/department.guard';

/**
 * Module Comptes Bancaires (liste, new, edit, historique) réservé au département
 * Finance (MEMBER ou DIRECTOR) ou SYSTEM_ADMIN — departmentGuard laisse toujours
 * passer SYSTEM_ADMIN (accès transversal). Guard posé une seule fois sur la route
 * parente : s'applique à tous les enfants.
 *
 * Restriction au niveau Angular uniquement — l'API GET /banques côté backend reste
 * ouverte à tout authentifié, car elle sert de donnée de référence (listes déroulantes
 * "choisir une banque") à d'autres modules indépendants (Agios, Commissions, Droits
 * constatés, etc.). La restreindre casserait ces modules pour les autres départements.
 */
export const COMPTES_BANCAIRES_ROUTES: Routes = [
  {
    path: '',
    canActivate: [departmentGuard('Finance')],
    children: [
      {
        path: '',
        loadComponent: () => import('./views/comptes-bancaires/comptes-bancaires.component')
          .then(m => m.ComptesBancairesComponent),
      },
      {
        path: 'new',
        loadComponent: () => import('./views/banque-edit/banque-edit.component')
          .then(m => m.BanqueEditComponent),
      },
      {
        path: 'edit/:id',
        loadComponent: () => import('./views/banque-edit/banque-edit.component')
          .then(m => m.BanqueEditComponent),
      },
      {
        path: 'history/:id',
        loadComponent: () => import('./views/history/history-oflines/history-oflines.component')
          .then(m => m.HistoryOflinesComponent),
      },
    ]
  }
];
