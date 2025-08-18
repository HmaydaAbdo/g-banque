import { Routes } from '@angular/router';
import {CmtListComponent} from "../views/cmt-list/cmt-list.component";
import {CmtFormComponent} from "../views/cmt-form/cmt-form.component";
import {TableauAmortissementComponent} from "../views/tableau-amortissement-cmt/tableau-amortissement.componen";

export const cmtRoutes: Routes = [
  {
    path: '',
    redirectTo: 'list',
    pathMatch: 'full'
  },
  {
    path: 'list',
    component:CmtListComponent,
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
