import { Routes } from '@angular/router';

export const FRAIS_BANCAIRE_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./views/frais-bancaire-list/frais-bancaire-list.component').then(m => m.FraisBancaireListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./views/frais-bancaire-form/frais-bancaire-form.component').then(m => m.FraisBancaireFormComponent)
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./views/frais-bancaire-form/frais-bancaire-form.component').then(m => m.FraisBancaireFormComponent)
  }
];
