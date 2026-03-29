import { Routes } from '@angular/router';
import { ComptesBancairesComponent } from './views/comptes-bancaires/comptes-bancaires.component';
import { BanqueEditComponent } from './views/banque-edit/banque-edit.component';
import { HistoryOflinesComponent } from './views/history/history-oflines/history-oflines.component';

export const COMPTES_BANCAIRES_ROUTES: Routes = [
  {
    path: '',
    children: [
      { path: '', component: ComptesBancairesComponent },
      { path: 'new', component: BanqueEditComponent },
      { path: 'edit/:id', component: BanqueEditComponent },
      { path: 'history/:id', component: HistoryOflinesComponent },
    ]
  }
];
