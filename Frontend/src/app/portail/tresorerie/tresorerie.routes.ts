import { Routes } from '@angular/router';

export const TRESORERIE_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./views/tresorerie-previsionnelle/tresorerie-previsionnelle.component')
      .then(m => m.TresoreriePrevisionnelleComponent)
  }
];
