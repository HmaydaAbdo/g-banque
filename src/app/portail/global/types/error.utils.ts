import { HttpErrorResponse } from '@angular/common/http';
import { ApiErrorResponse } from './ApiErrorResponse';

/**
 * Extrait le message lisible depuis une HttpErrorResponse.
 *
 * Priorité :
 *   1. `err.error.message` — champ standardisé de l'ApiErrorResponse du backend
 *   2. `err.message`       — message HTTP natif (ex. "Unknown Error")
 *   3. `fallback`          — valeur fournie par l'appelant
 *
 * @param err      L'erreur reçue dans le bloc `error:` d'un subscribe
 * @param fallback Message affiché si aucun message n'est trouvé
 */
export function extractErrorMessage(
  err: HttpErrorResponse | any,
  fallback = 'Une erreur inattendue s\'est produite.'
): string {
  if (err instanceof HttpErrorResponse) {
    const body = err.error as ApiErrorResponse | null;
    if (body?.message) return body.message;
    if (err.message)   return err.message;
  }
  return fallback;
}

/**
 * Retourne true si l'erreur est un 4xx (erreur client — règle métier, validation…)
 */
export function isClientError(err: HttpErrorResponse | any): boolean {
  return err instanceof HttpErrorResponse && err.status >= 400 && err.status < 500;
}

/**
 * Retourne true si l'erreur est un 5xx (erreur serveur)
 */
export function isServerError(err: HttpErrorResponse | any): boolean {
  return err instanceof HttpErrorResponse && err.status >= 500;
}
