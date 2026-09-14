import { ApplicationConfig, provideZoneChangeDetection, LOCALE_ID } from '@angular/core';
import { provideRouter, withRouterConfig } from '@angular/router';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';

import { routes } from './app.routes';
import { provideAnimations } from "@angular/platform-browser/animations";
import { provideHttpClient, withInterceptors } from "@angular/common/http";
import { appHttpInterceptorInterceptor } from "./auth/interceptors/app-http-interceptor.interceptor";
import { DatePipe } from "@angular/common";
import { MessageService } from "primeng/api";

// Enregistre la locale française pour Angular (DatePipe, DecimalPipe, etc.)
registerLocaleData(localeFr);

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideHttpClient(withInterceptors([appHttpInterceptorInterceptor])),
    provideRouter(routes, withRouterConfig({ paramsInheritanceStrategy: 'always' })),
    provideAnimations(),
    MessageService,
    DatePipe,
    { provide: LOCALE_ID, useValue: 'fr' }
  ]
};
