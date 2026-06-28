import { inject, Injectable } from '@angular/core';
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
    return this.http.post<PageResponse<AgiosResponseDto>>(`${this.apiUrl}/search`, dto, { params });
  }

  updateAgios(id: number, dto: AgiosRequestDto): Observable<AgiosResponseDto> {
    return this.http.put<AgiosResponseDto>(`${this.apiUrl}/${id}`, dto);
  }

  deleteAgios(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getOutstandingAgios(
    banqueId: number,
    dto?: AgiosSearchDto,
    page = 0,
    size = 10,
    sort = 'dateEmission,desc'
  ): Observable<EncoursAgiosDto> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', sort);

    if (dto) {
      Object.entries(dto).forEach(([key, value]) => {
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
