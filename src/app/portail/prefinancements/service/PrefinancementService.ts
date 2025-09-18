import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {environment} from "../../../../environments/environment";
import {PrefinancementRequestDTO} from "../dtos/PrefinancementRequestDTO";
import {PrefinancementResponseDTO} from "../dtos/PrefinancementResponseDTO";
import {PrefinancementSearchDTO} from "../dtos/PrefinancementSearchDTO";
import {PageResponse} from "../../global/types/PageResponse";
import {EncoursPrefinancementDto} from "../dtos/EncoursPrefinancementDto";
import {EncoursPrefinancementSearchDto} from "../dtos/EncoursPrefinancementSearchDto";


@Injectable({
  providedIn: 'root',
})

export class PrefinancementService {

  private apiUrl = `${environment.apiUrl}prefinancements`;

  constructor(private http: HttpClient) {}

  /**
   * Creates a new Prefinancement record.
   *
   * @param request The DTO containing the data for the new record.
   * @returns An Observable of the newly created record's response DTO.
   */
  createPrefinancement(request: PrefinancementRequestDTO): Observable<PrefinancementResponseDTO> {
    return this.http.post<PrefinancementResponseDTO>(this.apiUrl, request);
  }

  /**
   * Updates an existing Prefinancement record by its ID.
   *
   * @param id The ID of the record to update.
   * @param request The DTO containing the updated data.
   * @returns An Observable of the updated record's response DTO.
   */
  updatePrefinancement(id: number, request: PrefinancementRequestDTO): Observable<PrefinancementResponseDTO> {
    return this.http.put<PrefinancementResponseDTO>(`${this.apiUrl}/${id}`, request);
  }

  /**
   * Deletes a Prefinancement record by its ID.
   *
   * @param id The ID of the record to delete.
   * @returns An Observable of void.
   */
  deletePrefinancement(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Retrieves a Prefinancement record by its ID.
   *
   * @param id The ID of the record to retrieve.
   * @returns An Observable of the Prefinancement response DTO.
   */
  getPrefinancementById(id: number): Observable<PrefinancementResponseDTO> {
    return this.http.get<PrefinancementResponseDTO>(`${this.apiUrl}/${id}`);
  }

  /**
   * Searches for Prefinancement records with pagination, sorting, and optional criteria.
   *
   * @param searchCriteria The DTO containing the search criteria.
   * @param page The page number (0-indexed).
   * @param size The number of elements per page.
   * @param sort The sorting string (e.g., 'id,asc').
   * @returns An Observable of a paginated list of Prefinancement response DTOs.
   */
  getAllPrefinancements(
    searchCriteria: PrefinancementSearchDTO,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ): Observable<PageResponse<PrefinancementResponseDTO>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (searchCriteria.marcheId) {
      params = params.set('marcheId', searchCriteria.marcheId.toString());
    }
    if (searchCriteria.banqueId) {
      params = params.set('banqueId', searchCriteria.banqueId.toString());
    }
    if (searchCriteria.type) {
      params = params.set('type', searchCriteria.type);
    }
    if (searchCriteria.status) {
      params = params.set('status', searchCriteria.status);
    }

    return this.http.get<PageResponse<PrefinancementResponseDTO>>(this.apiUrl, { params });
  }

  /**
   * Changes the status of a Prefinancement record to 'PAID'.
   *
   * @param id The ID of the record to update.
   * @returns An Observable of the updated record's response DTO.
   */
  markAsPaid(id: number): Observable<PrefinancementResponseDTO> {
    return this.http.patch<PrefinancementResponseDTO>(`${this.apiUrl}/${id}/status/paid`, {});
  }

  /**
   * Retrieves the encours (in progress) remboursement records for a specific banque.
   *
   * @param banqueId The ID of the banque.
   * @param searchDto
   * @param page The page number (0-indexed).
   * @param size The number of elements per page.
   * @param sort The sorting string (e.g., 'dateEmission,asc').
   * @returns An Observable of a paginated list of Prefinancement response DTOs.
   */
  getEncoursRemboursement(
    banqueId: number,
    searchDto:EncoursPrefinancementSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,asc'
  ): Observable<EncoursPrefinancementDto> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if(searchDto.marcheName){
      params = params.set('marcheName', searchDto.marcheName);
    }

    if(searchDto.marcheNumero){
      params = params.set('marcheNumero', searchDto.marcheNumero);
    }

    return this.http.get<EncoursPrefinancementDto>(
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
   * @returns An Observable of a paginated list of Prefinancement response DTOs.
   */
  getEncoursDeblocage(
    banqueId: number,
    searchDto:EncoursPrefinancementSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,asc'
  ): Observable<EncoursPrefinancementDto> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if(searchDto.marcheName){
      params = params.set('marcheName', searchDto.marcheName);
    }

    if(searchDto.marcheNumero){
      params = params.set('marcheNumero', searchDto.marcheNumero);
    }

    return this.http.get<EncoursPrefinancementDto>(
      `${this.apiUrl}/deblocage/encours/${banqueId}`,
      { params }
    );
  }

}
