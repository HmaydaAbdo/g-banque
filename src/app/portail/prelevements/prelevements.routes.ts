import { Routes } from '@angular/router';

export const PRELEVEMENTS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./views/prelevement-list/prelevement-list.component').then(m => m.PrelevementListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./views/prelevement-form/prelevement-form.component').then(m => m.PrelevementFormComponent)
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./views/prelevement-form/prelevement-form.component').then(m => m.PrelevementFormComponent)
  }
];
