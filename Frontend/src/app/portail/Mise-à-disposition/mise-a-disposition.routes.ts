import { Routes } from '@angular/router';
import { departmentGuard } from '../../auth/guards/department.guard';

export const MISE_A_DISPOSITION_ROUTES: Routes = [
  {
    path: 'decaissement',
    loadComponent: () => import('./views/mise-a-disposition-decaissement-list/mise-a-disposition-decaissement-list.component').then(m => m.MiseADispositionDecaissementListComponent),
    canActivate: [departmentGuard('Finance')]
  }
];
