import { Routes } from '@angular/router';
import { departmentGuard } from '../../auth/guards/department.guard';

export const VIREMENTS_ROUTES: Routes = [
  {
    path: 'decaissement',
    loadComponent: () => import('./views/virement-decaissement-list/virement-decaissement-list.component').then(m => m.VirementDecaissementListComponent),
    canActivate: [departmentGuard('Finance')]
  },
  {
    path: 'encaissement',
    loadComponent: () => import('./views/virement-encaissement-list/virement-encaissement-list.component').then(m => m.VirementEncaissementListComponent),
    canActivate: [departmentGuard('Finance')]
  },
  {
    path: 'new',
    loadComponent: () => import('./views/virement-form/virement-form.component').then(m => m.VirementFormComponent),
    canActivate: [departmentGuard('Finance')]
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./views/virement-form/virement-form.component').then(m => m.VirementFormComponent),
    canActivate: [departmentGuard('Finance')]
  }
];
