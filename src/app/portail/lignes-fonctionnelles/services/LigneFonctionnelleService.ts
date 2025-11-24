// ligne-fonctionnelle.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { LigneFonctionnelleResponse } from "../dtos/LigneFonctionnelleResponse";
import { LigneFonctionnelleRequest } from '../dtos/LigneFonctionnelleRequest';
import { LigneFonctionnelleSearchCriteria } from "../dtos/LigneFonctionnelleSearchCriteria";
import { PageResponse } from "../../global/types/PageResponse";
import {LigneFonctionnelleChartData} from "../dtos/LigneFonctionnelleChartData";
import {LigneFonctionnelleChartHistoryRequest} from "../dtos/LigneFonctionnelleChartHistoryRequest";
import {SousLigneRequestDto} from "../dtos/SousLigneRequestDto";

@Injectable({
  providedIn: 'root',
})
export class LigneFonctionnelleService {
  private apiUrl = `${environment.apiUrl}lignes-fonctionnelles`;

  constructor(private http: HttpClient) {}

  // 1. Create LigneFonctionnelle (POST /lignes-fonctionnelles-encours)
  createLigneFonctionnelle(request: LigneFonctionnelleRequest): Observable<LigneFonctionnelleResponse> {
    return this.http.post<LigneFonctionnelleResponse>(this.apiUrl, request);
  }

  // 2. Get LigneFonctionnelle by ID (GET /lignes-fonctionnelles-encours/{id})
  getLigneFonctionnelleById(id: number): Observable<LigneFonctionnelleResponse> {
    return this.http.get<LigneFonctionnelleResponse>(`${this.apiUrl}/${id}`);
  }

  // 3. Get Active LigneFonctionnelles (GET /lignes-fonctionnelles-encours)
  // This method maps to the backend's getActiveLigneFonctionnelles endpoint.
  getCurrentLigneFonctionnelles(
    criteria: LigneFonctionnelleSearchCriteria, // This now includes actif
    page: number ,
    size: number ,
    sort: string
  ): Observable<PageResponse<LigneFonctionnelleResponse>> {
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    params = params.append('sort', sort);

    if (criteria.banqueId !== undefined && criteria.banqueId !== null) {
      params = params.append('banqueId', criteria.banqueId.toString());
    }
    if (criteria.natureLigneFonctionnelleId !== undefined && criteria.natureLigneFonctionnelleId !== null) {
      params = params.append('natureLigneFonctionnelleId', criteria.natureLigneFonctionnelleId.toString());
    }
    if (criteria.yearDepart !== undefined && criteria.yearDepart !== null) {
      params = params.append('yearDepart', criteria.yearDepart.toString());
    }

    if (criteria.actif !== undefined && criteria.actif !== null) {
      params = params.append('actif', criteria.actif.toString());
    }

    if (criteria.isExpired !== undefined && criteria.isExpired !== null) {
      params = params.append('isExpired', criteria.isExpired.toString());
    }

    return this.http.get<PageResponse<LigneFonctionnelleResponse>>(this.apiUrl, { params });
  }

  // 4. Get LignesFonctionnelles by BanqueOfDebit ID (GET /lignes-fonctionnelles-encours/banque/{banqueId})
  // This is the method explicitly marked as changed.
  getLignesFonctionnellesByBanqueId(
    banqueId: number,
    criteria: LigneFonctionnelleSearchCriteria , // Optional criteria with empty object as default
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ): Observable<PageResponse<LigneFonctionnelleResponse>> {
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    params = params.append('sort', sort);

    // Apply search criteria if provided (natureLigneFonctionnelleId, yearDepart)
    if (criteria.natureLigneFonctionnelleId !== undefined && criteria.natureLigneFonctionnelleId !== null) {
      params = params.append('natureLigneFonctionnelleId', criteria.natureLigneFonctionnelleId.toString());
    }
    if (criteria.yearDepart !== undefined && criteria.yearDepart !== null) {
      params = params.append('yearDepart', criteria.yearDepart.toString());
    }

     if (criteria.actif !== undefined && criteria.actif !== null) {
      params = params.append('actif', criteria.actif.toString());
     }

    return this.http.get<PageResponse<LigneFonctionnelleResponse>>(`${this.apiUrl}/banque/${banqueId}`, { params });
  }

  // 5. Update LigneFonctionnelle (PUT /lignes-fonctionnelles-encours/{id})
  updateLigneFonctionnelle(id: number, request: LigneFonctionnelleRequest): Observable<LigneFonctionnelleResponse> {
    return this.http.put<LigneFonctionnelleResponse>(`${this.apiUrl}/${id}`, request);
  }

  // 6. Delete LigneFonctionnelle (DELETE /lignes-fonctionnelles-encours/{id})
  deleteLigneFonctionnelle(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

    // 7. deactivate a functional ligne (PUT /lignes-fonctionnelles-encours/deactivate/{banqueId}/nature/{natureLigneFonctionnelleId})
     deactivateLigneFonctionnelle(ligneId: number): Observable<void> {
       return this.http.put<void>(`${this.apiUrl}/deactivate/${ligneId}`, {});
    }

  manageSousLignes(ligneMereId: number, sousLignes: SousLigneRequestDto[]): Observable<LigneFonctionnelleResponse> {
    const url = `${this.apiUrl}/${ligneMereId}/sous-lignes`;
    return this.http.put<LigneFonctionnelleResponse>(url, sousLignes);
  }

  // 8. Get LigneFonctionnelle Chart History (GET /lignes-fonctionnelles-encours/history)
  getLigneFonctionnelleChartHistory(request: LigneFonctionnelleChartHistoryRequest): Observable<LigneFonctionnelleChartData> {
    let params = new HttpParams();
    params = params.append('banqueId', request.banqueId.toString());
    params = params.append('natureLigneFonctionnelleId', request.natureLigneFonctionnelleId.toString());

    return this.http.get<LigneFonctionnelleChartData>(`${this.apiUrl}/history`, { params });
  }


}
