import { Routes } from '@angular/router';

export const AGIOS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./views/agios-list/agios-list.component').then(m => m.AgiosListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./views/agios-form/agios.form.component').then(m => m.AgiosFormComponent)
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./views/agios-form/agios.form.component').then(m => m.AgiosFormComponent)
  },
  {
    path: 'calcul',
    loadComponent: () => import('../agios-calcul/views/agios-calcul-dashboard/agios-calcul-dashboard.component').then(m => m.AgiosCalculDashboardComponent)
  },
  {
    path: 'calcul/detail/:banqueId/:type/:trim/:annee',
    loadComponent: () => import('../agios-calcul/views/agios-calcul-detail/agios-calcul-detail.component').then(m => m.AgiosCalculDetailComponent)
  }
];
