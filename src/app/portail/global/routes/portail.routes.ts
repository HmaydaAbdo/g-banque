import { Routes } from '@angular/router';
import { AcceuilComponent } from "../../accueil/views/acceuil/acceuil.component";
import { ComptesBancairesComponent } from "../../comptes-bancaire/views/comptes-bancaires/comptes-bancaires.component";
import { LignesFonctionnellesComponent } from "../../lignes-fonctionnelles/views/lignes-fonctionnelles-encours/lignes-fonctionnelles.component";
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
import {LesEncoursComponent} from "../../etatsDesEncours/views/parent-componant/les-encours/les-encours.component";
import {
  EcheancesLeasingComponent
} from "../../etatsDesEncours/views/lesEncours/decaissement/leasing/views/echeances-leasing/echeances-leasing.component";
import {
  EcheancesCmtComponent
} from "../../etatsDesEncours/views/lesEncours/decaissement/cmt/views/echeances-cmt/echeances-cmt.component";
import {
  ChequesEncoursComponent
} from "../../etatsDesEncours/views/lesEncours/decaissement/cheques-encours/cheques-encours.component";
import {LcnEncoursComponent} from "../../etatsDesEncours/views/lesEncours/decaissement/lcn-encours/lcn-encours.component";
import {
  VirementEncoursComponent
} from "../../etatsDesEncours/views/lesEncours/decaissement/virement-encours/virement-encours.component";
import {
  MiseADispositionEncoursComponent
} from "../../etatsDesEncours/views/lesEncours/decaissement/mise-a-disposition-encours/mise-a-disposition-encours.component";
import {
  RapprochemntMensuelleComponent
} from "../../etatsDesEncours/rapprochement/views/rapprochemnt-mensuelle/rapprochemnt-mensuelle.component";
import {
  SelectBanquePourLEncoursComponent
} from "../../etatsDesEncours/views/select-banque-pour-l-encours/select-banque-pour-l-encours.component";
import {LcnListComponent} from "../../LCN/views/lcn-list/lcn-list.component";
import {ChequesListComponent} from "../../cheques/views/cheques-list/cheques-list.component";
import {VirementListComponent} from "../../Virements/views/virement-list/virement-list.component";
import {
  MiseADispositionListComponent
} from "../../Mise-à-disposition/views/mise-a-disposition-list/mise-a-disposition-list.component";
import {
  RecapDecaissementComponent
} from "../../etatsDesEncours/views/lesEncours/decaissement/recap/views/recap-decaissement/recap-decaissement.component";
import {
  LignesFonctionnellesExpireesComponent
} from "../../lignes-fonctionnelles/views/lignes-fonctionnelles-expirees/lignes-fonctionnelles-expirees.component";
import {
  LignesFonctionnellesArchivesComponent
} from "../../lignes-fonctionnelles/views/lignes-fonctionnelles-archives/lignes-fonctionnelles-archives.component";
import {TransactionHistoryComponent} from "../../transaction-history/transaction-history.component";

export const PORTAIL_ROUTES: Routes = [
  { path: '', redirectTo: 'accueil', pathMatch: 'full'},
  { path: 'accueil', component: AcceuilComponent  },
  { path: 'comptes-bancaires', component: ComptesBancairesComponent  },
  { path: "comptes-bancaires/new", component: BanqueEditComponent },
  { path: "comptes-bancaires/edit/:id", component: BanqueEditComponent },
  { path: "comptes-bancaires/history/:id", component: BankHistoryComponent },
  { path: 'lignes-fonctionnelles-encours', component: LignesFonctionnellesComponent},
  { path: 'lignes-fonctionnelles-encours/new', component: LigneFonctionnelleEditComponent },
  { path: 'lignes-fonctionnelles-encours/edit/:id', component: LigneFonctionnelleEditComponent },
  { path: 'lignes-fonctionnelles-expired', component: LignesFonctionnellesExpireesComponent},
  { path: 'lignes-fonctionnelles-archived', component: LignesFonctionnellesArchivesComponent},
  { path: 'natures-fonctionnelles', component: NatureLigneFonctionnelleListComponent },
  { path: 'natures-fonctionnelles/new', component: NatureLigneFonctionnelleEditComponent },
  { path: 'natures-fonctionnelles/edit/:id', component: NatureLigneFonctionnelleEditComponent },
  { path: 'tableau-de-bord', component: TableauDeBordComponent , canActivate: [adminGuard]},
  { path: 'evolution-taux-plafond', component: EvolTauxPlafondComponent, canActivate: [adminGuard] },
  { path: 'solde-par-banque', component: SoldeParBanqueComponent, canActivate: [adminGuard] },
  { path: 'tresorerie-globale', component: TresorerieGlobaleComponent, canActivate: [adminGuard] },
  { path: 'logs', component: LogsComponent , canActivate: [adminGuard]},
  {path: 'leasing', loadChildren: () => import('../../leasing/route/leasing-routing.module').then(m => m.LEASING_FEATURE_ROUTES)},
  {path:"cmt",loadChildren:()=>import('../../cmt/route/cmt-routing').then(m=>m.cmtRoutes)},
  { path: 'etats-des-encours/:banqueId',component:LesEncoursComponent,
    children:[
      {path:'', redirectTo:'rapprochement', pathMatch:'full'},
      {path:'echeances-leasing', component:EcheancesLeasingComponent},
      {path:'echeances-cmt', component:EcheancesCmtComponent},
      {path:'cheques', component:ChequesEncoursComponent},
      {path:'lcn', component:LcnEncoursComponent},
      {path:'virements', component:VirementEncoursComponent},
      {path:'mise-a-disposition', component:MiseADispositionEncoursComponent},
      {path: 'rapprochement',component:RapprochemntMensuelleComponent},
      {path:"recap-decaissement",component:RecapDecaissementComponent}
    ]
  },
  { path: 'etats-des-encours',component:SelectBanquePourLEncoursComponent},
  {path:"LCN",component:LcnListComponent},
  {path:"Chèques",component:ChequesListComponent},
  {path:"Virements",component:VirementListComponent},
  {path:"Mise-a-disposition",component:MiseADispositionListComponent},
  {path:"historique-des-transactions",component:TransactionHistoryComponent}

];
