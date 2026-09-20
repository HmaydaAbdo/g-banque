import { Routes } from '@angular/router';
import { departmentGuard } from '../../../auth/guards/department.guard';
import { systemAdminGuard } from '../../../auth/guards/system-admin.guard';

export const cmtRoutes: Routes = [
  {
    path: '',
    canActivate: [departmentGuard('Finance')],
    children: [
      {
        path: '',
        redirectTo: 'encours',
        pathMatch: 'full'
      },
      {
        // Édition d'une échéance seule (statut PREVU uniquement, réservé aux administrateurs
        // système — remplace le dialogue historique de TableauAmortissementComponent, 21/09/2026).
        path: 'echeances/edit/:id',
        canActivate: [systemAdminGuard],
        loadComponent: () =>
          import('../views/echeance-cmt-edit/echeance-cmt-edit.component').then(m => m.EcheanceCmtEditComponent)
      },
      {
        path: 'encours',
        loadComponent: () =>
          import('../views/cmt-list-actifs/cmt-list.component').then(m => m.CmtListComponent)
      },
      {
        path: 'en-attente-de-ML',
        loadComponent: () =>
          import('../views/cmt-list-en-attente-de-ml/cmt-list-en-attente-de-ml.component').then(m => m.CmtListEnAttenteDeMLComponent)
      },
      {
        path: 'archives',
        loadComponent: () =>
          import('../views/cmt-list-archived/cmt-list-archived.component').then(m => m.CmtListArchivedComponent)
      },
      {
        path: 'new',
        loadComponent: () =>
          import('../views/cmt-form/cmt-form.component').then(m => m.CmtFormComponent)
      },
      {
        path: 'edit/:id',
        loadComponent: () =>
          import('../views/cmt-form/cmt-form.component').then(m => m.CmtFormComponent)
      },
      {
        path: ':id/tableau-amortissement',
        loadComponent: () =>
          import('../views/tableau-amortissement-cmt/tableau-amortissement.componen').then(m => m.TableauAmortissementComponent)
      }
    ]
  }
];
