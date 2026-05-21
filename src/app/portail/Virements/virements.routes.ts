import { Routes } from '@angular/router';

export const VIREMENTS_ROUTES: Routes = [
  {
    path: 'decaissement',
    loadComponent: () => import('./views/virement-decaissement-list/virement-decaissement-list.component').then(m => m.VirementDecaissementListComponent)
  },
  {
    path: 'encaissement',
    loadComponent: () => import('./views/virement-encaissement-list/virement-encaissement-list.component').then(m => m.VirementEncaissementListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./views/virement-form/virement-form.component').then(m => m.VirementFormComponent)
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./views/virement-form/virement-form.component').then(m => m.VirementFormComponent)
  }
];
