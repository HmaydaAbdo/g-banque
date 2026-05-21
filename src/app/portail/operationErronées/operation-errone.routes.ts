import { Routes } from '@angular/router';

export const OPERATION_ERRONE_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./views/operation-errone-list/operation-errone-list.component').then(m => m.OperationErroneListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./views/operation-errone-form/operation-errone-form.component').then(m => m.OperationErroneFormComponent)
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./views/operation-errone-form/operation-errone-form.component').then(m => m.OperationErroneFormComponent)
  }
];
