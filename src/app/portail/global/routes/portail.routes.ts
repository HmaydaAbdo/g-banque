import { Routes } from '@angular/router';
import { AcceuilComponent } from "../../accueil/views/acceuil/acceuil.component";
import { ComptesBancairesComponent } from "../../comptes-bancaire/views/comptes-bancaires/comptes-bancaires.component";
import { LignesFonctionnellesComponent } from "../../lignes-fonctionnelles/views/lignes-fonctionnelles/lignes-fonctionnelles.component";
import { TableauDeBordComponent } from "../../tableau-de-bord/views/tableau-de-bord/tableau-de-bord.component";
import { BanqueEditComponent } from "../../comptes-bancaire/views/banque-edit/banque-edit.component";
import {LigneFonctionnelleEditComponent} from "../../lignes-fonctionnelles/views/ligne-fonctionnelle-edit/ligne-fonctionnelle-edit.component";
import {adminGuard} from "../../../auth/guards/admin.guard";
import {NatureLigneFonctionnelleListComponent} from "../../lignes-fonctionnelles/views/nature-ligne-fonctionnelle-list/nature-ligne-fonctionnelle-list.component";
import {NatureLigneFonctionnelleEditComponent} from "../../lignes-fonctionnelles/views/nature-ligne-fonctionnelle-edit/nature-ligne-fonctionnelle-edit.component";
import {BankHistoryComponent} from "../../comptes-bancaire/views/bank-history/bank-history.component";
import {LogsComponent} from "../../settings/logs/views/logs/logs.component";
import {EvolTauxPlafondComponent} from "../../tableau-de-bord/views/evol-taux-plafond/evol-taux-plafond.component";
import {SoldeParBanqueComponent} from "../../tableau-de-bord/views/solde-par-banque/solde-par-banque.component";
import {TresorerieGlobaleComponent} from "../../tableau-de-bord/views/tresorerie-globale/tresorerie-globale.component";
import {SelectForHistoryComponent} from "../../comptes-bancaire/views/select-for-history/select-for-history.component";


export const PORTAIL_ROUTES: Routes = [
  { path: '', redirectTo: 'accueil', pathMatch: 'full'},
  { path: 'accueil', component: AcceuilComponent  },
  { path: 'comptes-bancaires', component: ComptesBancairesComponent  },
  { path: "select-for-history", component: SelectForHistoryComponent },
  { path: "comptes-bancaires/new", component: BanqueEditComponent },
  { path: "comptes-bancaires/edit/:id", component: BanqueEditComponent },
  { path: "comptes-bancaires/history/:id", component: BankHistoryComponent },
  { path: 'lignes-fonctionnelles', component: LignesFonctionnellesComponent},
  { path: 'lignes-fonctionnelles/new', component: LigneFonctionnelleEditComponent },
  { path: 'lignes-fonctionnelles/edit/:id', component: LigneFonctionnelleEditComponent },
  { path: 'natures-fonctionnelles', component: NatureLigneFonctionnelleListComponent },
  { path: 'natures-fonctionnelles/new', component: NatureLigneFonctionnelleEditComponent },
  { path: 'natures-fonctionnelles/edit/:id', component: NatureLigneFonctionnelleEditComponent },
  { path: 'tableau-de-bord', component: TableauDeBordComponent , canActivate: [adminGuard]},
  { path: 'evolution-taux-plafond', component: EvolTauxPlafondComponent, canActivate: [adminGuard] },
  { path: 'solde-par-banque', component: SoldeParBanqueComponent, canActivate: [adminGuard] },
  { path: 'tresorerie-globale', component: TresorerieGlobaleComponent, canActivate: [adminGuard] },
  { path: 'logs', component: LogsComponent , canActivate: [adminGuard]},
];
