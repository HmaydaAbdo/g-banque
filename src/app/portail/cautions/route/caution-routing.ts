import { Routes } from '@angular/router';
import { systemAdminGuard } from '../../../auth/guards/system-admin.guard';

/**
 * Routes du module Caution.
 *
 * Structure :
 *   /portail/cautions                → liste des cautions
 *   /portail/cautions/new            → création
 *   /portail/cautions/edit/:id       → modification
 *   /portail/cautions/commissions    → liste des commissions sur cautions
 *   /portail/cautions/tva            → liste des TVA sur cautions
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
    loadComponent: () =>
      import('../views/caution-form/caution-form.component')
        .then(m => m.CautionFormComponent),
  },
  {
    path: 'edit/:id',
    loadComponent: () =>
      import('../views/caution-form/caution-form.component')
        .then(m => m.CautionFormComponent),
  },
  {
    path: 'commissions',
    loadComponent: () =>
      import('../views/commission-caution-list/commission-caution-list.component')
        .then(m => m.CommissionCautionListComponent),
  },
  {
    path: 'commissions/edit/:id',
    loadComponent: () =>
      import('../views/commission-caution-form/commission-caution-form.component')
        .then(m => m.CommissionCautionFormComponent),
  },
  {
    path: 'tva',
    loadComponent: () =>
      import('../views/tva-caution-list/tva-caution-list.component')
        .then(m => m.TvaCautionListComponent),
  },
];
