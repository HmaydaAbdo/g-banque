import { Routes } from '@angular/router';
import { departmentGuard } from '../../auth/guards/department.guard';

export const COMMISSION_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./views/CommissionListComponent/commission-list.component').then(m => m.CommissionListComponent),
    canActivate: [departmentGuard('Finance')]
  },
  {
    path: 'new',
    loadComponent: () => import('./views/commission-form/commission-form.component').then(m => m.CommissionFormComponent),
    canActivate: [departmentGuard('Finance')]
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./views/commission-form/commission-form.component').then(m => m.CommissionFormComponent),
    canActivate: [departmentGuard('Finance')]
  }
];
