import { Provider, EnvironmentProviders } from '@angular/core';
import { DatePipe } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ConfirmationService, MessageService } from 'primeng/api';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

/**
 * Dépendances communes des tests unitaires (cf. conventions/testing.md) :
 * mêmes services globaux que app.config.ts (DatePipe, MessageService), HTTP intercepté (aucun appel réseau), routeur vide, animations neutralisées,
 * services PrimeNG et dialogue dynamique factices.
 */
export const testProviders: (Provider | EnvironmentProviders)[] = [
  provideHttpClient(),
  provideHttpClientTesting(),
  provideRouter([]),
  provideNoopAnimations(),
  DatePipe,
  MessageService,
  ConfirmationService,
  { provide: DynamicDialogConfig, useValue: { data: {} } },
  { provide: DynamicDialogRef, useValue: { close: () => undefined } },
];
