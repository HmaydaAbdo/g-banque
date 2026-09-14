import { Routes } from '@angular/router';

export const DECOMPTES_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./views/decomptes-list/decomptes-list.component').then(m => m.DecomptesListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./views/decomptes-form/decomptes-form.component').then(m => m.DecompteFormComponent)
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./views/decomptes-form/decomptes-form.component').then(m => m.DecompteFormComponent)
  }
];
