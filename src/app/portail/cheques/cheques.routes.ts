import { Routes } from '@angular/router';
import { departmentGuard } from '../../auth/guards/department.guard';

export const CHEQUES_ROUTES: Routes = [
  {
    path: 'decaissements',
    canActivate: [departmentGuard('Finance')],
    loadComponent: () => import('./views/cheques-decaissements-list/cheques-decaissements-list.component')
      .then(m => m.ChequesDecaissementsListComponent)
  },
  {
    path: 'encaissements',
    canActivate: [departmentGuard('Finance')],
    loadComponent: () => import('./views/cheques-encaissements-list/cheques-encaissements-list.component')
      .then(m => m.ChequesEncaissementsListComponent)
  },
  {
    path: 'new',
    canActivate: [departmentGuard('Finance')],
    loadComponent: () => import('./views/cheques-form/cheques-form.component')
      .then(m => m.ChequesFormComponent)
  },
  {
    path: 'edit/:id',
    canActivate: [departmentGuard('Finance')],
    loadComponent: () => import('./views/cheques-form/cheques-form.component')
      .then(m => m.ChequesFormComponent)
  },
];
