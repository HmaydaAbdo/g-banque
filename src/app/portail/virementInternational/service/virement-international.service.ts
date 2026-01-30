import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../global/types/PageResponse';

import { VirementInternationalRequestDto } from '../dtos/VirementInternationalRequestDto';
import { VirementInternationalResponseDto } from '../dtos/VirementInternationalResponseDto';
import { VirementInternationalSearchDto } from '../dtos/VirementInternationalSearchDto';
import { EncoursVirementInternationalResponseDto } from '../dtos/EncoursVirementInternationalResponseDto';
import { EncoursVirementsInternationalSearchDto } from '../dtos/EncoursVirementsInternationalSearchDto';

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

  /* ===================== ENCOURS ===================== */

  getEncoursDecaissement(
    banqueId: number,
    searchDto: EncoursVirementsInternationalSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<EncoursVirementInternationalResponseDto> {

    let params = this.buildEncoursParams(searchDto, page, size, sort);

    return this.http.get<EncoursVirementInternationalResponseDto>(
      `${this.apiUrl}/decaissement/encours/${banqueId}`,
      { params }
    );
  }

  getEncoursEncaissement(
    banqueId: number,
    searchDto: EncoursVirementsInternationalSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<EncoursVirementInternationalResponseDto> {

    let params = this.buildEncoursParams(searchDto, page, size, sort);

    return this.http.get<EncoursVirementInternationalResponseDto>(
      `${this.apiUrl}/encaissement/encours/${banqueId}`,
      { params }
    );
  }

  /* ===================== PRIVATE HELPERS ===================== */

  private buildSearchParams(
    searchDto: any,
    page: number,
    size: number,
    sort: string
  ): HttpParams {

    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    Object.entries(searchDto || {}).forEach(([key, value]) => {
      if (value !== null && value !== undefined && value !== '') {
        if (value instanceof Date) {
          params = params.set(key, value.toISOString());
        } else {
          params = params.set(key, value.toString());
        }
      }
    });

    return params;
  }

  private buildEncoursParams(
    searchDto: EncoursVirementsInternationalSearchDto,
    page: number,
    size: number,
    sort: string
  ): HttpParams {

    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (searchDto.dateEmission) {
      params = params.set('dateEmission', searchDto.dateEmission);
    }
    if (searchDto.beneficiaire) {
      params = params.set('beneficiaire', searchDto.beneficiaire);
    }
    if (searchDto.emetteur) {
      params = params.set('emetteur', searchDto.emetteur);
    }
    if (searchDto.montant) {
      params = params.set('montant', searchDto.montant.toString());
    }

    return params;
  }
}
