import { Routes } from '@angular/router';

export const LIGNES_SPECIFIQUES_ENCOURS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./views/spefic-ligne-encours/spefic-ligne-encours.component').then(m => m.SpeficLigneEncoursComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./views/spefic-ligne-edit/spefic-ligne-edit.component').then(m => m.SpeficLigneEditComponent)
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./views/spefic-ligne-edit/spefic-ligne-edit.component').then(m => m.SpeficLigneEditComponent)
  }
];

export const LIGNES_SPECIFIQUES_ARCHIVED_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./views/spefic-ligne-archivees/spefic-ligne-archivees.component').then(m => m.SpeficLigneArchiveesComponent)
  }
];
