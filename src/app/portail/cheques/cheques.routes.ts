import { Routes } from '@angular/router';

export const CHEQUES_ROUTES: Routes = [
  {
    path: 'decaissements',
    loadComponent: () => import('./views/cheques-decaissements-list/cheques-decaissements-list.component')
      .then(m => m.ChequesDecaissementsListComponent)
  },
  {
    path: 'encaissements',
    loadComponent: () => import('./views/cheques-encaissements-list/cheques-encaissements-list.component')
      .then(m => m.ChequesEncaissementsListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./views/cheques-form/cheques-form.component')
      .then(m => m.ChequesFormComponent)
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./views/cheques-form/cheques-form.component')
      .then(m => m.ChequesFormComponent)
  },
];
