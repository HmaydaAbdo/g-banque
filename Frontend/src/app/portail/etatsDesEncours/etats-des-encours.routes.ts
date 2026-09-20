import { Routes } from '@angular/router';
import { rapprochementActifGuard } from './guards/rapprochement-actif.guard';

export const ETATS_DES_ENCOURS_ROUTES: Routes = [
  {
    path: 'select',
    loadComponent: () => import('./views/select-rapprochement/select-rapprochement.component').then(m => m.SelectRapprochementComponent)
  },
  {
    path: ':rapprochementId',
    canActivate: [rapprochementActifGuard],
    loadComponent: () => import('./views/parent-componant/les-encours/les-encours.component').then(m => m.LesEncoursComponent),
    children: [
      { path: '', redirectTo: 'recapitulatif', pathMatch: 'full' },

      { path: 'rapprochement', loadComponent: () => import('./views/lesEncours/rapprochemnt-encours/rapprochemnt-encours.component').then(m => m.RapprochemntEncoursComponent) },

      { path: 'recapitulatif', loadComponent: () => import('./views/lesEncours/recap/views/recapitulatif/recapitulatif.component').then(m => m.RecapitulatifComponent) },
    ]
  }
];
