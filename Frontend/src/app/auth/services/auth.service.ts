// src/app/auth/services/auth.service.ts

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable, BehaviorSubject, of } from 'rxjs';
import { catchError, distinctUntilChanged, finalize, map, shareReplay, tap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { SessionProfile } from "../dto/SessionProfile";
import { LoginCredentials } from "../dto/LoginCredentials";
import { DepartmentRoleType } from "../types/DepartmentRoleType";

const SYSTEM_ADMIN_AUTHORITY = 'SYSTEM_ADMIN';

/**
 * Session utilisateur. Les jetons (accès 15 min, rafraîchissement) sont des cookies HttpOnly posés
 * par le backend : ils ne sont ni lisibles ni stockés ici (aucun localStorage). Le service ne
 * conserve que le profil (username + authorities) renvoyé par le backend.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private authUrl = `${environment.apiUrl}auth`;

  /** Profil de la session courante ; null = aucune session. */
  private profileSubject = new BehaviorSubject<SessionProfile | null>(null);
  /** Émet à l'ouverture et à la fermeture de session (pas à chaque renouvellement silencieux). */
  isAuthenticated$: Observable<boolean> = this.profileSubject.pipe(
    map(profile => profile !== null),
    distinctUntilChanged()
  );
  private refreshInFlight$: Observable<boolean> | null = null;

  /**
   * Au démarrage de l'application : récupère la session portée par les cookies, s'il y en a une.
   * Ne lève jamais d'erreur — sans session valide, l'utilisateur reste simplement déconnecté.
   */
  restoreSession(): Observable<void> {
    return this.http.get<SessionProfile>(`${this.authUrl}/profile`).pipe(
      tap(profile => this.profileSubject.next(profile)),
      map(() => undefined),
      catchError(() => of(undefined))
    );
  }

  login(credentials: LoginCredentials): Observable<SessionProfile> {
    return this.http.post<SessionProfile>(`${this.authUrl}/login`, credentials).pipe(
      tap(profile => this.profileSubject.next(profile))
    );
  }

  /**
   * Renouvelle la session (cookie de rafraîchissement). Un seul appel à la fois : les requêtes
   * qui échouent en 401 au même moment partagent le même renouvellement.
   *
   * @returns true si la session est renouvelée (200, ou 204 = déjà renouvelée par un autre onglet)
   */
  refreshSession(): Observable<boolean> {
    if (!this.refreshInFlight$) {
      this.refreshInFlight$ = this.http
        .post<SessionProfile | null>(`${this.authUrl}/refresh`, null, { observe: 'response' })
        .pipe(
          tap((response: HttpResponse<SessionProfile | null>) => {
            if (response.body) {
              this.profileSubject.next(response.body);
            }
          }),
          map(() => true),
          catchError(() => of(false)),
          finalize(() => this.refreshInFlight$ = null),
          shareReplay(1)
        );
    }
    return this.refreshInFlight$;
  }

  /**
   * Déconnexion : révoque la session côté serveur puis efface la session locale et redirige vers
   * la connexion. Émet toujours true : un échec de l'appel n'empêche pas la déconnexion locale.
   */
  logout(): Observable<boolean> {
    return this.http.post<void>(`${this.authUrl}/logout`, null).pipe(
      catchError(() => of(undefined)),
      map(() => true),
      tap(() => this.endSession())
    );
  }

  /** Efface la session locale et renvoie vers la connexion (session expirée ou déconnexion). */
  endSession(): void {
    this.profileSubject.next(null);
    this.router.navigate(['/login']);
  }

  isAuthenticated(): boolean {
    return this.profileSubject.value !== null;
  }

  getUsername(): string | null {
    return this.profileSubject.value?.username ?? null;
  }

  // ========================================
  // RBAC — modèle authorities (SYSTEM_ADMIN / DEPT_<nom>:<MEMBER|DIRECTOR>)
  // ========================================

  getAuthorities(): string[] {
    return this.profileSubject.value?.authorities ?? [];
  }

  /** Administrateur système — accès transversal, n'appartient à aucun département. */
  isSystemAdmin(): boolean {
    return this.getAuthorities().includes(SYSTEM_ADMIN_AUTHORITY);
  }

  /** Vrai si l'utilisateur appartient au département donné, quel que soit son rôle (MEMBER ou DIRECTOR). */
  belongsToDepartment(department: string): boolean {
    const prefix = `DEPT_${department}:`;
    return this.getAuthorities().some(authority => authority.startsWith(prefix));
  }

  /** Vrai si l'utilisateur a précisément ce rôle dans ce département. */
  hasDepartmentRole(department: string, role: DepartmentRoleType): boolean {
    return this.getAuthorities().includes(`DEPT_${department}:${role}`);
  }

  /**
   * Administrateur système — alias conservé pour compatibilité avec le code existant
   * (~30 sites d'appel hors périmètre RBAC, ex. modules Chèques/Virements/LCN/Leasing/CMT).
   * Nouveau code : utiliser {@link isSystemAdmin} directement.
   */
  isAdmin(): boolean {
    return this.isSystemAdmin();
  }
}
