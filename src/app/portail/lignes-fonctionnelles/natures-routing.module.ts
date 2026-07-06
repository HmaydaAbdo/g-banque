import { Routes } from '@angular/router';
import { departmentGuard } from '../../auth/guards/department.guard';

export const NATURES_ROUTES: Routes = [
  {
    path: '',
    canActivate: [departmentGuard('Finance')],
    children: [
      {
        path: '',
        loadComponent: () => import('./views/nature-ligne-fonctionnelle-list/nature-ligne-fonctionnelle-list.component').then(m => m.NatureLigneFonctionnelleListComponent)
      },
      {
        path: 'new',
        loadComponent: () => import('./views/nature-ligne-fonctionnelle-edit/nature-ligne-fonctionnelle-edit.component').then(m => m.NatureLigneFonctionnelleEditComponent)
      },
      {
        path: 'edit/:id',
        loadComponent: () => import('./views/nature-ligne-fonctionnelle-edit/nature-ligne-fonctionnelle-edit.component').then(m => m.NatureLigneFonctionnelleEditComponent)
      }
    ]
  }
];
