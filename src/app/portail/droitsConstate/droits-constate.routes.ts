import { Routes } from '@angular/router';

export const DROITS_CONSTATE_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./views/droit-constate-list/droit-constate-list.component').then(m => m.DroitConstateListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./views/droit-constate-form/droit-constate-form.component').then(m => m.DroitConstateFormComponent)
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./views/droit-constate-form/droit-constate-form.component').then(m => m.DroitConstateFormComponent)
  }
];
