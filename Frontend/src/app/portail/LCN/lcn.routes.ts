import { Routes } from '@angular/router';
import { departmentGuard } from '../../auth/guards/department.guard';

export const LCN_ROUTES: Routes = [
  {
    path: 'decaissements',
    loadComponent: () => import('./views/lcn-decaissements-list/lcn-decaissements-list.component').then(m => m.LcnDecaissementsListComponent),
    canActivate: [departmentGuard('Finance')]
  },
  {
    path: 'encaissements',
    loadComponent: () => import('./views/lcn-encaissements-list/lcn-encaissements-list.component').then(m => m.LcnEncaissementsListComponent),
    canActivate: [departmentGuard('Finance')]
  },
  {
    path: 'new',
    loadComponent: () => import('./views/lcn-form/lcn-form.component').then(m => m.LcnFormComponent),
    canActivate: [departmentGuard('Finance')]
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./views/lcn-form/lcn-form.component').then(m => m.LcnFormComponent),
    canActivate: [departmentGuard('Finance')]
  }
];
