import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

export const appHttpInterceptorInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
): Observable<HttpEvent<unknown>> => {
  const authService = inject(AuthService);

  // Skip interceptor for login endpoint
  if (req.url.includes("/auth/login")) {
    return next(req);
  }

  // Get the authentication token
  const authToken = authService.getToken();

  // If token exists and is valid, add it to the request
  if (authToken && authService.isTokenValid()) {
    const clonedRequest = req.clone({
      headers: req.headers.set('Authorization', `Bearer ${authToken}`)
    });

    return next(clonedRequest).pipe(
      catchError((error: HttpErrorResponse) => {
        // Only logout on 401/403 from the server
        if (error.status === 401 || error.status === 403) {
          console.warn('Server rejected authentication. Logging out...');
          authService.logout();
        }
        return throwError(() => error);
      })
    );
  }

  // CRITICAL: If no token or invalid token, proceed WITHOUT forcing logout
  // Let the backend decide if authentication is required
  console.log('No valid token found. Proceeding without authentication header.');

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // If backend requires auth, it will return 401/403
      if (error.status === 401 || error.status === 403) {
        console.warn('Authentication required by server. Logging out...');
        authService.logout();
      }
      return throwError(() => error);
    })
  );
};
