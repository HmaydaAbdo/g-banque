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
import {LesEncoursComponent} from "../../etatsDesEncours/views/parent-componant/les-encours/les-encours.component";
import {
  EcheancesLeasingComponent
} from "../../etatsDesEncours/views/lesEncours/leasing/views/echeances-leasing/echeances-leasing.component";
import {
  EcheancesCmtComponent
} from "../../etatsDesEncours/views/lesEncours/cmt/views/echeances-cmt/echeances-cmt.component";
import {
  ChequesEncoursComponent
} from "../../etatsDesEncours/views/lesEncours/cheques-encours/cheques-encours.component";
import {LcnEncoursComponent} from "../../etatsDesEncours/views/lesEncours/lcn-encours/lcn-encours.component";
import {
  VirementEncoursComponent
} from "../../etatsDesEncours/views/lesEncours/virement-encours/virement-encours.component";
import {
  MiseADispositionEncoursComponent
} from "../../etatsDesEncours/views/lesEncours/mise-a-disposition-encours/mise-a-disposition-encours.component";
import {
  RapprochemntMensuelleComponent
} from "../../etatsDesEncours/rapprochement/views/rapprochemnt-mensuelle/rapprochemnt-mensuelle.component";
import {
  SelectBanquePourLEncoursComponent
} from "../../etatsDesEncours/views/select-banque-pour-l-encours/select-banque-pour-l-encours.component";
import {RecapRapprochementComponent} from "../../etatsDesEncours/recap-rapprochement/recap-rapprochement.component";
import {LcnActifListComponent} from "../../LCN/views/lcn-actif-list/lcn-actif-list.component";
import {ChequesActifListComponent} from "../../cheques/views/cheques-actif-list/cheques-actif-list.component";
import {VirementActifListComponent} from "../../Virements/views/virement-actif-list/virement-actif-list.component";
import {
  MiseADispositionActifListComponent
} from "../../Mise-à-disposition/views/mise-a-disposition-actif-list/mise-a-disposition-actif-list.component";

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
      {path: 'rapprochement',component:RapprochemntMensuelleComponent}
    ]
  },
  { path: 'etats-des-encours',component:SelectBanquePourLEncoursComponent},
  {path:"récapitulatif",component:RecapRapprochementComponent},
  {path:"LCN",component:LcnActifListComponent},
  {path:"Chèques",component:ChequesActifListComponent},
  {path:"Virements",component:VirementActifListComponent},
  {path:"Mise à disposition",component:MiseADispositionActifListComponent}
];
