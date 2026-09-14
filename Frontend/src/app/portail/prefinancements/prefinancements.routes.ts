import { Routes } from '@angular/router';

export const PREFINANCEMENTS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./views/prefinancements-list/prefinancements-list.component').then(m => m.PrefinancementsListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./views/prefinancement-form/prefinancement-form.component').then(m => m.PrefinancementFormComponent)
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./views/prefinancement-form/prefinancement-form.component').then(m => m.PrefinancementFormComponent)
  }
];
