import { Routes } from '@angular/router';

export const MARCHES_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./views/marches-list/marches-list.component').then(m => m.MarchesListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./views/marches-form/marches-form.component').then(m => m.MarchesFormComponent)
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./views/marches-form/marches-form.component').then(m => m.MarchesFormComponent)
  }
];
