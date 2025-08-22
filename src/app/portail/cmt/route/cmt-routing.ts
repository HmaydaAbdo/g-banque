import { Routes } from '@angular/router';
import {CmtListComponent} from "../views/cmt-list-actifs/cmt-list.component";
import {CmtFormComponent} from "../views/cmt-form/cmt-form.component";
import {TableauAmortissementComponent} from "../views/tableau-amortissement-cmt/tableau-amortissement.componen";
import {CmtListEnAttenteDeMLComponent} from "../views/cmt-list-en-attente-de-ml/cmt-list-en-attente-de-ml.component";
import {CmtListArchivedComponent} from "../views/cmt-list-archived/cmt-list-archived.component";

export const cmtRoutes: Routes = [
  {
    path: '',
    redirectTo: 'encours',
    pathMatch: 'full'
  },
  {
    path: 'encours',
    component:CmtListComponent,
  },
  {
    path: 'en-attente-de-ML',
    component:CmtListEnAttenteDeMLComponent,
  },
  {
    path: 'archives',
    component:CmtListArchivedComponent,
  },
  {
    path: 'new',
    component: CmtFormComponent,
  },
  {
    path: 'edit/:id',
    component: CmtFormComponent,
  },
  {
    path: ':id/tableau-amortissement',
    component: TableauAmortissementComponent
  }
];
