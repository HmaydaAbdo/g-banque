import { Routes } from '@angular/router';
import { OrganismeLeasingListComponent } from "../organismesLeasing/views/organismesLeasingList/organisme-leasing-list.component";
import {
  OrganismeLeasingFormComponent
} from "../organismesLeasing/views/organisme-leasing-form/organisme-leasing-form.component";
import {
  ContratLeasingListComponent
} from "../contratsLeasing/views/contrats-leasing-list/contrats-leasing-list.component";
import {
  ContratLeasingCreationComponent
} from "../contratsLeasing/views/contrat-leasing-creation/contrat-leasing-creation.component";
import {
  OrganismeStepComponent
} from "../contratsLeasing/views/contrat-leasing-creation/wizard/organisme/organisme-step.component";
import {
  GeneralDetailsStepComponent
} from "../contratsLeasing/views/contrat-leasing-creation/wizard/GeneralDetail/GeneralDetailsStepComponent";
import {
  BanqueStepComponent
} from "../contratsLeasing/views/contrat-leasing-creation/wizard/BanqueOfDebit/BanqueStepComponent";
import {
  ContractTypeStepComponent
} from "../contratsLeasing/views/contrat-leasing-creation/wizard/contract-type/contract-type-step.component";
import {PhasesStepComponent} from "../contratsLeasing/views/contrat-leasing-creation/wizard/Phases/PhasesStepComponent";
import {RecapStepComponent} from "../contratsLeasing/views/contrat-leasing-creation/wizard/recap/RecapStepComponent";
import {
  TableauAmortissementComponent
} from "../contratsLeasing/views/tableauAmortissement/TableauAmortissementComponent";

export const LEASING_FEATURE_ROUTES: Routes = [
  {
    path: 'organismesLeasing', // This will be the base path for all leasing-related routes
    children: [ // Use children to nest the routes under 'leasing'
      {
        path: '',
        component: OrganismeLeasingListComponent
      },
      {
        path: 'new',
        component: OrganismeLeasingFormComponent
      },
      {
        path: 'edit/:id',
        component: OrganismeLeasingFormComponent
      }
    ]
  },
  {
    path: 'contratsLeasing',
    children: [
      {
        path: '', // Path for the list of contracts: /contratsLeasing
        component: ContratLeasingListComponent
      },
      {
        path: 'creation', // Path for the creation wizard: /contratsLeasing/creation
        component: ContratLeasingCreationComponent,
        children: [ // Nested routes for each step of the wizard
          { path: '', redirectTo: 'organisme', pathMatch: 'full' }, // Default step when hitting /contratsLeasing/creation
          { path: 'organisme', component: OrganismeStepComponent },
          { path: 'general-details', component: GeneralDetailsStepComponent },
          { path: 'banque', component: BanqueStepComponent },
          { path: 'type', component: ContractTypeStepComponent },
          { path: 'phases', component: PhasesStepComponent },
          { path: 'recap', component: RecapStepComponent },
        ]
      },
      { path: ':id/tableau-amortissement',component: TableauAmortissementComponent,}
      // You can add other contract-related routes here if needed, e.g., edit/:id
    ]
  },
  {
    path: '',
    redirectTo: 'contratsLeasing',
    pathMatch: 'full'
  }
];
