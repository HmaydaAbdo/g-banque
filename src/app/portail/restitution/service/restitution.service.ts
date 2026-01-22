import { Injectable } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RestitutionRequestDto } from '../dtos/RestitutionRequestDto';
import { RestitutionResponseDto } from '../dtos/RestitutionResponseDto';
import { RestitutionSearchDto } from '../dtos/RestitutionSearchDto';
import { EncoursRestitution } from '../dtos/EncoursRestitution';
import { PageResponse } from '../../global/types/PageResponse';

@Injectable({
  providedIn: 'root'
})
export class RestitutionService {

  private readonly apiUrl = `${environment.apiUrl}restitutions`;

  constructor(private http: HttpClient) {}

  /**
   * Créer une nouvelle restitution
   * @param requestDto Les données de la restitution à créer
   */
  createRestitution(requestDto: RestitutionRequestDto): Observable<RestitutionResponseDto> {
    return this.http.post<RestitutionResponseDto>(this.apiUrl, requestDto);
  }

  /**
   * Récupérer une restitution par son ID
   * @param id L'identifiant de la restitution
   */
  getById(id: number): Observable<RestitutionResponseDto> {
    return this.http.get<RestitutionResponseDto>(`${this.apiUrl}/${id}`);
  }

  /**
   * Rechercher des restitutions avec pagination
   * @param searchDto Les critères de recherche
   * @param page Le numéro de la page (commence à 0)
   * @param size La taille de la page
   * @param sort Le critère de tri
   */
  searchRestitutions(
    searchDto: RestitutionSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<PageResponse<RestitutionResponseDto>> {

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return this.http.post<PageResponse<RestitutionResponseDto>>(
      `${this.apiUrl}/search`,
      searchDto,
      { params }
    );
  }

  /**
   * Mettre à jour une restitution existante
   * @param id L'identifiant de la restitution
   * @param requestDto Les nouvelles données de la restitution
   */
  updateRestitution(id: number, requestDto: RestitutionRequestDto): Observable<RestitutionResponseDto> {
    return this.http.put<RestitutionResponseDto>(`${this.apiUrl}/${id}`, requestDto);
  }

  /**
   * Supprimer une restitution
   * @param id L'identifiant de la restitution à supprimer
   */
  deleteRestitution(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Récupérer les restitutions en cours (encours) pour une banque donnée
   * @param banqueId L'identifiant de la banque
   * @param searchDto Les critères de recherche optionnels
   * @param page Le numéro de la page
   * @param size La taille de la page
   * @param sort Le critère de tri
   */
  getOutstandingRestitutions(
    banqueId: number,
    searchDto?: RestitutionSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<EncoursRestitution> {

    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    // Ajouter les paramètres de recherche s'ils sont fournis
    if (searchDto) {
      Object.entries(searchDto).forEach(([key, value]) => {
        if (value !== null && value !== undefined && value !== '') {
          params = params.set(key, String(value));
        }
      });
    }

    return this.http.get<EncoursRestitution>(`${this.apiUrl}/encours/${banqueId}`, { params });
  }

  toggleStatus(id: number): Observable<RestitutionResponseDto> {
    return this.http.patch<RestitutionResponseDto>(
      `${this.apiUrl}/${id}/toggle-status`,
      null
    );
  }

}
