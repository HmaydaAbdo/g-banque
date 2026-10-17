import { Routes } from '@angular/router';

export const TRESORERIE_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./views/tresorerie-previsionnelle/tresorerie-previsionnelle.component')
      .then(m => m.TresoreriePrevisionnelleComponent)
  },
  {
    // Plan d'une banque : page dédiée (récit et tableau), ouverte depuis la carte de la banque ou l'écran global
    path: 'banque/:banqueId',
    loadComponent: () => import('./views/plan-banque/plan-banque.component')
      .then(m => m.PlanBanqueComponent)
  }
];
