import { Routes } from '@angular/router';

export const IMPAYEES_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./views/ImpayeeList/Impayee-list.component').then(m => m.ImpayeeListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./views/ImpayeeForm/Impayee-form.component').then(m => m.ImpayeeFormComponent)
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./views/ImpayeeForm/Impayee-form.component').then(m => m.ImpayeeFormComponent)
  }
];
