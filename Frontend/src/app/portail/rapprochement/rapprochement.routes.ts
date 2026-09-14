import { Routes } from '@angular/router';
import { departmentGuard } from '../../auth/guards/department.guard';

export const RAPPROCHEMENT_ROUTES: Routes = [
  {
    path: 'details/:id',
    loadComponent: () => import('./views/details-rapprochement/details-rapprochement.component').then(m => m.DetailsRapprochementComponent),
    canActivate: [departmentGuard('Finance')]
  },
  {
    path: 'historique',
    loadComponent: () => import('./views/historique-des-rapprochement/historique-des-rapprochement.component').then(m => m.HistoriqueDesRapprochementComponent),
    canActivate: [departmentGuard('Finance')]
  }
];
