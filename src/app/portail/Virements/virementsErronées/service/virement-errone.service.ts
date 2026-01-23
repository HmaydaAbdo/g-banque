
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {VirementErroneResponseDto} from "../dtos/VirementErroneResponseDto";
import {EncoursVirementErroneDto} from "../dtos/EncoursVirementErroneDto";
import {VirementErroneSearchDto} from "../dtos/VirementErroneSearchDto";
import {VirementErroneRequestDto} from "../dtos/VirementErroneRequestDto";
import {environment} from "../../../../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class VirementErroneService {
  private apiUrl = `${environment.apiUrl}virement-errone`;

  constructor(private http: HttpClient) {
  }

  /**
   * Créer un nouveau virement erroné
   */
  createVirementErrone(dto: VirementErroneRequestDto): Observable<VirementErroneResponseDto> {
    return this.http.post<VirementErroneResponseDto>(this.apiUrl, dto);
  }

  /**
   * Récupérer un virement erroné par ID
   */
  getById(id: number): Observable<VirementErroneResponseDto> {
    return this.http.get<VirementErroneResponseDto>(`${this.apiUrl}/${id}`);
  }

  /**
   * Rechercher des virements erronés avec pagination
   */
  searchVirementErrone(
    searchDto: VirementErroneSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return this.http.post<any>(`${this.apiUrl}/search`, searchDto, {params});
  }

  /**
   * Mettre à jour un virement erroné
   */
  updateVirementErrone(id: number, dto: VirementErroneRequestDto): Observable<VirementErroneResponseDto> {
    return this.http.put<VirementErroneResponseDto>(`${this.apiUrl}/${id}`, dto);
  }

  /**
   * Supprimer un virement erroné
   */
  deleteVirementErrone(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Récupérer les encours des virements erronés pour une banque
   */
  getOutstandingVirementErrone(
    banqueId: number,
    searchDto: VirementErroneSearchDto = {},
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<EncoursVirementErroneDto> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    // Ajouter les paramètres de recherche
    if (searchDto.libelle) {
      params = params.set('libelle', searchDto.libelle);
    }
    if (searchDto.montant) {
      params = params.set('montant', searchDto.montant.toString());
    }
    if (searchDto.status) {
      params = params.set('status', searchDto.status);
    }

    return this.http.get<EncoursVirementErroneDto>(
      `${this.apiUrl}/encours/${banqueId}`,
      {params}
    );
  }

  /**
   * Basculer le statut d'un virement erroné (PREVU <-> PAYE)
   */
  toggleStatus(id: number): Observable<VirementErroneResponseDto> {
    return this.http.patch<VirementErroneResponseDto>(
      `${this.apiUrl}/${id}/toggle-status`,
      {}
    );
  }

}
