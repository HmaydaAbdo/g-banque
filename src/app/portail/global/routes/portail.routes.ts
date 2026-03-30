import { Routes } from '@angular/router';
import { AcceuilComponent } from "../../accueil/views/acceuil/acceuil.component";
import { LignesFonctionnellesComponent } from "../../lignes-fonctionnelles/views/lignes-fonctionnelles-encours/lignes-fonctionnelles.component";
import { LigneFonctionnelleEditComponent } from "../../lignes-fonctionnelles/views/ligne-fonctionnelle-edit/ligne-fonctionnelle-edit.component";
import { adminGuard } from "../../../auth/guards/admin.guard";
import { LogsComponent } from "../../settings/logs/views/logs/logs.component";
import { LesEncoursComponent } from "../../etatsDesEncours/views/parent-componant/les-encours/les-encours.component";
import { EcheancesLeasingComponent } from "../../etatsDesEncours/views/lesEncours/decaissement/echeances-leasing/echeances-leasing.component";
import { EcheancesCmtComponent } from "../../etatsDesEncours/views/lesEncours/decaissement/echeances-cmt/echeances-cmt.component";
import { ChequesEncoursComponent } from "../../etatsDesEncours/views/lesEncours/decaissement/cheques-encours/cheques-encours.component";
import { LcnEncoursComponent } from "../../etatsDesEncours/views/lesEncours/decaissement/lcn-encours/lcn-encours.component";
import { VirementEncoursComponent } from "../../etatsDesEncours/views/lesEncours/decaissement/virement-encours/virement-encours.component";
import { MiseADispositionEncoursComponent } from "../../etatsDesEncours/views/lesEncours/decaissement/mise-a-disposition-encours/mise-a-disposition-encours.component";
import { RapprochemntEncoursComponent } from "../../etatsDesEncours/views/lesEncours/rapprochemnt-encours/rapprochemnt-encours.component";
import { LcnDecaissementsListComponent } from "../../LCN/views/lcn-decaissements-list/lcn-decaissements-list.component";
import { ChequesDecaissementsListComponent } from "../../cheques/views/cheques-decaissements-list/cheques-decaissements-list.component";
import { VirementDecaissementListComponent } from "../../Virements/views/virement-decaissement-list/virement-decaissement-list.component";
import { MiseADispositionDecaissementListComponent } from "../../Mise-à-disposition/views/mise-a-disposition-decaissement-list/mise-a-disposition-decaissement-list.component";
import { RecapDecaissementComponent } from "../../etatsDesEncours/views/lesEncours/recap/views/recap-decaissement/recap-decaissement.component";
import { LignesFonctionnellesArchivesComponent } from "../../lignes-fonctionnelles/views/lignes-fonctionnelles-archives/lignes-fonctionnelles-archives.component";
import { TransactionHistoryComponent } from "../../transactions/views/transaction-history/transaction-history.component";
import { MarchesListComponent } from "../../Marches/views/marches-list/marches-list.component";
import { MarchesFormComponent } from "../../Marches/views/marches-form/marches-form.component";
import { SpeficLigneEncoursComponent } from "../../specific-Functional-Lignes/views/spefic-ligne-encours/spefic-ligne-encours.component";
import { SpeficLigneEditComponent } from "../../specific-Functional-Lignes/views/spefic-ligne-edit/spefic-ligne-edit.component";
import { SpeficLigneArchiveesComponent } from "../../specific-Functional-Lignes/views/spefic-ligne-archivees/spefic-ligne-archivees.component";
import { PrefinancementsListComponent } from "../../prefinancements/views/prefinancements-list/prefinancements-list.component";
import { PrefinancementFormComponent } from "../../prefinancements/views/prefinancement-form/prefinancement-form.component";
import { RecapEncaissementsComponent } from "../../etatsDesEncours/views/lesEncours/recap/views/recap-encaissements/recap-encaissements.component";
import { PrefinancementRemboursementsComponent } from "../../etatsDesEncours/views/lesEncours/decaissement/prefinancement-remboursements/prefinancement-remboursements.component";
import { PrefinancementDeblocageComponent } from "../../etatsDesEncours/views/lesEncours/encaissements/prefinancement-deblocage/prefinancement-deblocage.component";
import { DroitConstateListComponent } from "../../droitsConstate/views/droit-constate-list/droit-constate-list.component";
import { DroitConstateFormComponent } from "../../droitsConstate/views/droit-constate-form/droit-constate-form.component";
import { DroitConstateRemboursementsComponent } from "../../etatsDesEncours/views/lesEncours/decaissement/droit-constate-remboursements/droit-constate-remboursements.component";
import { DroitConstateDeblocageComponent } from "../../etatsDesEncours/views/lesEncours/encaissements/droit-constate-deblocage/droit-constate-deblocage.component";
import { DecomptesListComponent } from "../../decomptes/views/decomptes-list/decomptes-list.component";
import { DecompteFormComponent } from "../../decomptes/views/decomptes-form/decomptes-form.component";
import { DecomptesComponent } from "../../etatsDesEncours/views/lesEncours/encaissements/decomptes/decomptes.component";
import { ValidatedDcComponent } from "../../droitsConstate/LignesDroitsConstate/SuivieDesLignes/views/validated-dc/validated-dc.component";
import { ValidatedPrefiComponent } from "../../prefinancements/lignesPrefi/suivieDesLignes/views/validated-prefi/validated-prefi.component";
import { CommissionListComponent } from "../../commission/views/CommissionListComponent/commission-list.component";
import { EncoursCommissionsComponent } from "../../etatsDesEncours/views/lesEncours/decaissement/commission/encours-commissions.component";
import { CommissionFormComponent } from "../../commission/views/commission-form/commission-form.component";
import { AgiosListComponent } from "../../agios/views/agios-list/agios-list.component";
import { AgiosFormComponent } from "../../agios/views/agios-form/agios.form.component";
import { FraisBancaireListComponent } from "../../frais-bancaire/views/frais-bancaire-list/frais-bancaire-list.component";
import { FraisBancaireFormComponent } from "../../frais-bancaire/views/frais-bancaire-form/frais-bancaire-form.component";
import { EncoursAgiosComponent } from "../../etatsDesEncours/views/lesEncours/decaissement/agios/encours-agios.component";
import { EncoursFraisBancaireComponent } from "../../etatsDesEncours/views/lesEncours/decaissement/frais-bancaire/encours-fraisBancaire.component";
import { FactoringFormComponent } from "../../factoring/views/factoring-form/factoring-form.component";
import { FactoringListComponent } from "../../factoring/views/factoring-list/factoring-list.component";
import { FactoringRemboursementsComponent } from "../../etatsDesEncours/views/lesEncours/decaissement/factoring-remboursement/factoring-remboursements.component";
import { FactoringDeblocageComponent } from "../../etatsDesEncours/views/lesEncours/encaissements/factoring-deblocage/factoring-deblocage.component";
import { ValidatedFactoringComponent } from "../../factoring/LignesFactoring/SuivieDesLignes/views/validated-factoring.component";
import { VirementEncaissementEncoursComponent } from "../../etatsDesEncours/views/lesEncours/encaissements/virement-encours/virement-encours.component";
import { PrelevementListComponent } from "../../prelevements/views/prelevement-list/prelevement-list.component";
import { PrelevementFormComponent } from "../../prelevements/views/prelevement-form/prelevement-form.component";
import { EncoursPrelevementComponent } from "../../etatsDesEncours/views/lesEncours/decaissement/prelevement/encours-prelevement.component";
import { VirementFormComponent } from "../../Virements/views/virement-form/virement-form.component";
import { VirementEncaissementListComponent } from "../../Virements/views/virement-encaissement-list/virement-encaissement-list.component";
import { ChequesEncaissementsListComponent } from "../../cheques/views/cheques-encaissements-list/cheques-encaissements-list.component";
import { ChequesFormComponent } from "../../cheques/views/cheques-form/cheques-form.component";
import { ChequeEncaissementsEncoursComponent } from "../../etatsDesEncours/views/lesEncours/encaissements/cheque-encaissements-encours/cheque-encaissements-encours.component";
import { LcnFormComponent } from "../../LCN/views/lcn-form/lcn-form.component";
import { LcnEncaissementsListComponent } from "../../LCN/views/lcn-encaissements-list/lcn-encaissements-list.component";
import { EncaissementLcnEncoursComponent } from "../../etatsDesEncours/views/lesEncours/encaissements/encaissement-lcn-encours/encaissement-lcn-encours.component";
import { MiseADispositionFormComponent } from "../../Mise-à-disposition/views/mise-a-disposition-form/mise-a-disposition-form.component";
import { MiseADispoEncaissementEncoursComponent } from "../../etatsDesEncours/views/lesEncours/encaissements/mise-a-dispo-encaissement-encours/mise-a-dispo-encaissement-encours.component";
import { HistoriqueDesRapprochementComponent } from "../../rapprochement/views/historique-des-rapprochement/historique-des-rapprochement.component";
import { DetailsRapprochementComponent } from "../../rapprochement/views/details-rapprochement/details-rapprochement.component";
import { FunctionalLinesByMarcheComponent } from "../../Marches/views/specific-lines-by-marche/functionalLines-by-marche.component";
import { LcnEscompteByLineComponent } from "../../LCN/views/lcn-escompte-by-line/lcn-escompte-by-line.component";
import { ImpotEtTaxeListComponent } from "../../impotEtTaxes/views/impot-et-taxe-list/impot-et-taxe-list.component";
import { ImpotEtTaxeFormComponent } from "../../impotEtTaxes/views/impot-et-taxe-form/impot-et-taxe-form.component";
import { EncoursImpotsEtTaxesComponent } from "../../etatsDesEncours/views/lesEncours/decaissement/encours-impots-taxes/encours-impots-taxes.component";
import { RestitutionListComponent } from "../../restitution/views/restitution-list/restitution-list.component";
import { RestitutionFormComponent } from "../../restitution/views/restitution-form/restitution-form.component";
import { EncoursRestitutionComponent } from "../../etatsDesEncours/views/lesEncours/encaissements/restitution/encours-restitution.component";
import { OperationErroneListComponent } from "../../operationErronées/views/operation-errone-list/operation-errone-list.component";
import { OperationErroneFormComponent } from "../../operationErronées/views/operation-errone-form/operation-errone-form.component";
import {
  EncoursOperationErroneComponent
} from "../../etatsDesEncours/views/lesEncours/decaissement/operation-errone/operation-errone.component";
import {
  VirementInternationalListComponent
} from "../../virementInternational/views/virement-international-list/virement-international-list.component";
import {
  VirementInternationalFormComponent
} from "../../virementInternational/views/virement-international-form/virement-international-form.component";
import {
  VirementInternationalEncoursDecaissementComponent
} from "../../etatsDesEncours/views/lesEncours/decaissement/virementInternationaux/VirementInternationalEncoursDecaissement.component";
import {
  VirementInternationalEncoursEncaissementComponent
} from "../../etatsDesEncours/views/lesEncours/encaissements/virementInternationaux/VirementInternationalEncoursEncaissementComponent";
import {ImpayeeListComponent} from "../../impayees/views/ImpayeeList/Impayee-list.component";
import {ImpayeeFormComponent} from "../../impayees/views/ImpayeeForm/Impayee-form.component";
import {
  DecaissementEncoursImpayeeComponent
} from "../../etatsDesEncours/views/lesEncours/decaissement/impayee/decaissement-encours-impayee.component";
import {
  AgiosCalculDashboardComponent
} from "../../agios-calcul/views/agios-calcul-dashboard/agios-calcul-dashboard.component";
import {AgiosCalculDetailComponent} from "../../agios-calcul/views/agios-calcul-detail/agios-calcul-detail.component";
import {
  EncaissementEncoursImpayeeComponent
} from "../../etatsDesEncours/views/lesEncours/encaissements/impayee/encaissement-encours-impayee.component";

export const PORTAIL_ROUTES: Routes = [
  { path: '', redirectTo: 'accueil', pathMatch: 'full' },
  { path: 'accueil', component: AcceuilComponent },

  { path: "rapprochement/details/:id", component: DetailsRapprochementComponent },
  { path: "rapprochement/historique", component: HistoriqueDesRapprochementComponent },

  {
    path: 'comptes-bancaires',
    loadChildren: () => import('../../comptes-bancaire/comptes-bancaire-routing.module')
      .then(m => m.COMPTES_BANCAIRES_ROUTES)
  },

  {
    path: 'natures-fonctionnelles',
    loadChildren: () => import('../../lignes-fonctionnelles/natures-routing.module')
      .then(m => m.NATURES_ROUTES)
  },


  { path: 'lignes-fonctionnelles-encours', component: LignesFonctionnellesComponent },
  { path: 'lignes-fonctionnelles-encours/new', component: LigneFonctionnelleEditComponent },
  { path: 'lignes-fonctionnelles-encours/edit/:id', component: LigneFonctionnelleEditComponent },
  { path: 'lignes-fonctionnelles-archived', component: LignesFonctionnellesArchivesComponent },

  { path: 'lignes-fonctionnelles-specifiques-encours', component: SpeficLigneEncoursComponent },
  { path: 'lignes-fonctionnelles-specifiques-encours/new', component: SpeficLigneEditComponent },
  { path: 'lignes-fonctionnelles-specifiques-encours/edit/:id', component: SpeficLigneEditComponent },
  { path: 'lignes-fonctionnelles-specifiques-archived', component: SpeficLigneArchiveesComponent },
  { path: 'lignes-fonctionnelles-specifiques/marche/:id', component: FunctionalLinesByMarcheComponent },
  { path: "factoring-encours/:lineId", component: ValidatedFactoringComponent },
  { path: "prefi-encours/:lineId", component: ValidatedPrefiComponent },
  { path: "dc-encours/:lineId", component: ValidatedDcComponent },
  { path: "lcn-encours/:lineId", component: LcnEscompteByLineComponent },



  { path: 'leasing', loadChildren: () => import('../../leasing/route/leasing-routing.module').then(m => m.LEASING_FEATURE_ROUTES) },
  { path: "cmt", loadChildren: () => import('../../cmt/route/cmt-routing').then(m => m.cmtRoutes) },

  { path: "LCN/decaissements", component: LcnDecaissementsListComponent },
  { path: "LCN/encaissements", component: LcnEncaissementsListComponent },
  { path: "LCN/new", component: LcnFormComponent },
  { path: "LCN/edit/:id", component: LcnFormComponent },

  { path: "Cheques/decaissements", component: ChequesDecaissementsListComponent },
  { path: "Cheques/encaissements", component: ChequesEncaissementsListComponent },
  { path: 'Cheques/new', component: ChequesFormComponent },
  { path: 'Cheques/edit/:id', component: ChequesFormComponent },

  { path: "Virements/decaissement", component: VirementDecaissementListComponent },
  { path: "Virements/encaissement", component: VirementEncaissementListComponent },
  { path: 'Virements/new', component: VirementFormComponent },
  { path: 'Virements/edit/:id', component: VirementFormComponent },

  {path: 'virements-internationaux', component: VirementInternationalListComponent},
  {path: 'virements-internationaux/new', component: VirementInternationalFormComponent},
  {path: 'virements-internationaux/edit/:id', component: VirementInternationalFormComponent},

  { path: 'operations/errones', component: OperationErroneListComponent },
  { path: 'operations/errones/new', component: OperationErroneFormComponent },
  { path: 'operations/errones/edit/:id', component: OperationErroneFormComponent },

  { path: "Mise-a-disposition/decaissement", component: MiseADispositionDecaissementListComponent },
  { path: "Mise-a-disposition/new", component: MiseADispositionFormComponent },
  { path: 'Mise-a-disposition/edit/:id', component: MiseADispositionFormComponent },

  { path: "marches", component: MarchesListComponent },
  { path: "marches/new", component: MarchesFormComponent },
  { path: "marches/edit/:id", component: MarchesFormComponent },

  { path: 'préfinancements', component: PrefinancementsListComponent },
  { path: 'préfinancements/new', component: PrefinancementFormComponent },
  { path: 'préfinancements/edit/:id', component: PrefinancementFormComponent },

  { path: "droits-constate", component: DroitConstateListComponent },
  { path: "droits-constate/new", component: DroitConstateFormComponent },
  { path: "droits-constate/edit/:id", component: DroitConstateFormComponent },

  { path: "factoring", component: FactoringListComponent },
  { path: "factoring/new", component: FactoringFormComponent },
  { path: "factoring/edit/:id", component: FactoringFormComponent },

  { path: "decomptes", component: DecomptesListComponent },
  { path: "decomptes/new", component: DecompteFormComponent },
  { path: "decomptes/edit/:id", component: DecompteFormComponent },

  {
    path: 'etats-des-encours/:banqueId', component: LesEncoursComponent,
    children: [
      { path: '', redirectTo: 'rapprochement', pathMatch: 'full' },

      { path: 'rapprochement', component: RapprochemntEncoursComponent },

      { path: "recap-decaissement", component: RecapDecaissementComponent },
      { path: "recap-encaissement", component: RecapEncaissementsComponent },

      { path: 'echeances-leasing', component: EcheancesLeasingComponent },
      { path: 'echeances-cmt', component: EcheancesCmtComponent },

      { path: 'lcn-decaissements', component: LcnEncoursComponent },
      { path: 'lcn-encaissements', component: EncaissementLcnEncoursComponent },

      { path: 'virements-decaissements', component: VirementEncoursComponent },
      { path: 'virements-encaissements', component: VirementEncaissementEncoursComponent },

      { path: 'cheques-decaissements', component: ChequesEncoursComponent },
      { path: 'cheques-encaissements', component: ChequeEncaissementsEncoursComponent },

      { path: 'mise-a-disposition-decaissements', component: MiseADispositionEncoursComponent },
      { path: 'mise-a-disposition-encaissements', component: MiseADispoEncaissementEncoursComponent },

      { path: 'virements-internationaux-decaissements', component: VirementInternationalEncoursDecaissementComponent },
      { path: 'virements-internationaux-encaissements', component: VirementInternationalEncoursEncaissementComponent },

      { path: "prefinancement-remboursement", component: PrefinancementRemboursementsComponent },
      { path: "dc-remboursement", component: DroitConstateRemboursementsComponent },
      { path: "factoring-remboursement", component: FactoringRemboursementsComponent },

      { path: "prefinancement-deblocage", component: PrefinancementDeblocageComponent },
      { path: "dc-deblocage", component: DroitConstateDeblocageComponent },
      { path: "factoring-deblocage", component: FactoringDeblocageComponent },

      { path: "decomptes", component: DecomptesComponent },

      { path: "commissions", component: EncoursCommissionsComponent },
      { path: "agios", component: EncoursAgiosComponent },
      { path: "prelevements", component: EncoursPrelevementComponent },
      { path: "fraisBancaire", component: EncoursFraisBancaireComponent },
      { path: "impots-taxes", component: EncoursImpotsEtTaxesComponent },
      { path: "restitutions", component: EncoursRestitutionComponent },
      { path: "impayees-decaissement", component: DecaissementEncoursImpayeeComponent },
      { path: "impayees-encaissement", component: EncaissementEncoursImpayeeComponent },
      { path: "operations-errones", component: EncoursOperationErroneComponent },
    ]
  },

  { path: 'commissions', component: CommissionListComponent },
  { path: "commissions/new", component: CommissionFormComponent },
  { path: "commissions/edit/:id", component: CommissionFormComponent },

  { path: 'impots-taxes', component: ImpotEtTaxeListComponent },
  { path: "impots-taxes/new", component: ImpotEtTaxeFormComponent },
  { path: "impots-taxes/edit/:id", component: ImpotEtTaxeFormComponent },

  { path: 'restitutions', component: RestitutionListComponent },
  { path: "restitutions/new", component: RestitutionFormComponent },
  { path: "restitutions/edit/:id", component: RestitutionFormComponent },

  { path: 'agios', component: AgiosListComponent },
  { path: "agios/new", component: AgiosFormComponent },
  { path: "agios/edit/:id", component: AgiosFormComponent },
  { path: 'agios-calcul', component: AgiosCalculDashboardComponent },
  { path: 'agios-calcul/detail/:banqueId/:type/:trim/:annee', component: AgiosCalculDetailComponent },

  {
    path: 'impayees',
    children: [
      { path: '', component: ImpayeeListComponent },
      { path: 'new', component: ImpayeeFormComponent },
      { path: 'edit/:id', component: ImpayeeFormComponent }
    ]
  },
  { path: 'prelevements', component: PrelevementListComponent },
  { path: "prelevements/new", component: PrelevementFormComponent },
  { path: "prelevements/edit/:id", component: PrelevementFormComponent },

  { path: 'frais-bancaire', component: FraisBancaireListComponent },
  { path: "frais-bancaire/new", component: FraisBancaireFormComponent },
  { path: "frais-bancaire/edit/:id", component: FraisBancaireFormComponent },


  { path: "historique-des-transactions", component: TransactionHistoryComponent },
  { path: 'logs', component: LogsComponent, canActivate: [adminGuard] },
];
