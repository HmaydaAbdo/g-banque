import { Routes } from '@angular/router';

export const ETATS_DES_ENCOURS_ROUTES: Routes = [
  {
    path: 'select',
    loadComponent: () => import('./views/select-rapprochement/select-rapprochement.component').then(m => m.SelectRapprochementComponent)
  },
  {
    path: '',
    loadComponent: () => import('./views/parent-componant/les-encours/les-encours.component').then(m => m.LesEncoursComponent),
    children: [
      { path: '', redirectTo: 'rapprochement', pathMatch: 'full' },

      { path: 'rapprochement', loadComponent: () => import('./views/lesEncours/rapprochemnt-encours/rapprochemnt-encours.component').then(m => m.RapprochemntEncoursComponent) },

      { path: 'recap-decaissement', loadComponent: () => import('./views/lesEncours/recap/views/recap-decaissement/recap-decaissement.component').then(m => m.RecapDecaissementComponent) },
      { path: 'recap-encaissement', loadComponent: () => import('./views/lesEncours/recap/views/recap-encaissements/recap-encaissements.component').then(m => m.RecapEncaissementsComponent) },

      { path: 'echeances-leasing', loadComponent: () => import('./views/lesEncours/decaissement/echeances-leasing/echeances-leasing.component').then(m => m.EcheancesLeasingComponent) },
      { path: 'echeances-cmt', loadComponent: () => import('./views/lesEncours/decaissement/echeances-cmt/echeances-cmt.component').then(m => m.EcheancesCmtComponent) },

      { path: 'lcn-decaissements', loadComponent: () => import('./views/lesEncours/decaissement/lcn-encours/lcn-encours.component').then(m => m.LcnEncoursComponent) },
      { path: 'lcn-encaissements', loadComponent: () => import('./views/lesEncours/encaissements/encaissement-lcn-encours/encaissement-lcn-encours.component').then(m => m.EncaissementLcnEncoursComponent) },

      { path: 'virements-decaissements', loadComponent: () => import('./views/lesEncours/decaissement/virement-encours/virement-encours.component').then(m => m.VirementEncoursComponent) },
      { path: 'virements-encaissements', loadComponent: () => import('./views/lesEncours/encaissements/virement-encours/virement-encours.component').then(m => m.VirementEncaissementEncoursComponent) },

      { path: 'cheques-decaissements', loadComponent: () => import('./views/lesEncours/decaissement/cheques-encours/cheques-encours.component').then(m => m.ChequesEncoursComponent) },
      { path: 'cheques-encaissements', loadComponent: () => import('./views/lesEncours/encaissements/cheque-encaissements-encours/cheque-encaissements-encours.component').then(m => m.ChequeEncaissementsEncoursComponent) },

      { path: 'mise-a-disposition-decaissements', loadComponent: () => import('./views/lesEncours/decaissement/mise-a-disposition-encours/mise-a-disposition-encours.component').then(m => m.MiseADispositionEncoursComponent) },
      { path: 'mise-a-disposition-encaissements', loadComponent: () => import('./views/lesEncours/encaissements/mise-a-dispo-encaissement-encours/mise-a-dispo-encaissement-encours.component').then(m => m.MiseADispoEncaissementEncoursComponent) },

      { path: 'virements-internationaux-decaissements', loadComponent: () => import('./views/lesEncours/decaissement/virementInternationaux/VirementInternationalEncoursDecaissement.component').then(m => m.VirementInternationalEncoursDecaissementComponent) },
      { path: 'virements-internationaux-encaissements', loadComponent: () => import('./views/lesEncours/encaissements/virementInternationaux/VirementInternationalEncoursEncaissementComponent').then(m => m.VirementInternationalEncoursEncaissementComponent) },

      { path: 'prefinancement-remboursement', loadComponent: () => import('./views/lesEncours/decaissement/prefinancement-remboursements/prefinancement-remboursements.component').then(m => m.PrefinancementRemboursementsComponent) },
      { path: 'dc-remboursement', loadComponent: () => import('./views/lesEncours/decaissement/droit-constate-remboursements/droit-constate-remboursements.component').then(m => m.DroitConstateRemboursementsComponent) },
      { path: 'factoring-remboursement', loadComponent: () => import('./views/lesEncours/decaissement/factoring-remboursement/factoring-remboursements.component').then(m => m.FactoringRemboursementsComponent) },

      { path: 'prefinancement-deblocage', loadComponent: () => import('./views/lesEncours/encaissements/prefinancement-deblocage/prefinancement-deblocage.component').then(m => m.PrefinancementDeblocageComponent) },
      { path: 'dc-deblocage', loadComponent: () => import('./views/lesEncours/encaissements/droit-constate-deblocage/droit-constate-deblocage.component').then(m => m.DroitConstateDeblocageComponent) },
      { path: 'factoring-deblocage', loadComponent: () => import('./views/lesEncours/encaissements/factoring-deblocage/factoring-deblocage.component').then(m => m.FactoringDeblocageComponent) },

      { path: 'decomptes', loadComponent: () => import('./views/lesEncours/encaissements/decomptes/decomptes.component').then(m => m.DecomptesComponent) },

      { path: 'commissions', loadComponent: () => import('./views/lesEncours/decaissement/commission/encours-commissions.component').then(m => m.EncoursCommissionsComponent) },
      { path: 'agios', loadComponent: () => import('./views/lesEncours/decaissement/agios/encours-agios.component').then(m => m.EncoursAgiosComponent) },
      { path: 'prelevements', loadComponent: () => import('./views/lesEncours/decaissement/prelevement/encours-prelevement.component').then(m => m.EncoursPrelevementComponent) },
      { path: 'fraisBancaire', loadComponent: () => import('./views/lesEncours/decaissement/frais-bancaire/encours-fraisBancaire.component').then(m => m.EncoursFraisBancaireComponent) },
      { path: 'impots-taxes', loadComponent: () => import('./views/lesEncours/decaissement/encours-impots-taxes/encours-impots-taxes.component').then(m => m.EncoursImpotsEtTaxesComponent) },
      { path: 'restitutions', loadComponent: () => import('./views/lesEncours/encaissements/restitution/encours-restitution.component').then(m => m.EncoursRestitutionComponent) },
      { path: 'impayees-decaissement', loadComponent: () => import('./views/lesEncours/decaissement/impayee/decaissement-encours-impayee.component').then(m => m.DecaissementEncoursImpayeeComponent) },
      { path: 'impayees-encaissement', loadComponent: () => import('./views/lesEncours/encaissements/impayee/encaissement-encours-impayee.component').then(m => m.EncaissementEncoursImpayeeComponent) },
      { path: 'operations-errones', loadComponent: () => import('./views/lesEncours/decaissement/operation-errone/operation-errone.component').then(m => m.EncoursOperationErroneComponent) },
    ]
  }
];
