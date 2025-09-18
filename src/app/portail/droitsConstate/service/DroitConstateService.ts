import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from "../../../../environments/environment";
import { DroitConstateRequestDto } from "../dtos/DroitConstateRequestDto";
import { DroitConstateResponseDto } from "../dtos/DroitConstateResponseDto";
import { DroitConstateSearchDto } from "../dtos/DroitConstateSearchDto";
import { EncoursDroitConstateDto } from "../dtos/EncoursDroitConstateDto";
import { PageResponse } from "../../global/types/PageResponse";
import {EncoursDroitConstateSearchDto} from "../dtos/EncoursDroitConstateSearchDto";

@Injectable({
  providedIn: 'root',
})
export class DroitConstateService {

  private apiUrl = `${environment.apiUrl}droitsConstate`;

  constructor(private http: HttpClient) {}

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

    return this.http.get<PageResponse<DroitConstateResponseDto>>(this.apiUrl, { params });
  }

  /**
   * Changes the status of a DroitConstate record to 'PAID'.
   *
   * @param id The ID of the record to update.
   * @returns An Observable of the updated record's response DTO.
   */
  markAsPaid(id: number): Observable<DroitConstateResponseDto> {
    return this.http.patch<DroitConstateResponseDto>(`${this.apiUrl}/${id}/status/paid`, {});
  }

  /**
   * Retrieves the encours (in progress) remboursement records for a specific banque.
   *
   * @param banqueId The ID of the banque.
   * @param searchDto
   * @param page The page number (0-indexed).
   * @param size The number of elements per page.
   * @param sort The sorting string (e.g., 'dateEmission,asc').
   * @returns An Observable of a paginated list of DroitConstate response DTOs.
   */
  getEncoursRemboursement(
    banqueId: number,
    searchDto:EncoursDroitConstateSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,asc'
  ): Observable<EncoursDroitConstateDto> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if(searchDto.marcheName){
      params = params.set('marcheName', searchDto.marcheName);
    }

    if (searchDto.marcheNumero){
      params = params.set('marcheNumero', searchDto.marcheNumero);
    }
    return this.http.get<EncoursDroitConstateDto>(
      `${this.apiUrl}/remboursement/encours/${banqueId}`,
      { params }
    );
  }

  /**
   * Retrieves the encours (in progress) déblocage records for a specific banque.
   *
   * @param banqueId The ID of the banque.
   * @param searchDto
   * @param page The page number (0-indexed).
   * @param size The number of elements per page.
   * @param sort The sorting string (e.g., 'dateEmission,asc').
   * @returns An Observable of a paginated list of DroitConstate response DTOs.
   */
  getEncoursDeblocage(
    banqueId: number,
    searchDto:EncoursDroitConstateSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,asc'
  ): Observable<EncoursDroitConstateDto> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if(searchDto.marcheName){
      params = params.set('marcheName', searchDto.marcheName);
    }
    if (searchDto.marcheNumero){
      params = params.set('marcheNumero', searchDto.marcheNumero);
    }
    return this.http.get<EncoursDroitConstateDto>(
      `${this.apiUrl}/deblocage/encours/${banqueId}`,
      { params }
    );
  }
}
