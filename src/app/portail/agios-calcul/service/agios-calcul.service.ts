import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, Subject, tap } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { AgiosTrimestrielResponseDto } from '../dtos/AgiosTrimestrielResponseDto';
import { AgiosJournalierResponseDto } from '../dtos/AgiosJournalierResponseDto';
import { AgiosTrimestrielSearchDto } from '../dtos/AgiosTrimestrielSearchDto';
import { AgiosJournalierSearchDto } from '../dtos/AgiosJournalierSearchDto';
import { TypeAgios } from '../enums/TypeAgios';
import {PageResponse} from "../../global/types/PageResponse";
import {AgiosDetailAggregateDto} from "../dtos/Agiosdetailaggregatedto";

@Injectable({
  providedIn: 'root'
})
export class AgiosCalculService {

  private readonly apiUrl = `${environment.apiUrl}agios/calcul`;
  private readonly refresh$ = new Subject<void>();

  constructor(private http: HttpClient) {}

  // ═══════════════════════════════════════════════════════════════
  //  RÉFÉRENTIEL
  // ═══════════════════════════════════════════════════════════════

  /** GET /agios/calcul/types */
  getTypesActifs(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/types`);
  }

  // ═══════════════════════════════════════════════════════════════
  //  CONSULTATION PAGINÉE
  // ═══════════════════════════════════════════════════════════════

  /**
   * Recherche paginée des trimestres.
   * GET /agios/calcul/trimestres
   */
  searchTrimestres(
    dto: AgiosTrimestrielSearchDto,
    page: number,
    size: number,
    sort: string
  ): Observable<PageResponse<AgiosTrimestrielResponseDto>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (dto.banqueId != null) params = params.set('banqueId', dto.banqueId.toString());
    if (dto.type != null) params = params.set('type', dto.type);
    if (dto.annee != null) params = params.set('annee', dto.annee.toString());
    if (dto.trimestre != null) params = params.set('trimestre', dto.trimestre.toString());
    if (dto.statut != null) params = params.set('statut', dto.statut);

    return this.http.get<PageResponse<AgiosTrimestrielResponseDto>>(
      `${this.apiUrl}/trimestres`, { params }
    );
  }

  /**
   * Détail journalier paginé.
   * GET /agios/calcul/detail
   */
  searchDetail(
    dto: AgiosJournalierSearchDto,
    page: number,
    size: number,
    sort: string
  ): Observable<PageResponse<AgiosJournalierResponseDto>> {
    let params = this.buildJournalierParams(dto)
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return this.http.get<PageResponse<AgiosJournalierResponseDto>>(
      `${this.apiUrl}/detail`, { params }
    );
  }

  /**
   * Agrégats journaliers (HT, TVA, TTC, jours) avec date limite optionnelle.
   * GET /agios/calcul/detail/aggregates
   */
  getDetailAggregates(dto: AgiosJournalierSearchDto): Observable<AgiosDetailAggregateDto> {
    const params = this.buildJournalierParams(dto);
    return this.http.get<AgiosDetailAggregateDto>(
      `${this.apiUrl}/detail/aggregates`, { params }
    );
  }

  // ═══════════════════════════════════════════════════════════════
  //  ACTIONS
  // ═══════════════════════════════════════════════════════════════

  /**
   * Recalcul manuel d'un trimestre précis pour une banque donnée.
   * POST /agios/calcul/banque/{banqueId}/recalculer/{trim}/{annee}
   */
  recalculerParBanque(banqueId: number, trim: number, annee: number): Observable<string> {
    return this.http.post(
      `${this.apiUrl}/banque/${banqueId}/recalculer/${trim}/${annee}`,
      {},
      { responseType: 'text' }
    ).pipe(
      tap(() => this.triggerRefresh())
    );
  }

  /** POST /agios/calcul/recalculer */
  recalculerTout(): Observable<string> {
    return this.http.post(`${this.apiUrl}/recalculer`, {}, { responseType: 'text' }).pipe(
      tap(() => this.triggerRefresh())
    );
  }

  /** POST /agios/calcul/banque/{id}/type/{t}/cloturer/{trim}/{annee} */
  cloturerTrimestre(
    banqueId: number,
    type: TypeAgios,
    trimestre: number,
    annee: number
  ): Observable<AgiosTrimestrielResponseDto> {
    return this.http.post<AgiosTrimestrielResponseDto>(
      `${this.apiUrl}/banque/${banqueId}/type/${type}/cloturer/${trimestre}/${annee}`, {}
    ).pipe(
      tap(() => this.triggerRefresh())
    );
  }

  /** Signal de rafraîchissement */
  triggerRefresh(): void {
    this.refresh$.next();
  }

  // ═══════════════════════════════════════════════════════════════
  //  UTILS
  // ═══════════════════════════════════════════════════════════════

  private buildJournalierParams(dto: AgiosJournalierSearchDto): HttpParams {
    let params = new HttpParams();
    if (dto.banqueId != null) params = params.set('banqueId', dto.banqueId.toString());
    if (dto.type != null) params = params.set('type', dto.type);
    if (dto.trimestre != null) params = params.set('trimestre', dto.trimestre.toString());
    if (dto.annee != null) params = params.set('annee', dto.annee.toString());
    if (dto.dateLimite) params = params.set('dateLimite', dto.dateLimite);
    return params;
  }
}
