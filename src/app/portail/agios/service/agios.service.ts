import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

import { AgiosRequestDto } from '../dtos/AgiosRequestDto';
import { AgiosResponseDto } from '../dtos/AgiosResponseDto';
import { AgiosSearchDto } from '../dtos/AgiosSearchDto';
import { EncoursAgiosDto } from '../dtos/EncoursAgiosDto';
import { PageResponse } from '../../global/types/PageResponse';

@Injectable({
  providedIn: 'root'
})
export class AgiosService {

  /** Base API URL for Agios endpoints */
  private readonly apiUrl = `${environment.apiUrl}agios`;

  constructor(private http: HttpClient) {}

  /**
   * Creates a new Agios record.
   *
   * @param requestDto Data required to create the Agios.
   * @returns Observable emitting the created AgiosResponseDto.
   */
  createAgios(requestDto: AgiosRequestDto): Observable<AgiosResponseDto> {
    return this.http.post<AgiosResponseDto>(this.apiUrl, requestDto);
  }

  /**
   * Retrieves an Agios record by its ID.
   *
   * @param id Identifier of the Agios to retrieve.
   * @returns Observable emitting the AgiosResponseDto.
   */
  getById(id: number): Observable<AgiosResponseDto> {
    return this.http.get<AgiosResponseDto>(`${this.apiUrl}/${id}`);
  }

  /**
   * Searches for Agios with pagination and sorting.
   *
   * @param searchDto Criteria for filtering Agios records.
   * @param page Current page number (0-indexed).
   * @param size Number of records per page.
   * @param sort Sorting criteria, e.g., 'dateEmission,desc'.
   * @returns Observable emitting a paginated list of AgiosResponseDto.
   */
  searchAgios(
    searchDto: AgiosSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<PageResponse<AgiosResponseDto>> {

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return this.http.post<PageResponse<AgiosResponseDto>>(`${this.apiUrl}/search`, searchDto, { params });
  }

  /**
   * Updates an existing Agios record.
   *
   * @param id Identifier of the Agios to update.
   * @param requestDto Updated data for the Agios.
   * @returns Observable emitting the updated AgiosResponseDto.
   */
  updateAgios(id: number, requestDto: AgiosRequestDto): Observable<AgiosResponseDto> {
    return this.http.put<AgiosResponseDto>(`${this.apiUrl}/${id}`, requestDto);
  }

  /**
   * Deletes an Agios record by its ID.
   *
   * @param id Identifier of the Agios to delete.
   * @returns Observable emitting void upon success.
   */
  deleteAgios(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Retrieves a paginated list of outstanding Agios for a specific bank,
   * including the total outstanding amount.
   *
   * @param banqueId ID of the bank for which to retrieve Agios.
   * @param searchDto Optional filter criteria.
   * @param page Current page number (0-indexed).
   * @param size Number of records per page.
   * @param sort Sorting criteria, e.g., 'dateEmission,desc'.
   * @returns Observable emitting the EncoursAgiosDto.
   */
  getOutstandingAgios(
    banqueId: number,
    searchDto?: AgiosSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<EncoursAgiosDto> {

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

    return this.http.get<EncoursAgiosDto>(`${this.apiUrl}/encours/${banqueId}`, { params });
  }

  toggleStatus(id: number): Observable<AgiosResponseDto> {
    return this.http.patch<AgiosResponseDto>(`${this.apiUrl}/${id}/toggle-status`, {});
  }
}
