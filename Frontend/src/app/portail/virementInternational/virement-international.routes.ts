import { Routes } from '@angular/router';
import { departmentGuard } from '../../auth/guards/department.guard';

export const VIREMENT_INTERNATIONAL_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./views/virement-international-list/virement-international-list.component').then(m => m.VirementInternationalListComponent),
    canActivate: [departmentGuard('Finance')]
  },
  {
    path: 'new',
    loadComponent: () => import('./views/virement-international-form/virement-international-form.component').then(m => m.VirementInternationalFormComponent),
    canActivate: [departmentGuard('Finance')]
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./views/virement-international-form/virement-international-form.component').then(m => m.VirementInternationalFormComponent),
    canActivate: [departmentGuard('Finance')]
  }
];
