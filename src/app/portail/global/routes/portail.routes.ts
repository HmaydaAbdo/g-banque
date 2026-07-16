import { Routes } from '@angular/router';
import { systemAdminGuard } from "../../../auth/guards/system-admin.guard";
import { departmentGuard } from "../../../auth/guards/department.guard";
import { accueilDispatchGuard } from "../../../auth/guards/accueil-dispatch.guard";

export const PORTAIL_ROUTES: Routes = [

  { path: '', redirectTo: 'accueil', pathMatch: 'full' },

  /**
   * Accueil dispatcher — redirige chaque utilisateur vers l'accueil de son département.
   * Le guard retourne toujours un UrlTree (redirection) : le composant '' n'est jamais rendu.
   * Pour ajouter un accueil Marchés : ajouter l'entrée dans accueilDispatchGuard + ici.
   */
  {
    path: 'accueil',
    children: [
      {
        path: '',
        canActivate: [accueilDispatchGuard],
        loadComponent: () => import('../../accueil/views/accueil-bienvenue/accueil-bienvenue.component')
          .then(m => m.AccueilBienvenueComponent)
      },
      {
        path: 'finance',
        canActivate: [departmentGuard('Finance')],
        loadComponent: () => import('../../accueil/views/acceuil/acceuil.component')
          .then(m => m.AcceuilComponent)
      },
      {
        path: 'bienvenue',
        loadComponent: () => import('../../accueil/views/accueil-bienvenue/accueil-bienvenue.component')
          .then(m => m.AccueilBienvenueComponent)
      }
    ]
  },

  {
    path: 'rapprochement',
    loadChildren: () => import('../../rapprochement/rapprochement.routes').then(m => m.RAPPROCHEMENT_ROUTES)
  },

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


  {
    path: 'lignes-fonctionnelles-encours',
    loadChildren: () => import('../../lignes-fonctionnelles/lignes-fonctionnelles.routes').then(m => m.LIGNES_FONCTIONNELLES_ENCOURS_ROUTES)
  },
  {
    path: 'lignes-fonctionnelles-archived',
    loadChildren: () => import('../../lignes-fonctionnelles/lignes-fonctionnelles.routes').then(m => m.LIGNES_FONCTIONNELLES_ARCHIVED_ROUTES)
  },

  {
    path: 'lignes-fonctionnelles-specifiques-encours',
    loadChildren: () => import('../../specific-Functional-Lignes/lignes-specifiques.routes').then(m => m.LIGNES_SPECIFIQUES_ENCOURS_ROUTES)
  },
  {
    path: 'lignes-fonctionnelles-specifiques-archived',
    loadChildren: () => import('../../specific-Functional-Lignes/lignes-specifiques.routes').then(m => m.LIGNES_SPECIFIQUES_ARCHIVED_ROUTES)
  },
  { path: 'lignes-fonctionnelles-specifiques/marche/:id', canActivate: [departmentGuard('Finance')], loadComponent: () => import('../../Marches/views/specific-lines-by-marche/functionalLines-by-marche.component').then(m => m.FunctionalLinesByMarcheComponent) },
  { path: "factoring-encours/:lineId", canActivate: [departmentGuard('Finance')], loadComponent: () => import('../../factoring/LignesFactoring/SuivieDesLignes/views/validated-factoring.component').then(m => m.ValidatedFactoringComponent) },
  { path: "prefi-encours/:lineId", canActivate: [departmentGuard('Finance')], loadComponent: () => import('../../prefinancements/lignesPrefi/suivieDesLignes/views/validated-prefi/validated-prefi.component').then(m => m.ValidatedPrefiComponent) },
  { path: "dc-encours/:lineId", canActivate: [departmentGuard('Finance')], loadComponent: () => import('../../droitsConstate/LignesDroitsConstate/SuivieDesLignes/views/validated-dc/validated-dc.component').then(m => m.ValidatedDcComponent) },
  { path: "lcn-encours/:lineId", canActivate: [departmentGuard('Finance')], loadComponent: () => import('../../LCN/views/lcn-escompte-by-line/lcn-escompte-by-line.component').then(m => m.LcnEscompteByLineComponent) },
  { path: 'leasing',
    loadChildren: () => import('../../leasing/route/leasing-routing.module').then(m => m.LEASING_FEATURE_ROUTES) },

  { path: "cmt",
    loadChildren: () => import('../../cmt/route/cmt-routing').then(m => m.cmtRoutes) },

  {
    path: 'LCN',
    loadChildren: () => import('../../LCN/lcn.routes').then(m => m.LCN_ROUTES)
  },

  {
    path: 'Cheques',
    loadChildren: () => import('../../cheques/cheques.routes').then(m => m.CHEQUES_ROUTES)
  },

  {
    path: 'Virements',
    loadChildren: () => import('../../Virements/virements.routes').then(m => m.VIREMENTS_ROUTES)
  },

  {
    path: 'virements-internationaux',
    loadChildren: () => import('../../virementInternational/virement-international.routes').then(m => m.VIREMENT_INTERNATIONAL_ROUTES)
  },

  {
    path: 'operations/errones',
    canActivate: [departmentGuard('Finance')],
    loadChildren: () => import('../../operationErronées/operation-errone.routes').then(m => m.OPERATION_ERRONE_ROUTES)
  },

  {
    path: 'Mise-a-disposition',
    loadChildren: () => import('../../Mise-à-disposition/mise-a-disposition.routes').then(m => m.MISE_A_DISPOSITION_ROUTES)
  },

  {
    path: 'marches',
    canActivate: [departmentGuard('Finance')],
    loadChildren: () => import('../../Marches/marches.routes').then(m => m.MARCHES_ROUTES)
  },

  {
    path: 'préfinancements',
    canActivate: [departmentGuard('Finance')],
    loadChildren: () => import('../../prefinancements/prefinancements.routes').then(m => m.PREFINANCEMENTS_ROUTES)
  },

  {
    path: 'droits-constate',
    canActivate: [departmentGuard('Finance')],
    loadChildren: () => import('../../droitsConstate/droits-constate.routes').then(m => m.DROITS_CONSTATE_ROUTES)
  },

  {
    path: 'factoring',
    canActivate: [departmentGuard('Finance')],
    loadChildren: () => import('../../factoring/factoring.routes').then(m => m.FACTORING_ROUTES)
  },

  {
    path: 'decomptes',
    canActivate: [departmentGuard('Finance')],
    loadChildren: () => import('../../decomptes/decomptes.routes').then(m => m.DECOMPTES_ROUTES)
  },

  {
    path: 'etats-des-encours/:banqueId',
    loadChildren: () => import('../../etatsDesEncours/etats-des-encours.routes').then(m => m.ETATS_DES_ENCOURS_ROUTES)
  },

  {
    path: 'commissions',
    loadChildren: () => import('../../commission/commission.routes').then(m => m.COMMISSION_ROUTES)
  },

  {
    path: 'impots-taxes',
    loadChildren: () => import('../../impotEtTaxes/impot-et-taxe.routes').then(m => m.IMPOT_ET_TAXE_ROUTES)
  },

  {
    path: 'restitutions',
    canActivate: [departmentGuard('Finance')],
    loadChildren: () => import('../../restitution/restitution.routes').then(m => m.RESTITUTION_ROUTES)
  },

  {
    path: 'agios',
    canActivate: [departmentGuard('Finance')],
    loadChildren: () => import('../../agios/agios.routes').then(m => m.AGIOS_ROUTES)
  },

  {
    path: 'impayees',
    canActivate: [departmentGuard('Finance')],
    loadChildren: () => import('../../impayees/impayees.routes').then(m => m.IMPAYEES_ROUTES)
  },
  {
    path: 'prelevements',
    loadChildren: () => import('../../prelevements/prelevements.routes').then(m => m.PRELEVEMENTS_ROUTES)
  },

  {
    path: 'frais-bancaire',
    canActivate: [departmentGuard('Finance')],
    loadChildren: () => import('../../frais-bancaire/frais-bancaire.routes').then(m => m.FRAIS_BANCAIRE_ROUTES)
  },


  { path: "historique-des-transactions", loadComponent: () => import('../../transactions/views/transaction-history/transaction-history.component').then(m => m.TransactionHistoryComponent), canActivate: [departmentGuard('Finance')] },
  { path: 'logs', loadComponent: () => import('../../settings/logs/views/logs/logs.component').then(m => m.LogsComponent), canActivate: [systemAdminGuard] },

  // ── Module Caution ───────────────────────────────────────────────────────
  {
    path: 'cautions',
    loadChildren: () =>
      import('../../cautions/route/caution-routing').then(m => m.CAUTION_ROUTES),
  },

  // ── Accès refusé ─────────────────────────────────────────────────────────
  { path: 'unauthorized', loadComponent: () => import('../views/unauthorized/unauthorized.component').then(m => m.UnauthorizedComponent) },

  // ── Module Administration (départements, utilisateurs) — SYSTEM_ADMIN ────
  {
    path: 'admin',
    canActivate: [systemAdminGuard],
    loadChildren: () => import('../../admin/admin.routes').then(m => m.ADMIN_ROUTES)
  },
];
