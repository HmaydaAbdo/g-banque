import { Routes } from '@angular/router';

export const MISE_A_DISPOSITION_ROUTES: Routes = [
  {
    path: 'decaissement',
    loadComponent: () => import('./views/mise-a-disposition-decaissement-list/mise-a-disposition-decaissement-list.component').then(m => m.MiseADispositionDecaissementListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./views/mise-a-disposition-form/mise-a-disposition-form.component').then(m => m.MiseADispositionFormComponent)
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./views/mise-a-disposition-form/mise-a-disposition-form.component').then(m => m.MiseADispositionFormComponent)
  }
];
