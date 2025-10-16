import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from "../../../../environments/environment";


import { FactoringRequestDto } from "../dtos/FactoringRequestDto";
import { FactoringResponseDto } from "../dtos/FactoringResponseDto";
import { FactoringSearchDto } from "../dtos/FactoringSearchDto";
import { EncoursFactoringDto } from "../dtos/EncoursFactoringDto";
import { EncoursFactoringSearchDto } from "../dtos/EncoursFactoringSearchDto";
import {PageResponse} from "../../global/types/PageResponse";



@Injectable({
  providedIn: 'root',
})
export class FactoringService {


  private apiUrl = `${environment.apiUrl}factoring`;

  constructor(private http: HttpClient) {}



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

    return this.http.get<PageResponse<FactoringResponseDto>>(this.apiUrl, { params });
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

  // --- Encours Operations ---

  /**
   * Retrieves the encours (in progress) **remboursement** records for a specific banque.
   * (GET /factoring/remboursement/encours/{banqueId})
   *
   * @param banqueId The ID of the banque.
   * @param searchDto Search criteria for marcheName and marcheNumero.
   * @param page The page number (0-indexed).
   * @param size The number of elements per page.
   * @param sort The sorting string (default 'dateEmission,asc').
   * @returns An Observable of EncoursFactoringDto.
   */
  getEncoursForRemboursement(
    banqueId: number,
    searchDto: EncoursFactoringSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,asc'
  ): Observable<EncoursFactoringDto> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (searchDto.marcheName) {
      params = params.set('marcheName', searchDto.marcheName);
    }
    if (searchDto.marcheNumero) {
      params = params.set('marcheNumero', searchDto.marcheNumero);
    }

    return this.http.get<EncoursFactoringDto>(
      `${this.apiUrl}/remboursement/encours/${banqueId}`,
      { params }
    );
  }

  /**
   * Retrieves the encours (in progress) **déblocage** records for a specific banque.
   * (GET /factoring/deblocage/encours/{banqueId})
   *
   * @param banqueId The ID of the banque.
   * @param searchDto Search criteria for marcheName and marcheNumero.
   * @param page The page number (0-indexed).
   * @param size The number of elements per page.
   * @param sort The sorting string (default 'dateEmission,asc').
   * @returns An Observable of EncoursFactoringDto.
   */
  getEncoursForDeblocage(
    banqueId: number,
    searchDto: EncoursFactoringSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,asc'
  ): Observable<EncoursFactoringDto> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (searchDto.marcheName) {
      params = params.set('marcheName', searchDto.marcheName);
    }
    if (searchDto.marcheNumero) {
      params = params.set('marcheNumero', searchDto.marcheNumero);
    }

    return this.http.get<EncoursFactoringDto>(
      `${this.apiUrl}/deblocage/encours/${banqueId}`,
      { params }
    );
  }
}
