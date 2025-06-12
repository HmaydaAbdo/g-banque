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

export const PORTAIL_ROUTES: Routes = [

  { path: '', redirectTo: 'accueil', pathMatch: 'full'},
  { path: 'accueil', component: AcceuilComponent  },
  { path: 'comptes-bancaires', component: ComptesBancairesComponent  },
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
  { path: 'tableau-de-bord', component: TableauDeBordComponent , canActivate: [adminGuard]},

];
