import { Routes } from '@angular/router';

export const LIGNES_FONCTIONNELLES_ENCOURS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./views/lignes-fonctionnelles-encours/lignes-fonctionnelles.component').then(m => m.LignesFonctionnellesComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./views/ligne-fonctionnelle-edit/ligne-fonctionnelle-edit.component').then(m => m.LigneFonctionnelleEditComponent)
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./views/ligne-fonctionnelle-edit/ligne-fonctionnelle-edit.component').then(m => m.LigneFonctionnelleEditComponent)
  }
];

export const LIGNES_FONCTIONNELLES_ARCHIVED_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./views/lignes-fonctionnelles-archives/lignes-fonctionnelles-archives.component').then(m => m.LignesFonctionnellesArchivesComponent)
  }
];
