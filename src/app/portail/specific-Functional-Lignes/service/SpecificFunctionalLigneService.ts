import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from "../../../../environments/environment";
import { LigneFonctionnelleSpecifiqueRequest } from "../dtos/LigneFonctionnelleSpecifiqueRequest";
import { LigneFonctionnelleSpecifiqueResponse } from "../dtos/LigneFonctionnelleSpecifiqueResponse";
import { LigneFonctionnelleSpecifiqueSearchCriteria } from "../dtos/LigneFonctionnelleSpecifiqueSearchCriteria";
import { PageResponse } from "../../global/types/PageResponse";
import { AgregatedSuiviePrefiLigneResponseDto } from "../../prefinancements/lignesPrefi/suivieDesLignes/dtos/AgregatedSuiviePrefiLigneResponseDto";
import { AgregatedSuivieDcLigneResponseDto } from "../../droitsConstate/LignesDroitsConstate/SuivieDesLignes/Dtos/AgregatedSuivieDcLigneResponseDto";
import { AgregatedSuivieFactoringLigneResponseDto } from "../../factoring/LignesFactoring/SuivieDesLignes/dtos/AgregatedSuivieFactoringLigneResponseDto";

@Injectable({
  providedIn: 'root',
})
export class SpecificFunctionalLigneService {
  private apiUrl = `${environment.apiUrl}lignes-fonctionnelles-specifiques`;

  constructor(private http: HttpClient) {}

  /**
   * Helper to build pagination and search params
   */
  private buildParams(criteria: any, page: number, size: number, sort: string): HttpParams {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (criteria) {
      Object.keys(criteria).forEach(key => {
        const value = criteria[key];
        if (value !== undefined && value !== null) {
          params = params.append(key, value.toString());
        }
      });
    }
    return params;
  }

  // --- CRUD Operations ---

  createLigneFonctionnelleSpecifique(request: LigneFonctionnelleSpecifiqueRequest): Observable<LigneFonctionnelleSpecifiqueResponse> {
    return this.http.post<LigneFonctionnelleSpecifiqueResponse>(this.apiUrl, request);
  }

  getLigneFonctionnelleSpecifiqueById(id: number): Observable<LigneFonctionnelleSpecifiqueResponse> {
    return this.http.get<LigneFonctionnelleSpecifiqueResponse>(`${this.apiUrl}/${id}`);
  }

  getLignesFonctionnellesSpecifiques(
    criteria: LigneFonctionnelleSpecifiqueSearchCriteria,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ): Observable<PageResponse<LigneFonctionnelleSpecifiqueResponse>> {
    const params = this.buildParams(criteria, page, size, sort);
    return this.http.get<PageResponse<LigneFonctionnelleSpecifiqueResponse>>(this.apiUrl, { params });
  }

  updateLigneFonctionnelleSpecifique(id: number, request: LigneFonctionnelleSpecifiqueRequest): Observable<LigneFonctionnelleSpecifiqueResponse> {
    return this.http.put<LigneFonctionnelleSpecifiqueResponse>(`${this.apiUrl}/${id}`, request);
  }

  deactivateLigneFonctionnelle(ligneId: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/deactivate/${ligneId}`, {});
  }

  deleteLigneFonctionnelleSpecifique(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // --- Specialized Queries ---

  getLignesFonctionnellesByBanqueId(
    banqueId: number,
    criteria: LigneFonctionnelleSpecifiqueSearchCriteria,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ): Observable<PageResponse<LigneFonctionnelleSpecifiqueResponse>> {
    const params = this.buildParams(criteria, page, size, sort);
    return this.http.get<PageResponse<LigneFonctionnelleSpecifiqueResponse>>(
      `${this.apiUrl}/banque/${banqueId}`,
      { params }
    );
  }

  getLinesByMarche(
    marcheId: number,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ): Observable<PageResponse<LigneFonctionnelleSpecifiqueResponse>> {
    const params = this.buildParams(null, page, size, sort);
    return this.http.get<PageResponse<LigneFonctionnelleSpecifiqueResponse>>(
      `${this.apiUrl}/marche/${marcheId}`,
      { params }
    );
  }

  // --- Aggregated Tracking (Suivi) ---

  getPrefinancementsByLineId(id: number, page: number = 0, size: number = 10, sort: string = 'id,asc'): Observable<AgregatedSuiviePrefiLigneResponseDto> {
    const params = this.buildParams(null, page, size, sort);
    return this.http.get<AgregatedSuiviePrefiLigneResponseDto>(`${this.apiUrl}/${id}/prefinancements`, { params });
  }

  getDroitConstateByLineId(id: number, page: number = 0, size: number = 10, sort: string = 'id,asc'): Observable<AgregatedSuivieDcLigneResponseDto> {
    const params = this.buildParams(null, page, size, sort);
    return this.http.get<AgregatedSuivieDcLigneResponseDto>(`${this.apiUrl}/${id}/dc`, { params });
  }

  getFactoringByLineId(id: number, page: number = 0, size: number = 10, sort: string = 'id,asc'): Observable<AgregatedSuivieFactoringLigneResponseDto> {
    const params = this.buildParams(null, page, size, sort);
    return this.http.get<AgregatedSuivieFactoringLigneResponseDto>(`${this.apiUrl}/${id}/factoring`, { params });
  }

  // --- Export PDF ---

  /**
   * Exporte en PDF les lignes fonctionnelles spécifiques correspondant aux filtres fournis (mêmes
   * critères que {@link getLignesFonctionnellesSpecifiques}) — sans pagination. Réutilisé par les
   * écrans encours (criteria.actif = true) et archivées (criteria.actif = false).
   * GET /lignes-fonctionnelles-specifiques/export/pdf
   */
  exportPdf(criteria: LigneFonctionnelleSpecifiqueSearchCriteria): Observable<Blob> {
    let params = new HttpParams();
    if (criteria) {
      Object.keys(criteria).forEach(key => {
        const value = (criteria as any)[key];
        if (value !== undefined && value !== null) {
          params = params.append(key, value.toString());
        }
      });
    }
    return this.http.get(`${this.apiUrl}/export/pdf`, { params, responseType: 'blob' });
  }
}
