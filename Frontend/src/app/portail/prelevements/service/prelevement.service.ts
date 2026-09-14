import { Injectable } from "@angular/core";
import { environment } from "../../../../environments/environment";
import { HttpClient, HttpParams } from "@angular/common/http";
import { PrelevementRequestDto } from "../dtos/PrelevementRequestDto";
import { Observable } from "rxjs";
import { PrelevementResponseDto } from "../dtos/PrelevementResponseDto";
import { PageResponse } from "../../global/types/PageResponse";
import { PrelevementSearchDto } from "../dtos/PrelevementSearchDto";
import { EncoursPrelevement } from "../dtos/EncoursPrelevement";

@Injectable({
  providedIn: 'root'
})
export class PrelevementService {

  /** Base API URL for Prelevement endpoints */
  private readonly apiUrl = `${environment.apiUrl}prelevements`;

  constructor(private http: HttpClient) {}

  /**
   * Creates a new Prelevement record.
   *
   * @param requestDto Data required to create the Prelevement.
   * @returns Observable emitting the created PrelevementResponseDto.
   */
  createPrelevement(requestDto: PrelevementRequestDto): Observable<PrelevementResponseDto> {
    return this.http.post<PrelevementResponseDto>(this.apiUrl, requestDto);
  }

  /**
   * Retrieves a Prelevement record by its ID.
   *
   * @param id Identifier of the Prelevement to retrieve.
   * @returns Observable emitting the PrelevementResponseDto.
   */
  getById(id: number): Observable<PrelevementResponseDto> {
    return this.http.get<PrelevementResponseDto>(`${this.apiUrl}/${id}`);
  }

  /**
   * Searches for Prelevements with pagination and sorting.
   *
   * @param searchDto Criteria for filtering Prelevement records.
   * @param page Current page number (0-indexed).
   * @param size Number of records per page.
   * @param sort Sorting criteria, e.g., 'dateEmission,desc'.
   * @returns Observable emitting a paginated list of PrelevementResponseDto.
   */
  searchPrelevements(
    searchDto: PrelevementSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<PageResponse<PrelevementResponseDto>> {

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return this.http.post<PageResponse<PrelevementResponseDto>>(`${this.apiUrl}/search`, searchDto, { params });
  }

  /**
   * Updates an existing Prelevement record.
   *
   * @param id Identifier of the Prelevement to update.
   * @param requestDto Updated data for the Prelevement.
   * @returns Observable emitting the updated PrelevementResponseDto.
   */
  updatePrelevement(id: number, requestDto: PrelevementRequestDto): Observable<PrelevementResponseDto> {
    return this.http.put<PrelevementResponseDto>(`${this.apiUrl}/${id}`, requestDto);
  }

  /**
   * Deletes a Prelevement record by its ID.
   *
   * @param id Identifier of the Prelevement to delete.
   * @returns Observable emitting void upon success.
   */
  deletePrelevement(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Retrieves a paginated list of outstanding Prelevements for a specific bank,
   * including the total outstanding amount.
   *
   * @param banqueId ID of the bank for which to retrieve Prelevements.
   * @param searchDto Optional filter criteria.
   * @param page Current page number (0-indexed).
   * @param size Number of records per page.
   * @param sort Sorting criteria, e.g., 'dateEmission,desc'.
   * @returns Observable emitting the EncoursPrelevement.
   */
  getOutstandingPrelevement(
    banqueId: number,
    searchDto?: PrelevementSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<EncoursPrelevement> {

    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (searchDto) {
      Object.entries(searchDto).forEach(([key, value]) => {
        if (value !== null && value !== undefined && value !== '') {
          params = params.set(key, String(value));
        }
      });
    }

    return this.http.get<EncoursPrelevement>(`${this.apiUrl}/encours/${banqueId}`, { params });
  }

  /**
   * Toggles the status of a Prelevement
   * @param id Identifier of the Prelevement to toggle.
   * @returns Observable emitting the updated PrelevementResponseDto.
   */
  toggleStatus(id: number): Observable<PrelevementResponseDto> {
    return this.http.patch<PrelevementResponseDto>(`${this.apiUrl}/${id}/toggle-status`, {});
  }

  exportPdf(searchDto: PrelevementSearchDto): Observable<Blob> {
    let params = new HttpParams();
    Object.entries(searchDto || {}).forEach(([key, value]) => {
      if (value !== null && value !== undefined && value !== '') {
        params = params.set(key, String(value));
      }
    });
    return this.http.get(`${this.apiUrl}/export/pdf`, {params, responseType: 'blob'});
  }

  exportEncoursPdf(banqueId: number, searchDto: PrelevementSearchDto): Observable<Blob> {
    let params = new HttpParams();
    Object.entries(searchDto || {}).forEach(([key, value]) => {
      if (value !== null && value !== undefined && value !== '') {
        params = params.set(key, String(value));
      }
    });
    return this.http.get(`${this.apiUrl}/encours/${banqueId}/export/pdf`, {params, responseType: 'blob'});
  }
}
