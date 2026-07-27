import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { OperationErroneResponseDto } from "../dtos/OperationErroneResponseDto";
import { EncoursOperationsErroneDto } from "../dtos/EncoursOperationsErroneDto";
import { OperationErroneSearchDto } from "../dtos/OperationErroneSearchDto";
import { OperationErroneRequestDto } from "../dtos/OperationErroneRequestDto";
import { environment } from "../../../../environments/environment";
import { toLocalDateString } from "../../global/types/date.utils";

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

    return this.http.post<any>(`${this.apiUrl}/search`, this.toSearchPayload(searchDto), { params });
  }

  /**
   * Convertit les champs {@code Date} de {@link OperationErroneSearchDto} en chaînes `yyyy-MM-dd`
   * avant envoi en corps JSON — un objet {@code Date} sérialisé nativement produit un datetime
   * ISO complet que {@code LocalDate} côté backend ne sait pas parser.
   */
  private toSearchPayload(searchDto: OperationErroneSearchDto): any {
    return {
      ...searchDto,
      dateEmissionDebut: toLocalDateString(searchDto.dateEmissionDebut),
      dateEmissionFin: toLocalDateString(searchDto.dateEmissionFin),
      dateOperationDebut: toLocalDateString(searchDto.dateOperationDebut),
      dateOperationFin: toLocalDateString(searchDto.dateOperationFin),
      dateValeurDebut: toLocalDateString(searchDto.dateValeurDebut),
      dateValeurFin: toLocalDateString(searchDto.dateValeurFin),
    };
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

  /**
   * Exporte en PDF l'intégralité des opérations erronées correspondant aux filtres fournis
   * (mêmes critères que {@link searchOperationErrone}, sans pagination — le PDF contient tous les
   * résultats filtrés, pas seulement la page actuellement affichée).
   * GET /operation-errone/export/pdf
   */
  exportPdf(searchDto: OperationErroneSearchDto): Observable<Blob> {
    let params = new HttpParams();

    if (searchDto.banqueId != null) params = params.set('banqueId', searchDto.banqueId.toString());
    if (searchDto.libelle)          params = params.set('libelle', searchDto.libelle);
    if (searchDto.montant != null)  params = params.set('montant', searchDto.montant.toString());
    if (searchDto.status)           params = params.set('status', searchDto.status);
    if (searchDto.motif)            params = params.set('motif', searchDto.motif);
    params = this.appendDateRangeParams(params, searchDto);

    return this.http.get(`${this.apiUrl}/export/pdf`, { params, responseType: 'blob' });
  }

  /** Ajoute les 3 plages de dates (émission, opération, valeur) si renseignées. */
  private appendDateRangeParams(params: HttpParams, searchDto: OperationErroneSearchDto): HttpParams {
    if (searchDto.dateEmissionDebut) {
      params = params.set('dateEmissionDebut', toLocalDateString(searchDto.dateEmissionDebut)!);
    }
    if (searchDto.dateEmissionFin) {
      params = params.set('dateEmissionFin', toLocalDateString(searchDto.dateEmissionFin)!);
    }
    if (searchDto.dateOperationDebut) {
      params = params.set('dateOperationDebut', toLocalDateString(searchDto.dateOperationDebut)!);
    }
    if (searchDto.dateOperationFin) {
      params = params.set('dateOperationFin', toLocalDateString(searchDto.dateOperationFin)!);
    }
    if (searchDto.dateValeurDebut) {
      params = params.set('dateValeurDebut', toLocalDateString(searchDto.dateValeurDebut)!);
    }
    if (searchDto.dateValeurFin) {
      params = params.set('dateValeurFin', toLocalDateString(searchDto.dateValeurFin)!);
    }
    return params;
  }

  /**
   * Exporte en PDF l'intégralité de l'encours d'opérations erronées d'une banque correspondant
   * aux filtres fournis (mêmes critères que {@link getOutstandingOperationErrone}, sans
   * pagination — le PDF contient tous les résultats filtrés, pas seulement la page actuellement
   * affichée).
   * GET /operation-errone/encours/{banqueId}/export/pdf
   */
  exportEncoursPdf(banqueId: number, searchDto: OperationErroneSearchDto): Observable<Blob> {
    let params = new HttpParams();

    if (searchDto.libelle)         params = params.set('libelle', searchDto.libelle);
    if (searchDto.montant != null) params = params.set('montant', searchDto.montant.toString());
    if (searchDto.motif)           params = params.set('motif', searchDto.motif);

    return this.http.get(`${this.apiUrl}/encours/${banqueId}/export/pdf`, { params, responseType: 'blob' });
  }
}
