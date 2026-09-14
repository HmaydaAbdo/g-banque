import { Routes } from '@angular/router';
import { departmentGuard } from '../../auth/guards/department.guard';

export const IMPOT_ET_TAXE_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./views/impot-et-taxe-list/impot-et-taxe-list.component').then(m => m.ImpotEtTaxeListComponent),
    canActivate: [departmentGuard('Finance')]
  },
  {
    path: 'new',
    loadComponent: () => import('./views/impot-et-taxe-form/impot-et-taxe-form.component').then(m => m.ImpotEtTaxeFormComponent),
    canActivate: [departmentGuard('Finance')]
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./views/impot-et-taxe-form/impot-et-taxe-form.component').then(m => m.ImpotEtTaxeFormComponent),
    canActivate: [departmentGuard('Finance')]
  }
];
