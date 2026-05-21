import { Routes } from '@angular/router';

export const RAPPROCHEMENT_ROUTES: Routes = [
  {
    path: 'details/:id',
    loadComponent: () => import('./views/details-rapprochement/details-rapprochement.component').then(m => m.DetailsRapprochementComponent)
  },
  {
    path: 'historique',
    loadComponent: () => import('./views/historique-des-rapprochement/historique-des-rapprochement.component').then(m => m.HistoriqueDesRapprochementComponent)
  }
];
