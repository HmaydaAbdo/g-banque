import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
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

/**
 * Interceptor unique pour : (1) attacher le Bearer token, (2) gérer TOUTES les erreurs
 * HTTP de façon centralisée — aucun service/composant ne doit plus appeler
 * `messageService.add({severity:'error', ...})` lui-même (voir CLAUDE.md frontend).
 */
export const appHttpInterceptorInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn): Observable<HttpEvent<unknown>> => {
  const authService = inject(AuthService);
  const messageService = inject(MessageService);

  const isLoginRequest = req.url.includes('/auth/login');

  let requestToSend = req;

  if (!isLoginRequest) {
    const authToken = authService.getToken();
    if (authToken && authService.isTokenValid()) {
      requestToSend = req.clone({
        headers: req.headers.set('Authorization', `Bearer ${authToken}`)
      });
    } else {
      messageService.add({
        severity: 'error',
        summary: 'Session expirée',
        detail: 'Votre session a expiré, veuillez vous reconnecter.'
      });
      authService.logout().subscribe();
      return throwError(() => new Error('Authentication required or token invalid. User logged out.'));
    }
  }

  return next(requestToSend).pipe(
    catchError((error: HttpErrorResponse) => {
      const body = error.error as BackendErrorBody | null;
      const detail = body?.message || GENERIC_ERROR_MESSAGE;

      if (error.status === 401 && !isLoginRequest) {
        // Token rejeté côté serveur (expiré/invalide) — déconnexion, pas seulement un toast.
        messageService.add({
          severity: 'error',
          summary: 'Session expirée',
          detail: 'Votre session a expiré, veuillez vous reconnecter.'
        });
        authService.logout().subscribe();
      } else {
        // 403 (accès refusé), 400/404/409/500, échec /auth/login, erreur réseau...
        // — toast systématique avec le message réel du backend, sans déconnexion.
        messageService.add({
          severity: 'error',
          summary: body?.error || 'Erreur',
          detail
        });
      }

      return throwError(() => error);
    })
  );
};
