
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { NatureLigneFonctionnelleResponse } from '../dtos/nature-ligne-fonctionnelle/NatureLigneFonctionnelleResponse';
import { NatureLigneFonctionnelleRequest } from '../dtos/nature-ligne-fonctionnelle/NatureLigneFonctionnelleRequest';
import { NatureLigneFonctionnelleSearchCriteria } from '../dtos/nature-ligne-fonctionnelle/NatureLigneFonctionnelleSearchCriteria';
import { PorteeNature } from '../dtos/nature-ligne-fonctionnelle/PorteeNature';
import { PageResponse } from '../../global/types/PageResponse';

@Injectable({
  providedIn: 'root',
})
export class NatureLigneFonctionnelleService {
  private apiUrl = `${environment.apiUrl}natures-lignes-fonctionnelles`;

  constructor(private http: HttpClient) {}

  /**
   * Crée une nouvelle NatureLigneFonctionnelle.
   * @param request Les données à créer.
   * @returns Un Observable de la NatureLigneFonctionnelleResponse créée.
   */
  createNatureLigneFonctionnelle(request: NatureLigneFonctionnelleRequest): Observable<NatureLigneFonctionnelleResponse> {
    return this.http.post<NatureLigneFonctionnelleResponse>(this.apiUrl, request);
  }

  /**
   * Récupère une NatureLigneFonctionnelle par son ID.
   * @param id L'ID de la NatureLigneFonctionnelle.
   * @returns Un Observable de la NatureLigneFonctionnelleResponse.
   */
  getNatureLigneFonctionnelleById(id: number): Observable<NatureLigneFonctionnelleResponse> {
    return this.http.get<NatureLigneFonctionnelleResponse>(`${this.apiUrl}/${id}`);
  }

  /**
   * Récupère toutes les NatureLigneFonctionnelle avec pagination et critères de recherche.
   * @param criteria Critères de recherche.
   * @param page Le numéro de page (base 0).
   * @param size Le nombre d'éléments par page.
   * @param sort Les critères de tri (ex: "id,asc").
   * @returns Un Observable d'une liste paginée de NatureLigneFonctionnelleResponse.
   */
  getAllNatureLigneFonctionnelles(
    criteria: NatureLigneFonctionnelleSearchCriteria,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ): Observable<PageResponse<NatureLigneFonctionnelleResponse>> {
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());
    params = params.append('sort', sort);

    if (criteria.code) {
      params = params.append('code', criteria.code);
    }
    if (criteria.label) {
      params = params.append('label', criteria.label);
    }
    if (criteria.description) {
      params = params.append('description', criteria.description);
    }
    if (criteria.portee) {
      params = params.append('portee', criteria.portee);
    }

    return this.http.get<PageResponse<NatureLigneFonctionnelleResponse>>(this.apiUrl, { params });
  }

  /**
   * Récupère toutes les NatureLigneFonctionnelle sous forme de liste simple (non paginée).
   * Utile pour les listes déroulantes.
   * @param portee Filtre optionnel par portée compatible :
   *   - GENERALE   → retourne GENERALE + GENERALE_ET_SPECIFIQUE
   *   - SPECIFIQUE → retourne SPECIFIQUE + GENERALE_ET_SPECIFIQUE
   *   - null/undefined → toutes les natures
   * @returns Un Observable d'une liste de NatureLigneFonctionnelleResponse.
   */
  getAllNatureLigneFonctionnellesList(portee?: PorteeNature): Observable<NatureLigneFonctionnelleResponse[]> {
    let params = new HttpParams();
    if (portee) {
      params = params.append('portee', portee);
    }
    return this.http.get<NatureLigneFonctionnelleResponse[]>(`${this.apiUrl}/list`, { params });
  }

  /**
   * Met à jour une NatureLigneFonctionnelle existante.
   * @param id L'ID de la NatureLigneFonctionnelle à mettre à jour.
   * @param request Les données mises à jour.
   * @returns Un Observable de la NatureLigneFonctionnelleResponse mise à jour.
   */
  updateNatureLigneFonctionnelle(id: number, request: NatureLigneFonctionnelleRequest): Observable<NatureLigneFonctionnelleResponse> {
    return this.http.put<NatureLigneFonctionnelleResponse>(`${this.apiUrl}/${id}`, request);
  }

  /**
   * Supprime une NatureLigneFonctionnelle par son ID.
   * @param id L'ID de la NatureLigneFonctionnelle à supprimer.
   * @returns Un Observable de void.
   */
  deleteNatureLigneFonctionnelle(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
