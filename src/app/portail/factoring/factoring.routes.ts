import { Routes } from '@angular/router';

export const FACTORING_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./views/factoring-list/factoring-list.component').then(m => m.FactoringListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./views/factoring-form/factoring-form.component').then(m => m.FactoringFormComponent)
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./views/factoring-form/factoring-form.component').then(m => m.FactoringFormComponent)
  }
];
