import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { CautionResponseDto }  from '../dtos/CautionResponseDto';
import { CautionRequestDto }   from '../dtos/CautionRequestDto';
import { CautionSearchDto }    from '../dtos/CautionSearchDto';
import { PageResponse }        from '../../global/types/PageResponse';

/**
 * Service HTTP pour le module Caution.
 *
 * HttpClient uniquement ici — jamais dans les composants (SRP).
 * Chaque méthode retourne un Observable brut ; la logique de store et
 * de gestion d'état est déléguée aux composants via CautionStore.
 */
@Injectable({ providedIn: 'root' })
export class CautionService {

  private readonly apiUrl = `${environment.apiUrl}cautions`;

  constructor(private http: HttpClient) {}

  // ── Recherche paginée ─────────────────────────────────────────────────────

  /**
   * Recherche paginée avec filtres dynamiques.
   * Seuls les critères non-null/non-undefined sont envoyés en query params.
   */
  rechercher(criteres: CautionSearchDto): Observable<PageResponse<CautionResponseDto>> {
    const params = this.buildParams(criteres);
    return this.http.get<PageResponse<CautionResponseDto>>(this.apiUrl, { params });
  }

  // ── Lecture ───────────────────────────────────────────────────────────────

  getById(id: number): Observable<CautionResponseDto> {
    return this.http.get<CautionResponseDto>(`${this.apiUrl}/${id}`);
  }

  // ── Écriture ──────────────────────────────────────────────────────────────

  creer(request: CautionRequestDto): Observable<CautionResponseDto> {
    return this.http.post<CautionResponseDto>(this.apiUrl, request);
  }

  modifier(id: number, request: CautionRequestDto): Observable<CautionResponseDto> {
    return this.http.put<CautionResponseDto>(`${this.apiUrl}/${id}`, request);
  }

  /**
   * Bascule l'état actif/inactif d'une caution (admin uniquement côté backend).
   * Si actif = false, ce bouton ne doit pas apparaître côté frontend.
   */
  toggleActif(id: number): Observable<CautionResponseDto> {
    return this.http.patch<CautionResponseDto>(`${this.apiUrl}/${id}/toggle-actif`, {});
  }

  /**
   * Suppression physique.
   * Le backend refuse si actif = false → 422.
   */
  supprimer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // ── Helpers privés ────────────────────────────────────────────────────────

  private buildParams(criteres: CautionSearchDto): HttpParams {
    let params = new HttpParams();

    if (criteres.numeroCaution)             params = params.set('numeroCaution', criteres.numeroCaution);
    if (criteres.organisme)                 params = params.set('organisme',     criteres.organisme);
    if (criteres.banqueId  != null)         params = params.set('banqueId',      criteres.banqueId.toString());
    if (criteres.natureId  != null)         params = params.set('natureId',      criteres.natureId.toString());
    if (criteres.marcheId  != null)         params = params.set('marcheId',      criteres.marcheId.toString());
    if (criteres.dateDePrise)               params = params.set('dateDePrise',   criteres.dateDePrise);
    if (criteres.montant   != null)         params = params.set('montant',       criteres.montant.toString());
    if (criteres.actif     != null)         params = params.set('actif',         criteres.actif.toString());

    params = params.set('page', (criteres.page  ?? 0).toString());
    params = params.set('size', (criteres.size  ?? 10).toString());
    params = params.set('sort', criteres.sort   ?? 'dateDePrise,desc');

    return params;
  }
}
