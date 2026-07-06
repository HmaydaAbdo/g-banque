import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LigneFonctionnelleConfigRequest } from '../dtos/LigneFonctionnelleConfigRequest';
import { LigneFonctionnelleConfigResponse } from '../dtos/LigneFonctionnelleConfigResponse';
import { environment } from '../../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class LigneFonctionnelleConfigService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}lignes-fonctionnelles-config`;

  getConfig(): Observable<LigneFonctionnelleConfigResponse> {
    return this.http.get<LigneFonctionnelleConfigResponse>(this.apiUrl);
  }

  update(dto: LigneFonctionnelleConfigRequest): Observable<LigneFonctionnelleConfigResponse> {
    return this.http.put<LigneFonctionnelleConfigResponse>(this.apiUrl, dto);
  }
}
