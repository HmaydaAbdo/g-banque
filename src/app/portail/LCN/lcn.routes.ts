import { Routes } from '@angular/router';

export const LCN_ROUTES: Routes = [
  {
    path: 'decaissements',
    loadComponent: () => import('./views/lcn-decaissements-list/lcn-decaissements-list.component').then(m => m.LcnDecaissementsListComponent)
  },
  {
    path: 'encaissements',
    loadComponent: () => import('./views/lcn-encaissements-list/lcn-encaissements-list.component').then(m => m.LcnEncaissementsListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./views/lcn-form/lcn-form.component').then(m => m.LcnFormComponent)
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./views/lcn-form/lcn-form.component').then(m => m.LcnFormComponent)
  }
];
