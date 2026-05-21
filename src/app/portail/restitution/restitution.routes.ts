import { Routes } from '@angular/router';

export const RESTITUTION_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./views/restitution-list/restitution-list.component').then(m => m.RestitutionListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./views/restitution-form/restitution-form.component').then(m => m.RestitutionFormComponent)
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./views/restitution-form/restitution-form.component').then(m => m.RestitutionFormComponent)
  }
];
