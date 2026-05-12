import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { CautionResponseDto }      from '../dtos/CautionResponseDto';
import { CautionRequestDto }       from '../dtos/CautionRequestDto';
import { CautionSearchDto }        from '../dtos/CautionSearchDto';
import { CloturerCautionRequest }  from '../dtos/CloturerCautionRequest';
import { CloturerCautionResponse } from '../dtos/CloturerCautionResponse';
import { PageResponse }            from '../../global/types/PageResponse';

/**
 * Service HTTP + état réactif pour le module Caution.
 *
 * Principe : BehaviorSubject privé → mutation interne uniquement.
 *            Observable public    → les composants lisent en lecture seule.
 *
 * Pas de store séparé — tout est centralisé ici pour garder
 * la cohérence sans couche supplémentaire.
 */
@Injectable({ providedIn: 'root' })
export class CautionService {

  private readonly apiUrl = `${environment.apiUrl}cautions`;

  constructor(private http: HttpClient) {}

  // ── État réactif ──────────────────────────────────────────────────────────

  private readonly _cautions        = new BehaviorSubject<CautionResponseDto[]>([]);
  private readonly _totalElements   = new BehaviorSubject<number>(0);
  private readonly _loading         = new BehaviorSubject<boolean>(false);
  private readonly _selected        = new BehaviorSubject<CautionResponseDto | null>(null);

  readonly cautions$      = this._cautions.asObservable();
  readonly totalElements$ = this._totalElements.asObservable();
  readonly loading$       = this._loading.asObservable();
  readonly selected$      = this._selected.asObservable();

  // ── Mutations d'état ──────────────────────────────────────────────────────

  /** Remplace la liste complète (résultat d'une recherche paginée). */
  set(data: CautionResponseDto[]): void {
    this._cautions.next(data);
  }

  /** Met à jour le total d'éléments (pour le paginator). */
  setTotal(total: number): void {
    this._totalElements.next(total);
  }

  /** Active / désactive l'indicateur de chargement. */
  setLoading(loading: boolean): void {
    this._loading.next(loading);
  }

  /** Sélectionne la caution courante (pour édition ou détail). */
  selectCaution(caution: CautionResponseDto | null): void {
    this._selected.next(caution);
  }

  /**
   * Mise à jour locale immutable d'une caution après toggle ou modification.
   * Évite un rechargement complet de la liste.
   */
  updateInList(updated: CautionResponseDto): void {
    this._cautions.next(
      this._cautions.getValue().map(c => c.id === updated.id ? updated : c)
    );
  }

  /** Suppression locale d'une caution après hard-delete confirmé. */
  removeFromList(id: number): void {
    this._cautions.next(this._cautions.getValue().filter(c => c.id !== id));
  }

  /** Réinitialise l'état complet. */
  clear(): void {
    this._cautions.next([]);
    this._totalElements.next(0);
    this._selected.next(null);
  }

  // ── Requêtes HTTP ─────────────────────────────────────────────────────────

  /**
   * Recherche paginée avec filtres dynamiques.
   * Seuls les critères non-null/non-undefined sont envoyés en query params.
   */
  rechercher(criteres: CautionSearchDto): Observable<PageResponse<CautionResponseDto>> {
    const params = this.buildParams(criteres);
    return this.http.get<PageResponse<CautionResponseDto>>(this.apiUrl, { params });
  }

  getById(id: number): Observable<CautionResponseDto> {
    return this.http.get<CautionResponseDto>(`${this.apiUrl}/${id}`);
  }

  creer(request: CautionRequestDto): Observable<CautionResponseDto> {
    return this.http.post<CautionResponseDto>(this.apiUrl, request);
  }

  modifier(id: number, request: CautionRequestDto): Observable<CautionResponseDto> {
    return this.http.put<CautionResponseDto>(`${this.apiUrl}/${id}`, request);
  }

  /**
   * Clôture une caution active (main levée).
   * Génère la commission de clôture selon la stratégie de la banque,
   * puis marque la caution inactive avec sa date de main levée.
   */
  cloturer(id: number, request: CloturerCautionRequest): Observable<CloturerCautionResponse> {
    return this.http.post<CloturerCautionResponse>(`${this.apiUrl}/${id}/cloturer`, request);
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

    if (criteres.numeroCaution)     params = params.set('numeroCaution', criteres.numeroCaution);
    if (criteres.organisme)         params = params.set('organisme',     criteres.organisme);
    if (criteres.banqueId  != null) params = params.set('banqueId',      criteres.banqueId.toString());
    if (criteres.natureId  != null) params = params.set('natureId',      criteres.natureId.toString());
    if (criteres.marcheId  != null) params = params.set('marcheId',      criteres.marcheId.toString());
    if (criteres.dateDePrise)       params = params.set('dateDePrise',   criteres.dateDePrise);
    if (criteres.montant   != null) params = params.set('montant',       criteres.montant.toString());
    if (criteres.actif     != null) params = params.set('actif',         criteres.actif.toString());

    params = params.set('page', (criteres.page ?? 0).toString());
    params = params.set('size', (criteres.size ?? 10).toString());
    params = params.set('sort', criteres.sort  ?? 'dateDePrise,desc');

    return params;
  }
}
