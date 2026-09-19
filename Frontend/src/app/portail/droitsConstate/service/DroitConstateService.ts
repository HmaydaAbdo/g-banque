import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from "../../../../environments/environment";
import { DroitConstateRequestDto } from "../dtos/DroitConstateRequestDto";
import { DroitConstateResponseDto } from "../dtos/DroitConstateResponseDto";
import { DroitConstateSearchDto } from "../dtos/DroitConstateSearchDto";
import { PageResponse } from "../../global/types/PageResponse";

@Injectable({
  providedIn: 'root',
})
export class DroitConstateService {

  private apiUrl = `${environment.apiUrl}droitsConstate`;

  private readonly http = inject(HttpClient);

  /**
   * Creates a new DroitConstate record.
   *
   * @param request The DTO containing the data for the new record.
   * @returns An Observable of the newly created record's response DTO.
   */
  createDroitConstate(request: DroitConstateRequestDto): Observable<DroitConstateResponseDto> {
    return this.http.post<DroitConstateResponseDto>(this.apiUrl, request);
  }

  /**
   * Updates an existing DroitConstate record by its ID.
   *
   * @param id The ID of the record to update.
   * @param request The DTO containing the updated data.
   * @returns An Observable of the updated record's response DTO.
   */
  updateDroitConstate(id: number, request: DroitConstateRequestDto): Observable<DroitConstateResponseDto> {
    return this.http.put<DroitConstateResponseDto>(`${this.apiUrl}/${id}`, request);
  }

  /**
   * Deletes a DroitConstate record by its ID.
   *
   * @param id The ID of the record to delete.
   * @returns An Observable of void.
   */
  deleteDroitConstate(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Retrieves a DroitConstate record by its ID.
   *
   * @param id The ID of the record to retrieve.
   * @returns An Observable of the DroitConstate response DTO.
   */
  getDroitConstateById(id: number): Observable<DroitConstateResponseDto> {
    return this.http.get<DroitConstateResponseDto>(`${this.apiUrl}/${id}`);
  }

  /**
   * Searches for DroitConstate records with pagination, sorting, and optional criteria.
   *
   * @param searchCriteria The DTO containing the search criteria.
   * @param page The page number (0-indexed).
   * @param size The number of elements per page.
   * @param sort The sorting string (e.g., 'id,asc').
   * @returns An Observable of a paginated list of DroitConstate response DTOs.
   */
  getAllDroitConstates(
    searchCriteria: DroitConstateSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ): Observable<PageResponse<DroitConstateResponseDto>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    // Append search criteria to params if they exist
    if (searchCriteria.marcheId) {
      params = params.set('marcheId', searchCriteria.marcheId.toString());
    }
    if (searchCriteria.banqueId) {
      params = params.set('banqueId', searchCriteria.banqueId.toString());
    }
    if (searchCriteria.status) {
      params = params.set('status', searchCriteria.status);
    }
    if (searchCriteria.type) {
      params = params.set('type', searchCriteria.type);
    }
    params = this.appendDateRangeParams(params, searchCriteria);

    return this.http.get<PageResponse<DroitConstateResponseDto>>(this.apiUrl, { params });
  }

  /** Ajoute les 3 paires de plages de dates (émission/opération/valeur) si renseignées. */
  private appendDateRangeParams(params: HttpParams, searchCriteria: DroitConstateSearchDto): HttpParams {
    if (searchCriteria.dateEmissionFrom) params = params.set('dateEmissionFrom', this.toIsoDate(searchCriteria.dateEmissionFrom));
    if (searchCriteria.dateEmissionTo) params = params.set('dateEmissionTo', this.toIsoDate(searchCriteria.dateEmissionTo));
    if (searchCriteria.dateOperationFrom) params = params.set('dateOperationFrom', this.toIsoDate(searchCriteria.dateOperationFrom));
    if (searchCriteria.dateOperationTo) params = params.set('dateOperationTo', this.toIsoDate(searchCriteria.dateOperationTo));
    if (searchCriteria.dateValeurFrom) params = params.set('dateValeurFrom', this.toIsoDate(searchCriteria.dateValeurFrom));
    if (searchCriteria.dateValeurTo) params = params.set('dateValeurTo', this.toIsoDate(searchCriteria.dateValeurTo));
    return params;
  }

  /** Formate un objet Date en chaîne ISO yyyy-MM-dd pour Spring. */
  private toIsoDate(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  /**
   * Changes the status of a DroitConstate record to 'PAID'.
   *
   * @param id The ID of the record to update.
   * @returns An Observable of the updated record's response DTO.
   */
  toggleStatus(id: number): Observable<DroitConstateResponseDto> {
    return this.http.patch<DroitConstateResponseDto>(`${this.apiUrl}/${id}/status/toggle`, {});
  }

  /**
   * Exporte en PDF l'intégralité des droits constatés correspondant aux critères fournis (mêmes
   * filtres que {@link getAllDroitConstates}, sans pagination).
   */
  exportPdf(searchCriteria: DroitConstateSearchDto): Observable<Blob> {
    let params = new HttpParams();

    if (searchCriteria.marcheId) params = params.set('marcheId', searchCriteria.marcheId.toString());
    if (searchCriteria.banqueId) params = params.set('banqueId', searchCriteria.banqueId.toString());
    if (searchCriteria.status) params = params.set('status', searchCriteria.status);
    if (searchCriteria.type) params = params.set('type', searchCriteria.type);
    params = this.appendDateRangeParams(params, searchCriteria);

    return this.http.get(`${this.apiUrl}/export/pdf`, { params, responseType: 'blob' });
  }
}
