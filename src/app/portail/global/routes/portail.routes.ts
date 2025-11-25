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
import {LogsComponent} from "../../settings/logs/views/logs/logs.component";
import {EvolTauxPlafondComponent} from "../../tableau-de-bord/views/evol-taux-plafond/evol-taux-plafond.component";
import {SoldeParBanqueComponent} from "../../tableau-de-bord/views/solde-par-banque/solde-par-banque.component";
import {TresorerieGlobaleComponent} from "../../tableau-de-bord/views/tresorerie-globale/tresorerie-globale.component";
import {LesEncoursComponent} from "../../etatsDesEncours/views/parent-componant/les-encours/les-encours.component";
import {EcheancesLeasingComponent} from "../../etatsDesEncours/views/lesEncours/decaissement/echeances-leasing/echeances-leasing.component";
import {EcheancesCmtComponent}from "../../etatsDesEncours/views/lesEncours/decaissement/echeances-cmt/echeances-cmt.component";
import {ChequesEncoursComponent} from "../../etatsDesEncours/views/lesEncours/decaissement/cheques-encours/cheques-encours.component";
import {LcnEncoursComponent} from "../../etatsDesEncours/views/lesEncours/decaissement/lcn-encours/lcn-encours.component";
import {VirementEncoursComponent} from "../../etatsDesEncours/views/lesEncours/decaissement/virement-encours/virement-encours.component";
import {MiseADispositionEncoursComponent} from "../../etatsDesEncours/views/lesEncours/decaissement/mise-a-disposition-encours/mise-a-disposition-encours.component";
import {RapprochemntMensuelleComponent} from "../../etatsDesEncours/rapprochement/views/rapprochemnt-mensuelle/rapprochemnt-mensuelle.component";
import {SelectBanquePourLEncoursComponent} from "../../etatsDesEncours/views/select-banque-pour-l-encours/select-banque-pour-l-encours.component";
import {LcnListComponent} from "../../LCN/views/lcn-list/lcn-list.component";
import {ChequesListComponent} from "../../cheques/views/cheques-list/cheques-list.component";
import {VirementListComponent} from "../../Virements/views/virement-list/virement-list.component";
import {MiseADispositionListComponent} from "../../Mise-à-disposition/views/mise-a-disposition-list/mise-a-disposition-list.component";
import {RecapDecaissementComponent} from "../../etatsDesEncours/views/lesEncours/recap/views/recap-decaissement/recap-decaissement.component";
import {LignesFonctionnellesArchivesComponent} from "../../lignes-fonctionnelles/views/lignes-fonctionnelles-archives/lignes-fonctionnelles-archives.component";
import {TransactionHistoryComponent} from "../../transaction-history/transaction-history.component";
import {MarchesListComponent} from "../../Marches/views/marches-list/marches-list.component";
import {MarchesFormComponent} from "../../Marches/views/marches-form/marches-form.component";
import {SpeficLigneEncoursComponent} from "../../specific-Functional-Lignes/views/spefic-ligne-encours/spefic-ligne-encours.component";
import {SpeficLigneEditComponent} from "../../specific-Functional-Lignes/views/spefic-ligne-edit/spefic-ligne-edit.component";
import {SpeficLigneArchiveesComponent} from "../../specific-Functional-Lignes/views/spefic-ligne-archivees/spefic-ligne-archivees.component";
import {PrefinancementsListComponent} from "../../prefinancements/views/prefinancements-list/prefinancements-list.component";
import {PrefinancementFormComponent} from "../../prefinancements/views/prefinancement-form/prefinancement-form.component";
import {PrefiLinesListComponent} from "../../prefinancements/lignesPrefi/views/prefi-lines-list/prefi-lines-list.component";
import {RecapEncaissementsComponent} from "../../etatsDesEncours/views/lesEncours/recap/views/recap-encaissements/recap-encaissements.component";
import {PrefinancementRemboursementsComponent} from "../../etatsDesEncours/views/lesEncours/decaissement/prefinancement-remboursements/prefinancement-remboursements.component";
import {PrefinancementDeblocageComponent} from "../../etatsDesEncours/views/lesEncours/encaissements/prefinancement-deblocage/prefinancement-deblocage.component";
import {DroitConstateListComponent} from "../../droitsConstate/views/droit-constate-list/droit-constate-list.component";
import {DroitConstateFormComponent} from "../../droitsConstate/views/droit-constate-form/droit-constate-form.component";
import {DcLinesListComponent} from "../../droitsConstate/LignesDroitsConstate/views/dc-lines-list/dc-lines-list.component";
import {DroitConstateRemboursementsComponent} from "../../etatsDesEncours/views/lesEncours/decaissement/droit-constate-remboursements/droit-constate-remboursements.component";
import {DroitConstateDeblocageComponent} from "../../etatsDesEncours/views/lesEncours/encaissements/droit-constate-deblocage/droit-constate-deblocage.component";
import {DecomptesListComponent} from "../../decomptes/views/decomptes-list/decomptes-list.component";
import {DecompteFormComponent} from "../../decomptes/views/decomptes-form/decomptes-form.component";
import {DecomptesComponent} from "../../etatsDesEncours/views/lesEncours/encaissements/decomptes/decomptes.component";
import {ValidatedDcComponent} from "../../droitsConstate/LignesDroitsConstate/SuivieDesLignes/views/validated-dc/validated-dc.component";
import {ValidatedPrefiComponent} from "../../prefinancements/lignesPrefi/suivieDesLignes/views/validated-prefi/validated-prefi.component";
import {HistoryOflinesComponent} from "../../comptes-bancaire/views/history/history-oflines/history-oflines.component";
import {CommissionListComponent} from "../../commission/views/CommissionListComponent/commission-list.component";

import {
  EncoursCommissionsComponent
} from "../../etatsDesEncours/views/lesEncours/decaissement/commission/encours-commissions.component";
import {CommissionFormComponent} from "../../commission/views/commission-form/commission-form.component";
import {AgiosListComponent} from "../../agios/views/agios-list/agios-list.component";
import {AgiosFormComponent} from "../../agios/views/agios-form/agios.form.component";
import {FraisBancaireListComponent} from "../../frais-bancaire/views/frais-bancaire-list/frais-bancaire-list.component";
import {FraisBancaireFormComponent} from "../../frais-bancaire/views/frais-bancaire-form/frais-bancaire-form.component";
import {InteretListComponent} from "../../interest/views/interest-list/interet-list.component";
import {InteretFormComponent} from "../../interest/views/interest-form/interet-form.component";
import {EncoursAgiosComponent} from "../../etatsDesEncours/views/lesEncours/decaissement/agios/encours-agios.component";
import {
  EncoursInteretComponent
} from "../../etatsDesEncours/views/lesEncours/decaissement/interet/encours-interet.component";
import {
  EncoursFraisBancaireComponent
} from "../../etatsDesEncours/views/lesEncours/decaissement/frais-bancaire/encours-fraisBancaire.component";
import {
  FactoringLinesListComponent
} from "../../factoring/LignesFactoring/views/factoring-line-list/factoring-lines-list.component";
import {FactoringFormComponent} from "../../factoring/views/factoring-form/factoring-form.component";
import {FactoringListComponent} from "../../factoring/views/factoring-list/factoring-list.component";
import {
  FactoringRemboursementsComponent
} from "../../etatsDesEncours/views/lesEncours/decaissement/factoring-remboursement/factoring-remboursements.component";
import {
  FactoringDeblocageComponent
} from "../../etatsDesEncours/views/lesEncours/encaissements/factoring-deblocage/factoring-deblocage.component";
import {
  ValidatedFactoringComponent
} from "../../factoring/LignesFactoring/SuivieDesLignes/views/validated-factoring.component";
import {
  VirementEncaissementEncoursComponent
} from "../../etatsDesEncours/views/lesEncours/encaissements/virement-encours/virement-encours.component";
import {PrelevementListComponent} from "../../prelevements/views/prelevement-list/prelevement-list.component";
import {PrelevementFormComponent} from "../../prelevements/views/prelevement-form/prelevement-form.component";
import {
  EncoursPrelevementComponent
} from "../../etatsDesEncours/views/lesEncours/decaissement/prelevement/encours-prelevement.component";

export const PORTAIL_ROUTES: Routes = [
  { path: '', redirectTo: 'accueil', pathMatch: 'full'},
  { path: 'accueil', component: AcceuilComponent  },

  { path: 'tableau-de-bord', component: TableauDeBordComponent , canActivate: [adminGuard]},
  { path: 'evolution-taux-plafond', component: EvolTauxPlafondComponent, canActivate: [adminGuard] },
  { path: 'solde-par-banque', component: SoldeParBanqueComponent, canActivate: [adminGuard] },
  { path: 'tresorerie-globale', component: TresorerieGlobaleComponent, canActivate: [adminGuard] },

  { path: 'comptes-bancaires', component: ComptesBancairesComponent  },
  { path: "comptes-bancaires/new", component: BanqueEditComponent },
  { path: "comptes-bancaires/edit/:id", component: BanqueEditComponent },

  {path: "comptes-bancaires/history/:id", component: HistoryOflinesComponent},
  { path: 'lignes-fonctionnelles-encours', component: LignesFonctionnellesComponent},
  { path: 'lignes-fonctionnelles-encours/new', component: LigneFonctionnelleEditComponent },
  { path: 'lignes-fonctionnelles-encours/edit/:id', component: LigneFonctionnelleEditComponent },
  { path: 'lignes-fonctionnelles-archived', component: LignesFonctionnellesArchivesComponent},

  { path: 'lignes-fonctionnelles-specifiques-encours', component: SpeficLigneEncoursComponent },
  { path: 'lignes-fonctionnelles-specifiques-encours/new', component: SpeficLigneEditComponent },
  { path: 'lignes-fonctionnelles-specifiques-encours/edit/:id', component: SpeficLigneEditComponent },
  { path: 'lignes-fonctionnelles-specifiques-archived', component: SpeficLigneArchiveesComponent },

  { path: 'natures-fonctionnelles', component: NatureLigneFonctionnelleListComponent },
  { path: 'natures-fonctionnelles/new', component: NatureLigneFonctionnelleEditComponent },
  { path: 'natures-fonctionnelles/edit/:id', component: NatureLigneFonctionnelleEditComponent },

  {path: 'leasing', loadChildren: () => import('../../leasing/route/leasing-routing.module').then(m => m.LEASING_FEATURE_ROUTES)},
  {path:"cmt",loadChildren:()=>import('../../cmt/route/cmt-routing').then(m=>m.cmtRoutes)},

  {path:"LCN",component:LcnListComponent},
  {path:"Chèques",component:ChequesListComponent},
  {path:"Virements",component:VirementListComponent},
  {path:"Mise-a-disposition",component:MiseADispositionListComponent},

  { path: "marches", component: MarchesListComponent },
  { path: "marches/new", component: MarchesFormComponent },
  { path: "marches/edit/:id", component: MarchesFormComponent },

  { path: 'préfinancements', component: PrefinancementsListComponent },
  { path: 'préfinancements/new', component: PrefinancementFormComponent },
  { path: 'préfinancements/edit/:id', component: PrefinancementFormComponent },
  { path: 'prefi-lines', component: PrefiLinesListComponent },
  { path:"prefi-line/:id/encours/validated",component:ValidatedPrefiComponent},

  {path:"droits-constate",component:DroitConstateListComponent},
  {path:"droits-constate/new",component:DroitConstateFormComponent},
  {path:"droits-constate/edit/:id",component:DroitConstateFormComponent},
  {path:"dc-lines",component:DcLinesListComponent},
  {path:"dc-line/:id/encours/validated",component:ValidatedDcComponent},

  {path:"factoring",component:FactoringListComponent},
  {path:"factoring/new",component:FactoringFormComponent},
  {path:"factoring/edit/:id",component:FactoringFormComponent},
  {path:"factoring-lines",component:FactoringLinesListComponent},
  {path:"factoring-line/:id/encours/validated",component:ValidatedFactoringComponent},

  {path:"decomptes",component:DecomptesListComponent},
  {path:"decomptes/new",component:DecompteFormComponent},
  {path:"decomptes/edit/:id",component:DecompteFormComponent},

  { path: 'etats-des-encours/:banqueId',component:LesEncoursComponent,
    children:[
      {path:'', redirectTo:'rapprochement', pathMatch:'full'},

      {path: 'rapprochement',component:RapprochemntMensuelleComponent},

      {path:"recap-decaissement",component:RecapDecaissementComponent},
      {path:"recap-encaissement",component:RecapEncaissementsComponent},

      {path:'echeances-leasing', component:EcheancesLeasingComponent},
      {path:'echeances-cmt', component:EcheancesCmtComponent},
      {path:'cheques', component:ChequesEncoursComponent},
      {path:'lcn', component:LcnEncoursComponent},
      {path:'virements-decaissements', component:VirementEncoursComponent},
      {path:'virements-encaissements',component:VirementEncaissementEncoursComponent},
      {path:'mise-a-disposition', component:MiseADispositionEncoursComponent},

      {path:"prefinancement-remboursement",component:PrefinancementRemboursementsComponent},
      {path:"dc-remboursement",component:DroitConstateRemboursementsComponent},
      {path:"factoring-remboursement",component:FactoringRemboursementsComponent},

      {path:"prefinancement-deblocage",component:PrefinancementDeblocageComponent},
      {path:"dc-deblocage",component:DroitConstateDeblocageComponent},
      {path:"factoring-deblocage",component:FactoringDeblocageComponent},

      {path:"decomptes",component:DecomptesComponent},

      {path:"commissions",component:EncoursCommissionsComponent},
      {path:"agios",component:EncoursAgiosComponent},
      {path:"prelevements",component:EncoursPrelevementComponent},
      {path:"interests",component:EncoursInteretComponent},
      {path:"fraisBancaire",component:EncoursFraisBancaireComponent},
    ]
  },

  { path: 'commissions', component: CommissionListComponent },
  {path:"commissions/new",component:CommissionFormComponent},
  {path:"commissions/edit/:id",component:CommissionFormComponent},

  { path: 'agios', component: AgiosListComponent },
  {path:"agios/new",component:AgiosFormComponent},
  {path:"agios/edit/:id",component:AgiosFormComponent},

  { path: 'prelevements', component: PrelevementListComponent },
  {path:"prelevements/new",component:PrelevementFormComponent},
  {path:"prelevements/edit/:id",component:PrelevementFormComponent},

  { path: 'frais-bancaire', component:  FraisBancaireListComponent},
  {path:"frais-bancaire/new",component:FraisBancaireFormComponent},
  {path:"frais-bancaire/edit/:id",component:FraisBancaireFormComponent},

  { path: 'frais-bancaire', component:  FraisBancaireListComponent},
  {path:"frais-bancaire/new",component:FraisBancaireFormComponent},
  {path:"frais-bancaire/edit/:id",component:FraisBancaireFormComponent},

  { path: 'interets', component:  InteretListComponent},
  {path:"interets/new",component:InteretFormComponent},
  {path:"interets/edit/:id",component:InteretFormComponent},

  { path: 'etats-des-encours',component:SelectBanquePourLEncoursComponent},
  {path:"historique-des-transactions",component:TransactionHistoryComponent},

  { path: 'logs', component: LogsComponent , canActivate: [adminGuard]},
];
