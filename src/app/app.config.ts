import {ApplicationConfig, provideZoneChangeDetection} from '@angular/core';
import { provideRouter, withRouterConfig } from '@angular/router';

import { routes } from './app.routes';
import {provideAnimations} from "@angular/platform-browser/animations";
import {provideHttpClient, withInterceptors} from "@angular/common/http";
import { appHttpInterceptorInterceptor} from "./auth/interceptors/app-http-interceptor.interceptor";
import {DatePipe} from "@angular/common";
import {ConfirmationService, MessageService} from "primeng/api";

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideHttpClient( withInterceptors([appHttpInterceptorInterceptor])),
    provideRouter(routes, withRouterConfig({ paramsInheritanceStrategy: 'always' })),
    provideAnimations(),
    MessageService,
    DatePipe
  ]
};
