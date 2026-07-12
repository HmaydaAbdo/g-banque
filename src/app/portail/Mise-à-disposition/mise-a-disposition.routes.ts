import { Routes } from '@angular/router';
import { departmentGuard } from '../../auth/guards/department.guard';

export const MISE_A_DISPOSITION_ROUTES: Routes = [
  {
    path: 'decaissement',
    loadComponent: () => import('./views/mise-a-disposition-decaissement-list/mise-a-disposition-decaissement-list.component').then(m => m.MiseADispositionDecaissementListComponent),
    canActivate: [departmentGuard('Finance')]
  },
  {
    path: 'new',
    loadComponent: () => import('./views/mise-a-disposition-form/mise-a-disposition-form.component').then(m => m.MiseADispositionFormComponent),
    canActivate: [departmentGuard('Finance')]
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./views/mise-a-disposition-form/mise-a-disposition-form.component').then(m => m.MiseADispositionFormComponent),
    canActivate: [departmentGuard('Finance')]
  }
];
