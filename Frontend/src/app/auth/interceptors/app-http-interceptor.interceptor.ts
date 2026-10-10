import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { Observable, throwError } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { MessageService } from 'primeng/api';

/**
 * Forme du corps d'erreur renvoyé par le backend (voir `ErrorResponse` côté Spring) —
 * { status, error, message, traces, timestamp }.
 */
interface BackendErrorBody {
  status?: number;
  error?: string;
  message?: string;
}

const GENERIC_ERROR_MESSAGE = 'Une erreur est survenue. Veuillez réessayer.';
const SESSION_URLS = ['/auth/login', '/auth/refresh', '/auth/logout'];
/** Échecs gérés par AuthService lui-même : jamais de toast. */
const SILENT_URLS = ['/auth/refresh', '/auth/logout'];

/**
 * Interceptor unique pour : (1) renouveler la session en silence quand le jeton d'accès a expiré,
 * (2) gérer TOUTES les erreurs HTTP de façon centralisée — aucun service/composant ne doit
 * appeler `messageService.add({severity:'error', ...})` lui-même (voir CLAUDE.md frontend).
 *
 * Aucun jeton n'est manipulé ici : ils voyagent en cookies HttpOnly, envoyés seuls par le
 * navigateur (même origine). L'en-tête CSRF X-XSRF-TOKEN est ajouté par Angular (support natif).
 */
export const appHttpInterceptorInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn): Observable<HttpEvent<unknown>> => {
  const authService = inject(AuthService);
  const messageService = inject(MessageService);

  const isSessionRequest = SESSION_URLS.some(url => req.url.includes(url));
  const isSilent = SILENT_URLS.some(url => req.url.includes(url));

  const showError = (error: HttpErrorResponse): void => {
    // 403 (accès refusé), 400/404/409/500, échec /auth/login, erreur réseau...
    // — toast systématique avec le message réel du backend, sans déconnexion.
    const body = error.error as BackendErrorBody | null;
    messageService.add({
      severity: 'error',
      summary: body?.error || 'Erreur',
      detail: body?.message || GENERIC_ERROR_MESSAGE
    });
  };

  const expireSession = (): void => {
    // Toast seulement si une session était ouverte (pas au démarrage d'un utilisateur non connecté).
    if (authService.isAuthenticated()) {
      messageService.add({
        severity: 'error',
        summary: 'Session expirée',
        detail: 'Votre session a expiré, veuillez vous reconnecter.'
      });
      authService.endSession();
    }
  };

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401 || isSessionRequest) {
        if (!isSilent) {
          showError(error);
        }
        return throwError(() => error);
      }

      // 401 : jeton d'accès expiré => un renouvellement, puis la requête est rejouée une seule fois.
      return authService.refreshSession().pipe(
        switchMap(refreshed => {
          if (!refreshed) {
            expireSession();
            return throwError(() => error);
          }
          return next(req).pipe(
            catchError((retryError: HttpErrorResponse) => {
              if (retryError.status === 401) {
                expireSession();
              } else {
                showError(retryError);
              }
              return throwError(() => retryError);
            })
          );
        })
      );
    })
  );
};
