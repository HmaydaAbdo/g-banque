import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ChargeFixeConfigRequest } from '../dtos/ChargeFixeConfigRequest';
import { ChargeFixeConfigResponse } from '../dtos/ChargeFixeConfigResponse';
import { TypeChargeFixe } from '../dtos/TypeChargeFixe';
import { PageResponse } from '../../../global/types/PageResponse';
import { environment } from '../../../../../environments/environment';

export interface ChargeFixeConfigSearchCriteria {
  banqueId?: number | null;
  type?: TypeChargeFixe | null;
  search?: string | null;
}

@Injectable({ providedIn: 'root' })
export class ChargeFixeConfigService {
  private http   = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}charge-fixe-config`;

  /**
   * Liste paginée, triable et filtrable des charges fixes.
   * @param criteria Filtres optionnels (banque, type, recherche libre).
   * @param page Numéro de page (0-indexed).
   * @param size Taille de page.
   * @param sort Chaîne de tri (ex: "banque.nom,asc").
   */
  findAll(
    criteria: ChargeFixeConfigSearchCriteria = {},
    page: number = 0,
    size: number = 15,
    sort: string = 'banque.nom,asc'
  ): Observable<PageResponse<ChargeFixeConfigResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (criteria.banqueId) params = params.set('banqueId', criteria.banqueId.toString());
    if (criteria.type)     params = params.set('type', criteria.type);
    if (criteria.search)   params = params.set('search', criteria.search);

    return this.http.get<PageResponse<ChargeFixeConfigResponse>>(this.apiUrl, { params });
  }

  create(dto: ChargeFixeConfigRequest): Observable<ChargeFixeConfigResponse> {
    return this.http.post<ChargeFixeConfigResponse>(this.apiUrl, dto);
  }

  update(id: number, dto: ChargeFixeConfigRequest): Observable<ChargeFixeConfigResponse> {
    return this.http.put<ChargeFixeConfigResponse>(`${this.apiUrl}/${id}`, dto);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
