// interet.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {InteretRequestDto} from "../dtos/InteretRequestDto";
import {InteretResponseDto} from "../dtos/InteretResponseDto";
import {InteretSearchDto} from "../dtos/InteretSearchDto";
import {PageResponse} from "../../global/types/PageResponse";
import {EncoursInteretDto} from "../dtos/EncoursInteretDto"; // Update path as needed


@Injectable({
  providedIn: 'root'
})
export class InteretService {


  private readonly apiUrl = `${environment.apiUrl}interest`;

  constructor(private http: HttpClient) {}

  // --- Create (POST /interest) ---
  createInterest(requestDto: InteretRequestDto): Observable<InteretResponseDto> {
    return this.http.post<InteretResponseDto>(this.apiUrl, requestDto);
  }

  // --- Get by ID (GET /interest/{id}) ---
  getById(id: number): Observable<InteretResponseDto> {
    return this.http.get<InteretResponseDto>(`${this.apiUrl}/${id}`);
  }

  // --- Search (POST /interest/search) ---
  searchInterest(
    searchDto: InteretSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<PageResponse<InteretResponseDto>> {

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    // The Spring controller uses POST with a request body for the search criteria
    return this.http.post<PageResponse<InteretResponseDto>>(`${this.apiUrl}/search`, searchDto, { params });
  }

  // --- Update (PUT /interest/{id}) ---
  updateInterest(id: number, requestDto: InteretRequestDto): Observable<InteretResponseDto> {
    return this.http.put<InteretResponseDto>(`${this.apiUrl}/${id}`, requestDto);
  }

  // --- Delete (DELETE /interest/{id}) ---
  deleteInterest(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // --- Get Outstanding Interest (GET /interest/encours/{banqueId}) ---
  getOutstandingInterest(
    banqueId: number,
    searchDto: InteretSearchDto = {},
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<EncoursInteretDto> {

    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    // Add optional search criteria as query parameters, matching the @ModelAttribute binding
    Object.entries(searchDto).forEach(([key, value]) => {
      if (value !== null && value !== undefined && value !== '') {
        params = params.set(key, String(value));
      }
    });

    return this.http.get<EncoursInteretDto>(`${this.apiUrl}/encours/${banqueId}`, { params });
  }

  toggleStatus(id: number): Observable<InteretResponseDto> {
    return this.http.patch<InteretResponseDto>(`${this.apiUrl}/${id}/toggle-status`, {});
  }

}
