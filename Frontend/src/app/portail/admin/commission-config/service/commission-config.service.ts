import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CommissionConfigRequest } from '../dtos/CommissionConfigRequest';
import { CommissionConfigResponse } from '../dtos/CommissionConfigResponse';
import { CommissionCondition } from '../dtos/CommissionCondition';
import { TransactionSource } from '../../../transactions/dtos/TransactionSource';
import { PageResponse } from '../../../global/types/PageResponse';
import {environment} from "../../../../../environments/environment";

export interface CommissionConfigSearchCriteria {
  banqueId?: number | null;
  type?: TransactionSource | null;
  condition?: CommissionCondition | null;
  search?: string | null;
}

/** Bilan du rattrapage des commissions leasing sur les échéances prévues existantes. */
export interface RattrapageCommissionsLeasing {
  contratsTraites: number;
  contratsSansRegle: number;
  commissionsCreees: number;
  dejaPresentes: number;
}

@Injectable({ providedIn: 'root' })
export class CommissionConfigService {
  private http   = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}commission-config`;

  /**
   * Liste paginée, triable et filtrable des règles de commission.
   * @param criteria Filtres optionnels (banque, type, condition, recherche libre).
   * @param page Numéro de page (0-indexed).
   * @param size Taille de page.
   * @param sort Chaîne de tri (ex: "banque.nom,asc").
   */
  findAll(
    criteria: CommissionConfigSearchCriteria = {},
    page: number = 0,
    size: number = 15,
    sort: string = 'banque.nom,asc'
  ): Observable<PageResponse<CommissionConfigResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (criteria.banqueId)  params = params.set('banqueId', criteria.banqueId.toString());
    if (criteria.type)      params = params.set('type', criteria.type);
    if (criteria.condition) params = params.set('condition', criteria.condition);
    if (criteria.search)    params = params.set('search', criteria.search);

    return this.http.get<PageResponse<CommissionConfigResponse>>(this.apiUrl, { params });
  }

  create(dto: CommissionConfigRequest): Observable<CommissionConfigResponse> {
    return this.http.post<CommissionConfigResponse>(this.apiUrl, dto);
  }

  update(id: number, dto: CommissionConfigRequest): Observable<CommissionConfigResponse> {
    return this.http.put<CommissionConfigResponse>(`${this.apiUrl}/${id}`, dto);
  }

  /** Crée les commissions leasing manquantes des échéances prévues (les échéances payées ne sont pas touchées). */
  rattraperCommissionsLeasing(): Observable<RattrapageCommissionsLeasing> {
    return this.http.post<RattrapageCommissionsLeasing>(`${environment.apiUrl}echeances-leasing/commissions/rattrapage`, null);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
