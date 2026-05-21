import { Routes } from '@angular/router';

export const COMMISSION_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./views/CommissionListComponent/commission-list.component').then(m => m.CommissionListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./views/commission-form/commission-form.component').then(m => m.CommissionFormComponent)
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./views/commission-form/commission-form.component').then(m => m.CommissionFormComponent)
  }
];
