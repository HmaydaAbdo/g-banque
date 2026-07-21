import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { CreditMoyenTermeResponseDto } from "../dtos/CreditMoyenTermeResponseDto";
import { CreditMoyenTermeSearchDto } from "../dtos/CreditMoyenTermeSearchDto";
import { PageResponse } from "../../global/types/PageResponse";
import { CreditMoyenTermeRequestDto } from "../dtos/CreditMoyenTermeRequestDto";
import { DeleteResponseDto } from "../dtos/DeleteResponseDto";

@Injectable({
  providedIn: 'root'
})
export class CmtService {

  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}credits-cmt`;

  /**
   * Fetches a paginated list of ACTIVE credits.
   */
  searchActiveCredits(searchDto: CreditMoyenTermeSearchDto): Observable<PageResponse<CreditMoyenTermeResponseDto>> {
    const params = this.buildSearchParams(searchDto);
    return this.http.get<PageResponse<CreditMoyenTermeResponseDto>>(`${this.apiUrl}/actifs`, { params });
  }

  /**
   * Fetches a paginated list of credits PENDING CLOSURE.
   */
  searchPendingClosureCredits(searchDto: CreditMoyenTermeSearchDto): Observable<PageResponse<CreditMoyenTermeResponseDto>> {
    const params = this.buildSearchParams(searchDto);
    return this.http.get<PageResponse<CreditMoyenTermeResponseDto>>(`${this.apiUrl}/en-attente-de-main-levees`, { params });
  }

  /**
   * Fetches a paginated list of ARCHIVED credits.
   */
  searchArchivedCredits(searchDto: CreditMoyenTermeSearchDto): Observable<PageResponse<CreditMoyenTermeResponseDto>> {
    const params = this.buildSearchParams(searchDto);
    return this.http.get<PageResponse<CreditMoyenTermeResponseDto>>(`${this.apiUrl}/archives`, { params });
  }

  /**
   * Fetches a single credit by ID.
   */
  getCreditById(id: number): Observable<CreditMoyenTermeResponseDto> {
    return this.http.get<CreditMoyenTermeResponseDto>(`${this.apiUrl}/${id}`);
  }

  /**
   * Exporte en PDF l'intégralité des crédits actifs correspondant aux filtres fournis
   * (mêmes critères que {@link searchActiveCredits}, sans pagination).
   */
  exportActiveCreditsPdf(searchDto: CreditMoyenTermeSearchDto): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/actifs/export/pdf`, { params: this.buildFilterParams(searchDto), responseType: 'blob' });
  }

  /**
   * Exporte en PDF l'intégralité des crédits en attente de main levée correspondant aux
   * filtres fournis (mêmes critères que {@link searchPendingClosureCredits}, sans pagination).
   */
  exportPendingClosureCreditsPdf(searchDto: CreditMoyenTermeSearchDto): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/en-attente-de-main-levees/export/pdf`, { params: this.buildFilterParams(searchDto), responseType: 'blob' });
  }

  /**
   * Exporte en PDF l'intégralité des crédits archivés correspondant aux filtres fournis
   * (mêmes critères que {@link searchArchivedCredits}, sans pagination).
   */
  exportArchivedCreditsPdf(searchDto: CreditMoyenTermeSearchDto): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/archives/export/pdf`, { params: this.buildFilterParams(searchDto), responseType: 'blob' });
  }

  /**
   * Creates a new credit.
   */
  createCredit(request: CreditMoyenTermeRequestDto): Observable<CreditMoyenTermeResponseDto> {
    return this.http.post<CreditMoyenTermeResponseDto>(this.apiUrl, request);
  }

  /**
   * Updates an existing credit.
   */
  updateCredit(id: number, request: CreditMoyenTermeRequestDto): Observable<CreditMoyenTermeResponseDto> {
    return this.http.put<CreditMoyenTermeResponseDto>(`${this.apiUrl}/${id}`, request);
  }

  /**
   * Archives a credit.
   */
  archiveCredit(id: number): Observable<CreditMoyenTermeResponseDto> {
    return this.http.put<CreditMoyenTermeResponseDto>(`${this.apiUrl}/${id}/archiver`, {});
  }

  /**
   * Deletes a credit by ID.
   */
  deleteCredit(id: number): Observable<DeleteResponseDto> {
    return this.http.delete<DeleteResponseDto>(`${this.apiUrl}/${id}`);
  }

  /**
   * Builds the HTTP parameters from the search DTO.
   * This centralizes the logic for pagination, sorting, and filters.
   */
  private buildSearchParams(searchDto: CreditMoyenTermeSearchDto): HttpParams {
    let params = new HttpParams();
    if (searchDto.designation) {
      params = params.set('designation', searchDto.designation);
    }
    if (searchDto.banqueId) {
      params = params.set('banqueId', searchDto.banqueId);
    }
    params = params.set('page', searchDto.page?.toString() || '0');
    params = params.set('size', searchDto.size?.toString() || '10');
    params = params.set('sort', searchDto.sort || 'id,asc');
    return params;
  }

  /**
   * Construit les paramètres de filtre seuls (sans pagination), utilisés par les exports PDF
   * qui impriment l'intégralité des résultats filtrés.
   */
  private buildFilterParams(searchDto: CreditMoyenTermeSearchDto): HttpParams {
    let params = new HttpParams();
    if (searchDto.designation) {
      params = params.set('designation', searchDto.designation);
    }
    if (searchDto.banqueId) {
      params = params.set('banqueId', searchDto.banqueId);
    }
    return params;
  }
}
