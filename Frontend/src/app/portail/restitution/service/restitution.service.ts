import { Injectable } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RestitutionRequestDto } from '../dtos/RestitutionRequestDto';
import { RestitutionResponseDto } from '../dtos/RestitutionResponseDto';
import { RestitutionSearchDto } from '../dtos/RestitutionSearchDto';
import { EncoursRestitution } from '../dtos/EncoursRestitution';
import { PageResponse } from '../../global/types/PageResponse';
import { toLocalDateString } from '../../global/types/date.utils';

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
      this.toSearchPayload(searchDto),
      { params }
    );
  }

  /**
   * Convertit les champs {@code Date} de {@link RestitutionSearchDto} en chaînes `yyyy-MM-dd`
   * avant envoi en corps JSON — un objet {@code Date} sérialisé nativement produit un datetime
   * ISO complet que {@code LocalDate} côté backend ne sait pas parser.
   */
  private toSearchPayload(searchDto: RestitutionSearchDto): any {
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

  /**
   * Exporte en PDF l'intégralité des restitutions correspondant aux filtres fournis (mêmes
   * critères que {@link searchRestitutions}, sans pagination — le PDF contient tous les résultats
   * filtrés, pas seulement la page actuellement affichée).
   */
  exportPdf(searchDto: RestitutionSearchDto): Observable<Blob> {
    let params = new HttpParams();

    if (searchDto.banqueId != null) params = params.set('banqueId', searchDto.banqueId.toString());
    if (searchDto.libelle)          params = params.set('libelle', searchDto.libelle);
    if (searchDto.montant != null)  params = params.set('montant', searchDto.montant.toString());
    if (searchDto.status)           params = params.set('status', searchDto.status);
    params = this.appendDateRangeParams(params, searchDto);

    return this.http.get(`${this.apiUrl}/export/pdf`, { params, responseType: 'blob' });
  }

  /** Ajoute les 3 plages de dates (émission, opération, valeur) si renseignées. */
  private appendDateRangeParams(params: HttpParams, searchDto: RestitutionSearchDto): HttpParams {
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
   * Exporte en PDF l'intégralité de l'encours de restitutions d'une banque correspondant aux
   * filtres fournis (mêmes critères que {@link getOutstandingRestitutions}, sans pagination — le
   * PDF contient tous les résultats filtrés, pas seulement la page actuellement affichée).
   */
  exportEncoursPdf(banqueId: number, searchDto: RestitutionSearchDto): Observable<Blob> {
    let params = new HttpParams();

    if (searchDto.libelle)          params = params.set('libelle', searchDto.libelle);
    if (searchDto.montant != null)  params = params.set('montant', searchDto.montant.toString());
    if (searchDto.status)           params = params.set('status', searchDto.status);

    return this.http.get(`${this.apiUrl}/encours/${banqueId}/export/pdf`, { params, responseType: 'blob' });
  }

}
