import { Injectable, inject } from '@angular/core';
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

  private readonly http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}prefinancements`;

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
    params = this.appendDateRangeParams(params, searchCriteria);

    return this.http.get<PageResponse<PrefinancementResponseDTO>>(this.apiUrl, { params });
  }

  /** Ajoute les 3 paires de plages de dates (émission/opération/valeur) si renseignées. */
  private appendDateRangeParams(params: HttpParams, searchCriteria: PrefinancementSearchDTO): HttpParams {
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
   * Changes the status of a Prefinancement record to 'PAID'.
   *
   * @param id The ID of the record to update.
   * @returns An Observable of the updated record's response DTO.
   */
  toggleStatus(id: number): Observable<PrefinancementResponseDTO> {
    return this.http.patch<PrefinancementResponseDTO>(`${this.apiUrl}/${id}/status/toggle`, {});
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

    if(searchDto.montant){
      params = params.set('montant', searchDto.montant.toString());
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

    if(searchDto.montant){
      params = params.set('montant', searchDto.montant.toString());
    }

    return this.http.get<EncoursPrefinancementDto>(
      `${this.apiUrl}/deblocage/encours/${banqueId}`,
      { params }
    );
  }

  /**
   * Exporte en PDF l'intégralité des préfinancements correspondant aux critères fournis (mêmes
   * filtres que {@link getAllPrefinancements}, sans pagination).
   */
  exportPdf(searchCriteria: PrefinancementSearchDTO): Observable<Blob> {
    let params = new HttpParams();

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
    params = this.appendDateRangeParams(params, searchCriteria);

    return this.http.get(`${this.apiUrl}/export/pdf`, { params, responseType: 'blob' });
  }

  /**
   * Exporte en PDF l'encours de remboursement d'une banque (mêmes filtres que
   * {@link getEncoursRemboursement}, sans pagination).
   */
  exportRemboursementEncoursPdf(banqueId: number, searchDto: EncoursPrefinancementSearchDto): Observable<Blob> {
    let params = new HttpParams();

    if (searchDto.marcheName) params = params.set('marcheName', searchDto.marcheName);
    if (searchDto.marcheNumero) params = params.set('marcheNumero', searchDto.marcheNumero);
    if (searchDto.montant) params = params.set('montant', searchDto.montant.toString());

    return this.http.get(`${this.apiUrl}/remboursement/encours/${banqueId}/export/pdf`, { params, responseType: 'blob' });
  }

  /**
   * Exporte en PDF l'encours de déblocage d'une banque (mêmes filtres que
   * {@link getEncoursDeblocage}, sans pagination).
   */
  exportDeblocageEncoursPdf(banqueId: number, searchDto: EncoursPrefinancementSearchDto): Observable<Blob> {
    let params = new HttpParams();

    if (searchDto.marcheName) params = params.set('marcheName', searchDto.marcheName);
    if (searchDto.marcheNumero) params = params.set('marcheNumero', searchDto.marcheNumero);
    if (searchDto.montant) params = params.set('montant', searchDto.montant.toString());

    return this.http.get(`${this.apiUrl}/deblocage/encours/${banqueId}/export/pdf`, { params, responseType: 'blob' });
  }

}
