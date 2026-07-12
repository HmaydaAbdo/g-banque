import { Routes } from '@angular/router';
import { departmentGuard } from '../../../auth/guards/department.guard';

/**
 * Routes du module Caution.
 *
 * Structure :
 *   /portail/cautions                → liste des cautions
 *   /portail/cautions/new            → création
 *   /portail/cautions/edit/:id       → modification
 *   /portail/cautions/commissions    → liste des commissions sur cautions
 *   /portail/cautions/tva            → liste des TVA sur cautions
 *
 * Accès : Finance (membre ou directeur) ou SYSTEM_ADMIN (cf. docs/rbac/caution.md).
 * La suppression définitive (SYSTEM_ADMIN uniquement) est conditionnée dans l'UI
 * (isSystemAdmin()), pas par un guard de route dédié.
 */
export const CAUTION_ROUTES: Routes = [
  {
    path: '',
    canActivate: [departmentGuard('Finance')],
    loadComponent: () =>
      import('../views/caution-list/caution-list.component')
        .then(m => m.CautionListComponent),
  },
  {
    path: 'new',
    canActivate: [departmentGuard('Finance')],
    loadComponent: () =>
      import('../views/caution-form/caution-form.component')
        .then(m => m.CautionFormComponent),
  },
  {
    path: 'edit/:id',
    canActivate: [departmentGuard('Finance')],
    loadComponent: () =>
      import('../views/caution-form/caution-form.component')
        .then(m => m.CautionFormComponent),
  },
  {
    path: 'commissions',
    canActivate: [departmentGuard('Finance')],
    loadComponent: () =>
      import('../views/commission-caution-list/commission-caution-list.component')
        .then(m => m.CommissionCautionListComponent),
  },
  {
    path: 'commissions/edit/:id',
    canActivate: [departmentGuard('Finance')],
    loadComponent: () =>
      import('../views/commission-caution-form/commission-caution-form.component')
        .then(m => m.CommissionCautionFormComponent),
  },
  {
    path: 'tva',
    canActivate: [departmentGuard('Finance')],
    loadComponent: () =>
      import('../views/tva-caution-list/tva-caution-list.component')
        .then(m => m.TvaCautionListComponent),
  },
];
