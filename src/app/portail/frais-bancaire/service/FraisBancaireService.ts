import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

import { FraisBancaireRequestDto } from '../dtos/FraisBancaireRequestDto';
import { FraisBancaireResponseDto } from '../dtos/FraisBancaireResponseDto';
import { FraisBancaireSearchDto } from '../dtos/FraisBancaireSearchDto';
import { EncoursFraisBancaireDto } from '../dtos/EncoursFraisBancaireDto';
import { PageResponse } from '../../global/types/PageResponse';

@Injectable({
  providedIn: 'root'
})
export class FraisBancaireService {

  /** Base API URL for Frais Bancaire endpoints */
  private readonly apiUrl = `${environment.apiUrl}frais-bancaire`;

  constructor(private http: HttpClient) {}

  /** Create a new Frais Bancaire */
  createFraisBancaire(requestDto: FraisBancaireRequestDto): Observable<FraisBancaireResponseDto> {
    return this.http.post<FraisBancaireResponseDto>(this.apiUrl, requestDto);
  }

  /** Get Frais Bancaire by ID */
  getById(id: number): Observable<FraisBancaireResponseDto> {
    return this.http.get<FraisBancaireResponseDto>(`${this.apiUrl}/${id}`);
  }

  /** Search Frais Bancaire with pagination */
  searchFraisBancaire(
    searchDto: FraisBancaireSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<PageResponse<FraisBancaireResponseDto>> {

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return this.http.post<PageResponse<FraisBancaireResponseDto>>(`${this.apiUrl}/search`, searchDto, { params });
  }

  /** Update Frais Bancaire */
  updateFraisBancaire(id: number, requestDto: FraisBancaireRequestDto): Observable<FraisBancaireResponseDto> {
    return this.http.put<FraisBancaireResponseDto>(`${this.apiUrl}/${id}`, requestDto);
  }

  /** Delete Frais Bancaire */
  deleteFraisBancaire(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Get outstanding Frais Bancaires (encours) for a given bank
   */
  getOutstandingFraisBancaire(
    banqueId: number,
    searchDto?: FraisBancaireSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<EncoursFraisBancaireDto> {

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

    return this.http.get<EncoursFraisBancaireDto>(`${this.apiUrl}/encours/${banqueId}`, { params });
  }
}
