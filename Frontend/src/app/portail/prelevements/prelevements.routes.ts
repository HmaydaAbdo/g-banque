import { Routes } from '@angular/router';
import { departmentGuard } from '../../auth/guards/department.guard';

export const PRELEVEMENTS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./views/prelevement-list/prelevement-list.component').then(m => m.PrelevementListComponent),
    canActivate: [departmentGuard('Finance')]
  },
  {
    path: 'new',
    loadComponent: () => import('./views/prelevement-form/prelevement-form.component').then(m => m.PrelevementFormComponent),
    canActivate: [departmentGuard('Finance')]
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./views/prelevement-form/prelevement-form.component').then(m => m.PrelevementFormComponent),
    canActivate: [departmentGuard('Finance')]
  }
];
