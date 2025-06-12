import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { LigneFonctionnelleResponse } from "../dtos/LigneFonctionnelleResponse";
import { LigneFonctionnelleRequest } from '../dtos/LigneFonctionnelleRequest';
import { LigneFonctionnelleSearchCriteria } from "../dtos/LigneFonctionnelleSearchCriteria";
import { PageResponse } from "../../global/types/PageResponse";


@Injectable({
  providedIn: 'root',
})
export class LigneFonctionnelleService {
  private apiUrl = `${environment.apiUrl}lignes-fonctionnelles`; // Base URL for functional lines API

  constructor(private http: HttpClient) {}

  /**
   * Creates a new functional line.
   * @param request The functional line data to create.
   * @returns An Observable of the created LigneFonctionnelle.
   */
  createLigneFonctionnelle(request: LigneFonctionnelleRequest): Observable<LigneFonctionnelleResponse> {
    return this.http.post<LigneFonctionnelleResponse>(this.apiUrl, request);
  }

  /**
   * Retrieves a functional line by its ID.
   * @param id The ID of the functional line.
   * @returns An Observable of the LigneFonctionnelle.
   */
  getLigneFonctionnelleById(id: number): Observable<LigneFonctionnelleResponse> {
    return this.http.get<LigneFonctionnelleResponse>(`${this.apiUrl}/${id}`);
  }

  /**
   * Retrieves all functional lines with pagination and search criteria.
   * @param criteria Search criteria.
   * @param page The page number (0-indexed).
   * @param size The number of items per page.
   * @param sort The sorting criteria (e.g., "id,asc").
   * @returns An Observable of a paginated list of LigneFonctionnelle.
   */
  getCurrentLigneFonctionnelles(
    criteria: LigneFonctionnelleSearchCriteria,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
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

    return this.http.get<PageResponse<LigneFonctionnelleResponse>>(this.apiUrl, { params });
  }

  /**
   * Retrieves all functional lines by bank ID.
   * @param banqueId The ID of the bank.
   * @returns An Observable of a list of LigneFonctionnelle.
   */
  getLignesFonctionnellesByBanqueId(banqueId: number): Observable<LigneFonctionnelleResponse[]> {
    return this.http.get<LigneFonctionnelleResponse[]>(`${this.apiUrl}/banque/${banqueId}`);
  }

  /**
   * Updates an existing functional line.
   * @param id The ID of the functional line to update.
   * @param request The updated functional line data.
   * @returns An Observable of the updated LigneFonctionnelle.
   */
  updateLigneFonctionnelle(id: number, request: LigneFonctionnelleRequest): Observable<LigneFonctionnelleResponse> {
    return this.http.put<LigneFonctionnelleResponse>(`${this.apiUrl}/${id}`, request);
  }

  /**
   * Deletes a functional line by its ID.
   * @param id The ID of the functional line to delete.
   * @returns An Observable of void.
   */
  deleteLigneFonctionnelle(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
