import { Routes } from '@angular/router';

export const PROVISION_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./views/provision-list/provision-list.component').then(m => m.ProvisionListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./views/provision-form/provision-form.component').then(m => m.ProvisionFormComponent)
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./views/provision-form/provision-form.component').then(m => m.ProvisionFormComponent)
  }
];
