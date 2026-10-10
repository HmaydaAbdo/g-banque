import { Routes } from '@angular/router';
import { authGuard } from "./auth/guards/auth.guard";


export const routes: Routes = [
  { path: '', loadComponent: () => import('./landing/landing.component').then(m => m.LandingComponent), pathMatch: 'full' },
  { path: 'connexion', loadComponent: () => import('./auth/views/login/login.component').then(m => m.LoginComponent) },
  {
    path: 'portail',
    loadComponent: () => import('./portail/global/views/portail/portail.component').then(m => m.PortailComponent),
    canActivate: [authGuard],
    children: [

      {
        path: '',
        loadChildren: () => import('./portail/global/routes/portail.routes').then(m => m.PORTAIL_ROUTES)
      }
    ]
  },
  { path: '**', redirectTo: '/connexion', pathMatch: 'full' }
];
