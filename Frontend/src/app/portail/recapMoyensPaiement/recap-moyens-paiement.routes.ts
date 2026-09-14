import { Routes } from '@angular/router';
import { departmentGuard } from '../../auth/guards/department.guard';

export const RECAP_MOYENS_PAIEMENT_ROUTES: Routes = [
  {
    path: 'encaissements',
    canActivate: [departmentGuard('Finance')],
    loadComponent: () => import('./views/recap-moyens-paiement-encaissements/recap-moyens-paiement-encaissements.component')
      .then(m => m.RecapMoyensPaiementEncaissementsComponent)
  },
  {
    path: 'decaissements',
    canActivate: [departmentGuard('Finance')],
    loadComponent: () => import('./views/recap-moyens-paiement-decaissements/recap-moyens-paiement-decaissements.component')
      .then(m => m.RecapMoyensPaiementDecaissementsComponent)
  },
];
