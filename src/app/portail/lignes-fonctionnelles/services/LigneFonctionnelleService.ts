// ligne-fonctionnelle.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { LigneFonctionnelleResponse } from "../dtos/LigneFonctionnelleResponse";
import { LigneFonctionnelleRequest } from '../dtos/LigneFonctionnelleRequest';
import { LigneFonctionnelleSearchCriteria } from "../dtos/LigneFonctionnelleSearchCriteria";
import { PageResponse } from "../../global/types/PageResponse";
import { LigneFonctionnelleChartData } from "../dtos/LigneFonctionnelleChartData";
import { LigneFonctionnelleChartHistoryRequest } from "../dtos/LigneFonctionnelleChartHistoryRequest";
import { SousLigneRequestDto } from "../dtos/SousLigneRequestDto";
import { AgregatedSuiviePrefiLigneResponseDto } from "../../prefinancements/lignesPrefi/suivieDesLignes/dtos/AgregatedSuiviePrefiLigneResponseDto";
import { AgregatedSuivieDcLigneResponseDto } from "../../droitsConstate/LignesDroitsConstate/SuivieDesLignes/Dtos/AgregatedSuivieDcLigneResponseDto";
import { AgregatedSuivieFactoringLigneResponseDto } from "../../factoring/LignesFactoring/SuivieDesLignes/dtos/AgregatedSuivieFactoringLigneResponseDto";
import {AgregatedSuivieLcnLigneResponseDto} from "../../LCN/dtos/AgregatedSuivieLcnLigneResponseDto";

@Injectable({
  providedIn: 'root',
})
export class LigneFonctionnelleService {
  private apiUrl = `${environment.apiUrl}lignes-fonctionnelles`;

  constructor(private http: HttpClient) {}

  // ==========================================================================
  // CRUD OPERATIONS
  // ==========================================================================

  /**
   * Create LigneFonctionnelle
   * POST /lignes-fonctionnelles
   */
  createLigneFonctionnelle(request: LigneFonctionnelleRequest): Observable<LigneFonctionnelleResponse> {
    return this.http.post<LigneFonctionnelleResponse>(this.apiUrl, request);
  }

  /**
   * Get LigneFonctionnelle by ID
   * GET /lignes-fonctionnelles/{id}
   */
  getLigneFonctionnelleById(id: number): Observable<LigneFonctionnelleResponse> {
    return this.http.get<LigneFonctionnelleResponse>(`${this.apiUrl}/${id}`);
  }

  /**
   * Get Active LigneFonctionnelles with search criteria
   * GET /lignes-fonctionnelles
   */
  getCurrentLigneFonctionnelles(
    criteria: LigneFonctionnelleSearchCriteria,
    page: number,
    size: number,
    sort: string
  ): Observable<PageResponse<LigneFonctionnelleResponse>> {
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    params = params.append('sort', sort);

    // Banque filter
    if (criteria.banqueId !== undefined && criteria.banqueId !== null) {
      params = params.append('banqueId', criteria.banqueId.toString());
    }

    // Nature filter
    if (criteria.natureLigneFonctionnelleId !== undefined && criteria.natureLigneFonctionnelleId !== null) {
      params = params.append('natureLigneFonctionnelleId', criteria.natureLigneFonctionnelleId.toString());
    }

    // Marche filter
    if (criteria.marcheId !== undefined && criteria.marcheId !== null) {
      params = params.append('marcheId', criteria.marcheId.toString());
    }

    // Year filter
    if (criteria.yearDepart !== undefined && criteria.yearDepart !== null) {
      params = params.append('yearDepart', criteria.yearDepart.toString());
    }

    // Active status filter
    if (criteria.actif !== undefined && criteria.actif !== null) {
      params = params.append('actif', criteria.actif.toString());
    }

    // Expired status filter
    if (criteria.isExpired !== undefined && criteria.isExpired !== null) {
      params = params.append('isExpired', criteria.isExpired.toString());
    }

    return this.http.get<PageResponse<LigneFonctionnelleResponse>>(this.apiUrl, { params });
  }

  /**
   * Get LignesFonctionnelles by Banque ID
   * GET /lignes-fonctionnelles/banque/{banqueId}
   */
  getLignesFonctionnellesByBanqueId(
    banqueId: number,
    criteria: LigneFonctionnelleSearchCriteria,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ): Observable<PageResponse<LigneFonctionnelleResponse>> {
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    params = params.append('sort', sort);

    // Nature filter
    if (criteria.natureLigneFonctionnelleId !== undefined && criteria.natureLigneFonctionnelleId !== null) {
      params = params.append('natureLigneFonctionnelleId', criteria.natureLigneFonctionnelleId.toString());
    }

    // Marche filter
    if (criteria.marcheId !== undefined && criteria.marcheId !== null) {
      params = params.append('marcheId', criteria.marcheId.toString());
    }

    // Year filter
    if (criteria.yearDepart !== undefined && criteria.yearDepart !== null) {
      params = params.append('yearDepart', criteria.yearDepart.toString());
    }

    // Active status filter
    if (criteria.actif !== undefined && criteria.actif !== null) {
      params = params.append('actif', criteria.actif.toString());
    }

    return this.http.get<PageResponse<LigneFonctionnelleResponse>>(`${this.apiUrl}/banque/${banqueId}`, { params });
  }

  /**
   * Update LigneFonctionnelle
   * PUT /lignes-fonctionnelles/{id}
   */
  updateLigneFonctionnelle(id: number, request: LigneFonctionnelleRequest): Observable<LigneFonctionnelleResponse> {
    return this.http.put<LigneFonctionnelleResponse>(`${this.apiUrl}/${id}`, request);
  }

  /**
   * Delete LigneFonctionnelle
   * DELETE /lignes-fonctionnelles/{id}
   */
  deleteLigneFonctionnelle(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Deactivate a functional ligne
   * PUT /lignes-fonctionnelles/deactivate/{ligneId}
   */
  deactivateLigneFonctionnelle(ligneId: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/deactivate/${ligneId}`, {});
  }

  // ==========================================================================
  // SOUS-LIGNES MANAGEMENT
  // ==========================================================================

  /**
   * Manage Sous-Lignes (create, update, delete)
   * PUT /lignes-fonctionnelles/{ligneMereId}/sous-lignes
   */
  manageSousLignes(ligneMereId: number, sousLignes: SousLigneRequestDto[]): Observable<LigneFonctionnelleResponse> {
    const url = `${this.apiUrl}/${ligneMereId}/sous-lignes`;
    return this.http.put<LigneFonctionnelleResponse>(url, sousLignes);
  }

  // ==========================================================================
  // CHART / HISTORY
  // ==========================================================================

  /**
   * Get LigneFonctionnelle Chart History
   * GET /lignes-fonctionnelles/history
   */
  getLigneFonctionnelleChartHistory(request: LigneFonctionnelleChartHistoryRequest): Observable<LigneFonctionnelleChartData> {
    let params = new HttpParams();
    params = params.append('banqueId', request.banqueId.toString());
    params = params.append('natureLigneFonctionnelleId', request.natureLigneFonctionnelleId.toString());

    return this.http.get<LigneFonctionnelleChartData>(`${this.apiUrl}/history`, { params });
  }

  // ==========================================================================
  // AGGREGATED TRACKING (SUIVI) - FOR GENERAL LINES
  // ==========================================================================

  /**
   * Helper to build pagination params
   */
  private buildParams(page: number, size: number, sort: string): HttpParams {
    return new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);
  }

  /**
   * Get Préfinancements by Line ID (General Line)
   * GET /lignes-fonctionnelles/{id}/prefinancements
   *
   * @param id - Line ID
   * @param page - Page number (0-indexed)
   * @param size - Page size
   * @param sort - Sort criteria (e.g., 'id,asc')
   * @returns Observable of aggregated préfinancements data with pagination
   */
  getPrefinancementsByLineId(
    id: number,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ): Observable<AgregatedSuiviePrefiLigneResponseDto> {
    const params = this.buildParams(page, size, sort);
    return this.http.get<AgregatedSuiviePrefiLigneResponseDto>(`${this.apiUrl}/${id}/prefinancements`, { params });
  }

  /**
   * Get Droits Constatés by Line ID (General Line)
   * GET /lignes-fonctionnelles/{id}/dc
   *
   * @param id - Line ID
   * @param page - Page number (0-indexed)
   * @param size - Page size
   * @param sort - Sort criteria (e.g., 'id,asc')
   * @returns Observable of aggregated droits constatés data with pagination
   */
  getDroitConstateByLineId(
    id: number,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ): Observable<AgregatedSuivieDcLigneResponseDto> {
    const params = this.buildParams(page, size, sort);
    return this.http.get<AgregatedSuivieDcLigneResponseDto>(`${this.apiUrl}/${id}/dc`, { params });
  }

  /**
   * Get Factoring by Line ID (General Line)
   * GET /lignes-fonctionnelles/{id}/factoring
   *
   * @param id - Line ID
   * @param page - Page number (0-indexed)
   * @param size - Page size
   * @param sort - Sort criteria (e.g., 'id,asc')
   * @returns Observable of aggregated factoring data with pagination
   */
  getFactoringByLineId(
    id: number,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ): Observable<AgregatedSuivieFactoringLigneResponseDto> {
    const params = this.buildParams(page, size, sort);
    return this.http.get<AgregatedSuivieFactoringLigneResponseDto>(`${this.apiUrl}/${id}/factoring`, { params });
  }

  getLcnByLineId(
    id: number,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<AgregatedSuivieLcnLigneResponseDto> {
    const params = this.buildParams(page, size, sort);
    return this.http.get<AgregatedSuivieLcnLigneResponseDto>(`${this.apiUrl}/${id}/lcn`, { params });
  }


}
