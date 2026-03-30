import { Routes } from '@angular/router';
import {NatureLigneFonctionnelleListComponent} from "./views/nature-ligne-fonctionnelle-list/nature-ligne-fonctionnelle-list.component";
import {NatureLigneFonctionnelleEditComponent} from "./views/nature-ligne-fonctionnelle-edit/nature-ligne-fonctionnelle-edit.component";


export const NATURES_ROUTES: Routes = [
  {
    path: '',
    children: [
      { path: '', component: NatureLigneFonctionnelleListComponent },
      { path: 'new', component: NatureLigneFonctionnelleEditComponent },
      { path: 'edit/:id', component: NatureLigneFonctionnelleEditComponent },
    ]
  }
];
