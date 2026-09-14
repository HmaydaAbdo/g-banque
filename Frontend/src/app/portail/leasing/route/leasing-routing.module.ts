import { Routes } from '@angular/router';
import {departmentGuard} from "../../../auth/guards/department.guard";

export const LEASING_FEATURE_ROUTES: Routes = [
  {
    path: 'organismesLeasing',
    canActivate: [departmentGuard('Finance')],
    children: [
      {
        path: '',
        loadComponent: () =>
          import('../organismesLeasing/views/organismesLeasingList/organisme-leasing-list.component')
            .then(m => m.OrganismeLeasingListComponent)
      },
      {
        path: 'new',
        loadComponent: () =>
          import('../organismesLeasing/views/organisme-leasing-form/organisme-leasing-form.component')
            .then(m => m.OrganismeLeasingFormComponent)
      },
      {
        path: 'edit/:id',
        loadComponent: () =>
          import('../organismesLeasing/views/organisme-leasing-form/organisme-leasing-form.component')
            .then(m => m.OrganismeLeasingFormComponent)
      }
    ]
  },
  {
    path: 'contratsCadres',
    canActivate: [departmentGuard('Finance')],
    loadComponent: () =>
      import('../contratCadre/views/contrat-cadre-list/contrat-cadre-list.component')
        .then(m => m.ContratCadreListComponent)
  },
  {
    path: 'contratsLeasing',
    canActivate: [departmentGuard('Finance')],
    children: [
      { path: '', redirectTo: 'encours', pathMatch: 'full' },
      {
        path: 'encours',
        loadComponent: () =>
          import('../contratsLeasing/views/contrats-leasing-actifs-list/contrats-leasing-list.component')
            .then(m => m.ContratLeasingListComponent)
      },
      {
        path: 'en-attente-de-ML',
        loadComponent: () =>
          import('../contratsLeasing/views/contrats-leasing-en-attente-de-ml/contrats-leasing-en-attente-de-ml.component')
            .then(m => m.ContratsLeasingEnAttenteDeMLComponent)
      },
      {
        path: 'archives',
        loadComponent: () =>
          import('../contratsLeasing/views/contrats-leasing-archives/contrats-leasing-archives.component')
            .then(m => m.ContratsLeasingArchivesComponent)
      },
      {
        path: 'creation',
        loadComponent: () =>
          import('../contratsLeasing/views/contrat-leasing-creation/contrat-leasing-creation.component')
            .then(m => m.ContratLeasingCreationComponent),
        children: [
          { path: '', redirectTo: 'organisme', pathMatch: 'full' },
          {
            path: 'organisme',
            loadComponent: () =>
              import('../contratsLeasing/views/contrat-leasing-creation/wizard/organisme/organisme-step.component')
                .then(m => m.OrganismeStepComponent)
          },
          {
            path: 'general-details',
            loadComponent: () =>
              import('../contratsLeasing/views/contrat-leasing-creation/wizard/GeneralDetail/GeneralDetailsStepComponent')
                .then(m => m.GeneralDetailsStepComponent)
          },
          {
            path: 'banque',
            loadComponent: () =>
              import('../contratsLeasing/views/contrat-leasing-creation/wizard/BanqueOfDebit/BanqueStepComponent')
                .then(m => m.BanqueStepComponent)
          },
          {
            path: 'type',
            loadComponent: () =>
              import('../contratsLeasing/views/contrat-leasing-creation/wizard/contract-type/contract-type-step.component')
                .then(m => m.ContractTypeStepComponent)
          },
          {
            path: 'phases',
            loadComponent: () =>
              import('../contratsLeasing/views/contrat-leasing-creation/wizard/Phases/PhasesStepComponent')
                .then(m => m.PhasesStepComponent)
          },
          {
            path: 'recap',
            loadComponent: () =>
              import('../contratsLeasing/views/contrat-leasing-creation/wizard/recap/RecapStepComponent')
                .then(m => m.RecapStepComponent)
          }
        ]
      },
      {
        path: ':id/tableau-amortissement',
        loadComponent: () =>
          import('../contratsLeasing/views/tableauAmortissement/TableauAmortissementComponent')
            .then(m => m.TableauAmortissementComponent)
      }
    ]
  },
  {
    path: '',
    redirectTo: 'contratsLeasing',
    pathMatch: 'full'
  }
];
