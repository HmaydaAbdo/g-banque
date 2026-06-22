// src/app/auth/services/auth.service.ts

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap, of } from 'rxjs';
import { delay, finalize } from 'rxjs/operators';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { LoginResponse } from "../dto/LoginResponse";
import { LoginCredentials } from "../dto/LoginCredentials";
import { jwtDecode } from 'jwt-decode';
import { DecodedToken } from "../types/DecodedToken";
import { DepartmentRoleType } from "../types/DepartmentRoleType";
import { Role } from "../enum/Role";

const SYSTEM_ADMIN_AUTHORITY = 'SYSTEM_ADMIN';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private apiUrl = environment.apiUrl;
  private authUrl = `${this.apiUrl}auth`;

  private isAuthenticatedSubject = new BehaviorSubject<boolean>(this.hasToken());
  isAuthenticated$ = this.isAuthenticatedSubject.asObservable();


  private hasToken(): boolean {
    return !!localStorage.getItem("token");
  }

  login(credentials: LoginCredentials): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.authUrl}/login`, credentials)
      .pipe(
        tap((response: LoginResponse) => {
          if (response.accessToken) {
            localStorage.setItem("token", response.accessToken);
            this.isAuthenticatedSubject.next(true);
          } else {
            console.warn("Login successful but 'accessToken' not found in response.");
          }
        })
      );
  }

  getToken(): string | null {
    if (this.hasToken()) {
      return localStorage.getItem("token");
    } else {
      console.log("No token found in localStorage.");
      return null;
    }
  }

  isTokenValid(): boolean {
    const token = this.getToken();
    if (token) {
      try {
        const decodedToken: DecodedToken = jwtDecode(token);
        const currentTime = Date.now() / 1000;
        return decodedToken.exp > currentTime && this.isAuthenticatedSubject.value;
      } catch (error) {
        console.error("Error decoding token or token expired:", error);
        return false;
      }
    }
    return this.isAuthenticatedSubject.value;
  }

  /**
   * Logs out the user by removing the token and navigating to the login page.
   * Returns an Observable<boolean> to indicate success/failure of the logout process.
   */
  logout(): Observable<boolean> {
    return of(true).pipe(
      delay(500),
      tap(() => {
        localStorage.removeItem("token");
        this.isAuthenticatedSubject.next(false);
        this.router.navigate(['/login']);
      }),
      finalize(() => console.log('Logout process finished'))
    );
  }

  private decodeToken(): DecodedToken | null {
    const token = this.getToken();
    if (token) {
      try {
        return jwtDecode<DecodedToken>(token);
      } catch (error) {
        console.error("Error decoding token:", error);
        return null;
      }
    }
    return null;
  }

  getUsername(): string | null {
    const decodedToken = this.decodeToken();
    return decodedToken ? decodedToken.sub : null;
  }

  // ========================================
  // RBAC — modèle authorities (SYSTEM_ADMIN / DEPT_<nom>:<MEMBER|DIRECTOR>)
  // ========================================

  getAuthorities(): string[] {
    const decodedToken = this.decodeToken();
    return decodedToken?.authorities ?? [];
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

  /**
   * @deprecated Conservé uniquement pour compatibilité avec le code existant qui importe
   * encore l'enum {@link Role} (ADMIN/SUPER_ADMIN → isSystemAdmin(), USER → authentifié).
   * Nouveau code : utiliser {@link isSystemAdmin}, {@link belongsToDepartment} ou
   * {@link hasDepartmentRole} directement — ne plus utiliser {@link Role}.
   */
  hasRole(role: Role): boolean {
    if (role === Role.ADMIN || role === Role.SUPER_ADMIN) {
      return this.isSystemAdmin();
    }
    return this.isAuthenticatedSubject.value;
  }
}
