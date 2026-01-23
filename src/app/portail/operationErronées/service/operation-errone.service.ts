import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { OperationErroneResponseDto } from "../dtos/OperationErroneResponseDto";
import { EncoursOperationsErroneDto } from "../dtos/EncoursOperationsErroneDto";
import { OperationErroneSearchDto } from "../dtos/OperationErroneSearchDto";
import { OperationErroneRequestDto } from "../dtos/OperationErroneRequestDto";
import { environment } from "../../../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class OperationErroneService {
  private apiUrl = `${environment.apiUrl}operation-errone`;

  constructor(private http: HttpClient) {
  }

  /**
   * Créer une nouvelle opération erronée
   * POST /operation-errone
   */
  createOperationErrone(dto: OperationErroneRequestDto): Observable<OperationErroneResponseDto> {
    return this.http.post<OperationErroneResponseDto>(this.apiUrl, dto);
  }

  /**
   * Récupérer une opération erronée par ID
   * GET /operation-errone/{id}
   */
  getById(id: number): Observable<OperationErroneResponseDto> {
    return this.http.get<OperationErroneResponseDto>(`${this.apiUrl}/${id}`);
  }

  /**
   * Rechercher des opérations erronées avec pagination
   * POST /operation-errone/search
   */
  searchOperationErrone(
    searchDto: OperationErroneSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return this.http.post<any>(`${this.apiUrl}/search`, searchDto, { params });
  }

  /**
   * Mettre à jour une opération erronée
   * PUT /operation-errone/{id}
   */
  updateOperationErrone(id: number, dto: OperationErroneRequestDto): Observable<OperationErroneResponseDto> {
    return this.http.put<OperationErroneResponseDto>(`${this.apiUrl}/${id}`, dto);
  }

  /**
   * Supprimer une opération erronée
   * DELETE /operation-errone/{id}
   */
  deleteOperationErrone(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Récupérer les encours des opérations erronées pour une banque
   * GET /operation-errone/encours/{banqueId}
   */
  getOutstandingOperationErrone(
    banqueId: number,
    searchDto: OperationErroneSearchDto = {},
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<EncoursOperationsErroneDto> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    // Ajouter les paramètres de recherche optionnels
    if (searchDto.libelle) {
      params = params.set('libelle', searchDto.libelle);
    }
    if (searchDto.montant !== undefined && searchDto.montant !== null) {
      params = params.set('montant', searchDto.montant.toString());
    }
    if (searchDto.status) {
      params = params.set('status', searchDto.status);
    }
    if (searchDto.motif) {
      params = params.set('motif', searchDto.motif);
    }

    return this.http.get<EncoursOperationsErroneDto>(
      `${this.apiUrl}/encours/${banqueId}`,
      { params }
    );
  }

  /**
   * Basculer le statut d'une opération erronée (PREVU <-> PAYE)
   * PATCH /operation-errone/{id}/toggle-status
   */
  toggleStatus(id: number): Observable<OperationErroneResponseDto> {
    return this.http.patch<OperationErroneResponseDto>(
      `${this.apiUrl}/${id}/toggle-status`,
      {}
    );
  }
}
