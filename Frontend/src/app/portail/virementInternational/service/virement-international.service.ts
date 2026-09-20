import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../global/types/PageResponse';

import { VirementInternationalRequestDto } from '../dtos/VirementInternationalRequestDto';
import { VirementInternationalResponseDto } from '../dtos/VirementInternationalResponseDto';
import { VirementInternationalSearchDto } from '../dtos/VirementInternationalSearchDto';
import {toLocalDateString} from "../../global/types/date.utils";

@Injectable({
  providedIn: 'root'
})
export class VirementInternationalService {

  private apiUrl = `${environment.apiUrl}virements-internationaux`;

  constructor(private http: HttpClient) {}

  /* ===================== CREATE ===================== */

  createVirementInternational(
    dto: VirementInternationalRequestDto
  ): Observable<VirementInternationalResponseDto> {
    return this.http.post<VirementInternationalResponseDto>(this.apiUrl, dto);
  }

  /* ===================== UPDATE ===================== */

  updateVirementInternational(
    id: number,
    dto: VirementInternationalRequestDto
  ): Observable<VirementInternationalResponseDto> {
    return this.http.put<VirementInternationalResponseDto>(
      `${this.apiUrl}/${id}`,
      dto
    );
  }

  /* ===================== GET BY ID ===================== */

  getVirementInternationalById(
    id: number
  ): Observable<VirementInternationalResponseDto> {
    return this.http.get<VirementInternationalResponseDto>(
      `${this.apiUrl}/${id}`
    );
  }

  /* ===================== DELETE ===================== */

  deleteVirementInternational(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /* ===================== SEARCH / LIST ===================== */

  getVirementsInternationaux(
    searchDto: VirementInternationalSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<PageResponse<VirementInternationalResponseDto>> {

    let params = this.buildSearchParams(searchDto, page, size, sort);

    return this.http.get<PageResponse<VirementInternationalResponseDto>>(
      this.apiUrl,
      { params }
    );
  }


  /* ===================== TOGGLE STATUS ===================== */

  toggleVirementInternationalStatus(
    id: number
  ): Observable<VirementInternationalResponseDto> {
    return this.http.patch<VirementInternationalResponseDto>(
      `${this.apiUrl}/toggle/${id}`,
      {}
    );
  }

  /* ===================== PRIVATE HELPERS ===================== */

  private buildSearchParams(
    searchDto: VirementInternationalSearchDto,
    page: number,
    size: number,
    sort: string
  ): HttpParams {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);
    params = this.buildFilterParams(params, searchDto);
    return params;
  }

  private buildFilterParams(initial: HttpParams, searchDto: VirementInternationalSearchDto): HttpParams {
    let params = initial;

    if (searchDto.banqueId !== null && searchDto.banqueId !== undefined) {
      params = params.set('banqueId', searchDto.banqueId.toString());
    }
    if (searchDto.beneficiaire) {
      params = params.set('beneficiaire', searchDto.beneficiaire);
    }
    if (searchDto.emetteur) {
      params = params.set('emetteur', searchDto.emetteur);
    }
    if (searchDto.montant !== null && searchDto.montant !== undefined) {
      params = params.set('montant', searchDto.montant.toString());
    }
    if (searchDto.libelle) {
      params = params.set('libelle', searchDto.libelle);
    }
    if (searchDto.observation) {
      params = params.set('observation', searchDto.observation);
    }
    if (searchDto.statut !== null && searchDto.statut !== undefined) {
      params = params.set('statut', searchDto.statut);
    }
    if (searchDto.type) {
      params = params.set('type', searchDto.type);
    }
    params = this.appendDateRangeParams(params, searchDto);

    return params;
  }

  /** Ajoute les 3 plages de dates (émission, opération, valeur) si renseignées. */
  private appendDateRangeParams(params: HttpParams, criteria: VirementInternationalSearchDto): HttpParams {
    if (criteria.dateEmissionFrom) params = params.set('dateEmissionFrom', toLocalDateString(criteria.dateEmissionFrom)!);
    if (criteria.dateEmissionTo) params = params.set('dateEmissionTo', toLocalDateString(criteria.dateEmissionTo)!);
    if (criteria.dateOperationFrom) params = params.set('dateOperationFrom', toLocalDateString(criteria.dateOperationFrom)!);
    if (criteria.dateOperationTo) params = params.set('dateOperationTo', toLocalDateString(criteria.dateOperationTo)!);
    if (criteria.dateValeurFrom) params = params.set('dateValeurFrom', toLocalDateString(criteria.dateValeurFrom)!);
    if (criteria.dateValeurTo) params = params.set('dateValeurTo', toLocalDateString(criteria.dateValeurTo)!);
    return params;
  }

  /* ===================== EXPORT PDF ===================== */

  exportPdf(searchDto: VirementInternationalSearchDto): Observable<Blob> {
    const params = this.buildFilterParams(new HttpParams(), searchDto);
    return this.http.get(`${this.apiUrl}/export/pdf`, {params, responseType: 'blob'});
  }
}
