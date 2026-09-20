import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from "../../../../environments/environment";


import { FactoringRequestDto } from "../dtos/FactoringRequestDto";
import { FactoringResponseDto } from "../dtos/FactoringResponseDto";
import { FactoringSearchDto } from "../dtos/FactoringSearchDto";
import {PageResponse} from "../../global/types/PageResponse";



@Injectable({
  providedIn: 'root',
})
export class FactoringService {


  private readonly http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}factoring`;



  /**
   * Creates a new Factoring record. (POST /factoring)
   *
   * @param requestDTO The DTO containing the data for the new record.
   * @returns An Observable of the newly created record's response DTO.
   */
  createFactoring(requestDTO: FactoringRequestDto): Observable<FactoringResponseDto> {
    return this.http.post<FactoringResponseDto>(this.apiUrl, requestDTO);
  }

  /**
   * Updates an existing Factoring record by its ID. (PUT /factoring/{id})
   *
   * @param id The ID of the record to update.
   * @param requestDTO The DTO containing the updated data.
   * @returns An Observable of the updated record's response DTO.
   */
  updateFactoring(id: number, requestDTO: FactoringRequestDto): Observable<FactoringResponseDto> {
    return this.http.put<FactoringResponseDto>(`${this.apiUrl}/${id}`, requestDTO);
  }

  /**
   * Deletes a Factoring record by its ID. (DELETE /factoring/{id})
   *
   * @param id The ID of the record to delete.
   * @returns An Observable of void (204 No Content).
   */
  deleteFactoring(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Retrieves a Factoring record by its ID. (GET /factoring/{id})
   *
   * @param id The ID of the record to retrieve.
   * @returns An Observable of the Factoring response DTO.
   */
  getFactoringById(id: number): Observable<FactoringResponseDto> {
    return this.http.get<FactoringResponseDto>(`${this.apiUrl}/${id}`);
  }

  // --- Search and Status Operations ---

  /**
   * Searches for Factoring records with pagination, sorting, and optional criteria. (GET /factoring)
   *
   * @param searchDTO The DTO containing the search criteria.
   * @param page The page number (0-indexed).
   * @param size The number of elements per page.
   * @param sort The sorting string (e.g., 'id,asc').
   * @returns An Observable of a paginated list of Factoring response DTOs.
   */
  searchFactoring(
    searchDTO: FactoringSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ): Observable<PageResponse<FactoringResponseDto>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    // Append search criteria to params
    if (searchDTO.marcheId) {
      params = params.set('marcheId', searchDTO.marcheId.toString());
    }
    if (searchDTO.banqueId) {
      params = params.set('banqueId', searchDTO.banqueId.toString());
    }
    if (searchDTO.status) {
      params = params.set('status', searchDTO.status);
    }
    if (searchDTO.type) {
      params = params.set('type', searchDTO.type);
    }
    params = this.appendDateRangeParams(params, searchDTO);

    return this.http.get<PageResponse<FactoringResponseDto>>(this.apiUrl, { params });
  }

  /** Ajoute les 3 paires de plages de dates (émission/opération/valeur) si renseignées. */
  private appendDateRangeParams(params: HttpParams, searchDTO: FactoringSearchDto): HttpParams {
    if (searchDTO.dateEmissionFrom) params = params.set('dateEmissionFrom', this.toIsoDate(searchDTO.dateEmissionFrom));
    if (searchDTO.dateEmissionTo) params = params.set('dateEmissionTo', this.toIsoDate(searchDTO.dateEmissionTo));
    if (searchDTO.dateOperationFrom) params = params.set('dateOperationFrom', this.toIsoDate(searchDTO.dateOperationFrom));
    if (searchDTO.dateOperationTo) params = params.set('dateOperationTo', this.toIsoDate(searchDTO.dateOperationTo));
    if (searchDTO.dateValeurFrom) params = params.set('dateValeurFrom', this.toIsoDate(searchDTO.dateValeurFrom));
    if (searchDTO.dateValeurTo) params = params.set('dateValeurTo', this.toIsoDate(searchDTO.dateValeurTo));
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
   * Toggles the status of a Factoring record. (PATCH /factoring/{id}/toggleStatus)
   *
   * @param id The ID of the record.
   * @returns An Observable of the updated record's response DTO.
   */
  toggleStatus(id: number): Observable<FactoringResponseDto> {
    return this.http.patch<FactoringResponseDto>(`${this.apiUrl}/${id}/toggleStatus`, {});
  }

  // --- Export PDF ---

  /**
   * Exporte en PDF l'intégralité des factorings correspondant aux critères fournis (mêmes
   * filtres que {@link searchFactoring}, sans pagination).
   */
  exportPdf(searchDTO: FactoringSearchDto): Observable<Blob> {
    let params = new HttpParams();

    if (searchDTO.marcheId) params = params.set('marcheId', searchDTO.marcheId.toString());
    if (searchDTO.banqueId) params = params.set('banqueId', searchDTO.banqueId.toString());
    if (searchDTO.status) params = params.set('status', searchDTO.status);
    if (searchDTO.type) params = params.set('type', searchDTO.type);
    params = this.appendDateRangeParams(params, searchDTO);

    return this.http.get(`${this.apiUrl}/export/pdf`, { params, responseType: 'blob' });
  }
}
