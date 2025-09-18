
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {environment} from "../../../../environments/environment";
import {LigneFonctionnelleSpecifiqueRequest} from "../dtos/LigneFonctionnelleSpecifiqueRequest";
import {LigneFonctionnelleSpecifiqueResponse} from "../dtos/LigneFonctionnelleSpecifiqueResponse";
import {LigneFonctionnelleSpecifiqueSearchCriteria} from "../dtos/LigneFonctionnelleSpecifiqueSearchCriteria";
import {PageResponse} from "../../global/types/PageResponse";
import {PrefiResponseDto} from "../../prefinancements/lignesPrefi/dtos/SpecificLigneResposneDto";
import {SpecifiqueLineSearchDto} from "../../prefinancements/lignesPrefi/dtos/SpecifiqueLineSearchDto";


@Injectable({
  providedIn: 'root',
})
export class SpecificFunctionalLigneService {
  private apiUrl = `${environment.apiUrl}lignes-fonctionnelles-specifiques`;

  constructor(private http: HttpClient) {}

  /**
   * 1. Create a specific functional line (POST /lignes-fonctionnelles-specifiques)
   */
  createLigneFonctionnelleSpecifique(request: LigneFonctionnelleSpecifiqueRequest): Observable<LigneFonctionnelleSpecifiqueResponse> {
    return this.http.post<LigneFonctionnelleSpecifiqueResponse>(this.apiUrl, request);
  }

  /**
   * 2. Get a specific functional line by ID (GET /lignes-fonctionnelles-specifiques/{id})
   */
  getLigneFonctionnelleSpecifiqueById(id: number): Observable<LigneFonctionnelleSpecifiqueResponse> {
    return this.http.get<LigneFonctionnelleSpecifiqueResponse>(`${this.apiUrl}/${id}`);
  }

  /**
   * 3. Get a paginated list of specific functional lignePrefi with search criteria
   * (GET /lignes-fonctionnelles-specifiques)
   */
  getLignesFonctionnellesSpecifiques(
    criteria: LigneFonctionnelleSpecifiqueSearchCriteria,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ): Observable<PageResponse<LigneFonctionnelleSpecifiqueResponse>> {
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    params = params.append('sort', sort);

    if (criteria.banqueId !== undefined && criteria.banqueId !== null) {
      params = params.append('banqueId', criteria.banqueId.toString());
    }
    if (criteria.marcheId !== undefined && criteria.marcheId !== null) {
      params = params.append('marcheId', criteria.marcheId.toString());
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

    return this.http.get<PageResponse<LigneFonctionnelleSpecifiqueResponse>>(this.apiUrl, { params });
  }

  /**
   * 4. Update a specific functional line (PUT /lignes-fonctionnelles-specifiques/{id})
   */
  updateLigneFonctionnelleSpecifique(id: number, request: LigneFonctionnelleSpecifiqueRequest): Observable<LigneFonctionnelleSpecifiqueResponse> {
    return this.http.put<LigneFonctionnelleSpecifiqueResponse>(`${this.apiUrl}/${id}`, request);
  }

  /**
   * 5. Deactivate a specific functional line (PUT /lignes-fonctionnelles-specifiques/deactivate/{ligneId})
   */
  deactivateLigneFonctionnelle(ligneId: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/deactivate/${ligneId}`, {});
  }

  /**
   * 6. Delete a specific functional line (DELETE /lignes-fonctionnelles-specifiques/{id})
   */
  deleteLigneFonctionnelleSpecifique(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getPrefi(
    searchCriteria: SpecifiqueLineSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ):Observable<PageResponse<PrefiResponseDto>>{
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    // Conditionally add search parameters
    if (searchCriteria.marcheId) {
      params = params.set('marcheId', searchCriteria.marcheId.toString());
    }
    if (searchCriteria.banqueId) {
      params = params.set('banqueId', searchCriteria.banqueId.toString());
    }

    return this.http.get<PageResponse<PrefiResponseDto>>(`${this.apiUrl}/prefi-lines`, { params });
  }

  getDcLines(
    searchCriteria: SpecifiqueLineSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ):Observable<PageResponse<PrefiResponseDto>>{
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    // Conditionally add search parameters
    if (searchCriteria.marcheId) {
      params = params.set('marcheId', searchCriteria.marcheId.toString());
    }
    if (searchCriteria.banqueId) {
      params = params.set('banqueId', searchCriteria.banqueId.toString());
    }

    return this.http.get<PageResponse<PrefiResponseDto>>(`${this.apiUrl}/dc-lines`, { params });
  }
}
