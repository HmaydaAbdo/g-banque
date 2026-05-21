import { Routes } from '@angular/router';

export const VIREMENT_INTERNATIONAL_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./views/virement-international-list/virement-international-list.component').then(m => m.VirementInternationalListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./views/virement-international-form/virement-international-form.component').then(m => m.VirementInternationalFormComponent)
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./views/virement-international-form/virement-international-form.component').then(m => m.VirementInternationalFormComponent)
  }
];
