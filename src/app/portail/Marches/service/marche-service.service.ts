import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from "../../../../environments/environment";
import {MarcheRequestDTO} from "../dtos/marche-request.dto";
import {MarcheResponseDTO} from "../dtos/marche-response.dto";
import {MarcheSearchDTO} from "../dtos/marche-search.dto";
import {PageResponse} from "../../global/types/PageResponse";


@Injectable({
  providedIn: 'root'
})
export class MarcheService {

  private apiUrl = `${environment.apiUrl}marches`;

  constructor(private http: HttpClient) {
  }

  /**
   * Crée un nouveau marché.
   * @param marcheRequest Les données du marché.
   * @returns Un Observable de MarcheResponseDTO.
   */
  createMarche(marcheRequest: MarcheRequestDTO): Observable<MarcheResponseDTO> {
    return this.http.post<MarcheResponseDTO>(this.apiUrl, marcheRequest);
  }

  /**
   * Récupère un marché par son ID.
   * @param id L'ID du marché.
   * @returns Un Observable de MarcheResponseDTO.
   */
  getMarcheById(id: number): Observable<MarcheResponseDTO> {
    return this.http.get<MarcheResponseDTO>(`${this.apiUrl}/${id}`);
  }

  /**
   * Récupère tous les marchés avec recherche, pagination et tri.
   * @param criteria Les critères de recherche.
   * @param page Le numéro de page (0-indexed).
   * @param size Le nombre d'éléments par page.
   * @param sort La chaîne de tri (ex: "id,asc").
   * @returns Un Observable de PageResponse<MarcheResponseDTO>.
   */
  getAllMarches(
    criteria: MarcheSearchDTO,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ): Observable<PageResponse<MarcheResponseDTO>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    // Ajouter les critères de recherche si présents
    if (criteria.nom) {
      params = params.set('nom', criteria.nom);
    }
    if (criteria.numero) {
      params = params.set('numero', criteria.numero);
    }
    if (criteria.maitreDouvrage) {
      params = params.set('maitreDouvrage', criteria.maitreDouvrage);
    }

    return this.http.get<PageResponse<MarcheResponseDTO>>(this.apiUrl, {params});
  }


  /**
   * Récupère les marchés avec pagination et tri, sans critères de recherche.
   * @param page Le numéro de page (0-indexé).
   * @param size Le nombre d'éléments par page.
   * @param sort La chaîne de tri (ex: "id,asc").
   * @returns Un Observable de PageResponse<MarcheResponseDTO>.
   */
  getAllMarchesForLazyLoad(
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ): Observable<PageResponse<MarcheResponseDTO>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return this.http.get<PageResponse<MarcheResponseDTO>>(this.apiUrl, {params});
  }
  /**
   * Met à jour un marché existant.
   * @param id L'ID du marché à mettre à jour.
   * @param marcheRequest Les données du marché mises à jour.
   * @returns Un Observable de MarcheResponseDTO.
   */
  updateMarche(id: number, marcheRequest: MarcheRequestDTO): Observable<MarcheResponseDTO> {
    return this.http.put<MarcheResponseDTO>(`${this.apiUrl}/${id}`, marcheRequest);
  }

  /**
   * Supprime un marché par son ID.
   * @param id L'ID du marché à supprimer.
   * @returns Un Observable<void>.
   */
  deleteMarche(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
