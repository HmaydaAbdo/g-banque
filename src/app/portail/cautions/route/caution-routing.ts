import { Routes } from '@angular/router';
import { adminGuard } from '../../../auth/guards/admin.guard';

/**
 * Routes du module Caution.
 *
 * - Liste : accessible à tous les utilisateurs authentifiés.
 * - new/edit : protégés par adminGuard — seul l'admin peut créer ou modifier.
 */
export const CAUTION_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('../views/caution-list/caution-list.component')
        .then(m => m.CautionListComponent),
  },
  {
    path: 'new',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('../views/caution-form/caution-form.component')
        .then(m => m.CautionFormComponent),
  },
  {
    path: 'edit/:id',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('../views/caution-form/caution-form.component')
        .then(m => m.CautionFormComponent),
  },
];
