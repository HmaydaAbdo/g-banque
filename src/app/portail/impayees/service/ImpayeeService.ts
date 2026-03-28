import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ImpayeeRequestDto } from '../dtos/ImpayeeRequestDto';
import { ImpayeeResponseDto } from '../dtos/ImpayeeResponseDto';
import { ImpayeeSearchDto } from '../dtos/ImpayeeSearchDto';
import { PageResponse } from '../../global/types/PageResponse';
import { EncoursImpayeeDto } from '../dtos/EncoursImpayeeDto';

@Injectable({
  providedIn: 'root'
})
export class ImpayeeService {

  private apiUrl = `${environment.apiUrl}impayees`;

  constructor(private http: HttpClient) {}

  // --- Create ---
  createImpayee(requestDto: ImpayeeRequestDto): Observable<ImpayeeResponseDto> {
    return this.http.post<ImpayeeResponseDto>(this.apiUrl, requestDto);
  }

  // --- Get by ID ---
  getImpayeeById(id: number): Observable<ImpayeeResponseDto> {
    return this.http.get<ImpayeeResponseDto>(`${this.apiUrl}/${id}`);
  }

  // --- Search with pagination ---
  searchImpayees(
    searchDto: ImpayeeSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ): Observable<PageResponse<ImpayeeResponseDto>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return this.http.post<PageResponse<ImpayeeResponseDto>>(`${this.apiUrl}/search`, searchDto, { params });
  }

  // --- Update ---
  updateImpayee(id: number, requestDto: ImpayeeRequestDto): Observable<ImpayeeResponseDto> {
    return this.http.put<ImpayeeResponseDto>(`${this.apiUrl}/${id}`, requestDto);
  }

  // --- Delete ---
  deleteImpayee(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // --- Get outstanding impayees ---
  getOutstandingImpayees(
    banqueId: number,
    searchDto: ImpayeeSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission'
  ): Observable<EncoursImpayeeDto> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (searchDto.libelle) {
      params = params.set('libelle', searchDto.libelle);
    }

    if (searchDto.motif) {
      params = params.set('motif', searchDto.motif);
    }

    if (searchDto.type) {
      params = params.set('type', searchDto.type);
    }

    if (searchDto.montant) {
      params = params.set('montant', searchDto.montant.toString());
    }

    if (searchDto.status) {
      params = params.set('status', searchDto.status);
    }

    return this.http.get<EncoursImpayeeDto>(`${this.apiUrl}/encours/${banqueId}`, { params });
  }

  // --- Toggle Status ---
  toggleStatus(id: number): Observable<ImpayeeResponseDto> {
    return this.http.patch<ImpayeeResponseDto>(`${this.apiUrl}/${id}/toggle-status`, {});
  }
}
