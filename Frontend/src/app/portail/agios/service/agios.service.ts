import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

import { AgiosRequestDto } from '../dtos/AgiosRequestDto';
import { AgiosResponseDto } from '../dtos/AgiosResponseDto';
import { AgiosSearchDto } from '../dtos/AgiosSearchDto';
import { PageResponse } from '../../global/types/PageResponse';
import { toLocalDateString } from '../../global/types/date.utils';

@Injectable({
  providedIn: 'root'
})
export class AgiosService {

  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}agios`;

  createAgios(dto: AgiosRequestDto): Observable<AgiosResponseDto> {
    return this.http.post<AgiosResponseDto>(this.apiUrl, dto);
  }

  getById(id: number): Observable<AgiosResponseDto> {
    return this.http.get<AgiosResponseDto>(`${this.apiUrl}/${id}`);
  }

  searchAgios(
    dto: AgiosSearchDto,
    page = 0,
    size = 10,
    sort = 'dateEmission,desc'
  ): Observable<PageResponse<AgiosResponseDto>> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', sort);
    return this.http.post<PageResponse<AgiosResponseDto>>(`${this.apiUrl}/search`, this.toSearchPayload(dto), { params });
  }

  /**
   * Convertit les champs {@code Date} de {@link AgiosSearchDto} en chaînes `yyyy-MM-dd` avant
   * envoi en corps JSON — un objet {@code Date} sérialisé nativement produit un datetime ISO
   * complet que {@code LocalDate} côté backend ne sait pas parser.
   */
  private toSearchPayload(dto: AgiosSearchDto): any {
    return {
      ...dto,
      dateEmissionDebut: toLocalDateString(dto.dateEmissionDebut),
      dateEmissionFin: toLocalDateString(dto.dateEmissionFin),
      dateOperationDebut: toLocalDateString(dto.dateOperationDebut),
      dateOperationFin: toLocalDateString(dto.dateOperationFin),
      dateValeurDebut: toLocalDateString(dto.dateValeurDebut),
      dateValeurFin: toLocalDateString(dto.dateValeurFin),
    };
  }

  updateAgios(id: number, dto: AgiosRequestDto): Observable<AgiosResponseDto> {
    return this.http.put<AgiosResponseDto>(`${this.apiUrl}/${id}`, dto);
  }

  deleteAgios(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  toggleStatus(id: number): Observable<AgiosResponseDto> {
    return this.http.patch<AgiosResponseDto>(`${this.apiUrl}/${id}/toggle-status`, {});
  }

  exportAgiosPdf(dto?: AgiosSearchDto): Observable<Blob> {
    let params = new HttpParams();
    if (dto) {
      if (dto.banqueId != null) params = params.set('banqueId', dto.banqueId.toString());
      if (dto.libelle) params = params.set('libelle', dto.libelle);
      if (dto.status) params = params.set('status', dto.status);
      params = this.appendDateRangeParams(params, dto);
    }
    return this.http.get(`${this.apiUrl}/export/pdf`, { params, responseType: 'blob' });
  }

  /** Ajoute les 3 plages de dates (émission, opération, valeur) si renseignées. */
  private appendDateRangeParams(params: HttpParams, dto: AgiosSearchDto): HttpParams {
    if (dto.dateEmissionDebut) {
      params = params.set('dateEmissionDebut', toLocalDateString(dto.dateEmissionDebut)!);
    }
    if (dto.dateEmissionFin) {
      params = params.set('dateEmissionFin', toLocalDateString(dto.dateEmissionFin)!);
    }
    if (dto.dateOperationDebut) {
      params = params.set('dateOperationDebut', toLocalDateString(dto.dateOperationDebut)!);
    }
    if (dto.dateOperationFin) {
      params = params.set('dateOperationFin', toLocalDateString(dto.dateOperationFin)!);
    }
    if (dto.dateValeurDebut) {
      params = params.set('dateValeurDebut', toLocalDateString(dto.dateValeurDebut)!);
    }
    if (dto.dateValeurFin) {
      params = params.set('dateValeurFin', toLocalDateString(dto.dateValeurFin)!);
    }
    return params;
  }
}
