import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CautionCommissionConfigRequest } from '../dtos/CautionCommissionConfigRequest';
import { CautionCommissionConfigResponse } from '../dtos/CautionCommissionConfigResponse';
import { PageResponse } from '../../../global/types/PageResponse';
import { environment } from '../../../../../environments/environment';

export interface CautionCommissionConfigSearchCriteria {
  banqueId?: number | null;
  search?: string | null;
}

@Injectable({ providedIn: 'root' })
export class CautionCommissionConfigService {
  private http   = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}cautions/commission-config`;

  /**
   * Liste paginée, triable et filtrable des configurations de commission caution.
   * @param criteria Filtres optionnels (banque, recherche libre).
   * @param page Numéro de page (0-indexed).
   * @param size Taille de page.
   * @param sort Chaîne de tri (ex: "banque.nom,asc").
   */
  findAll(
    criteria: CautionCommissionConfigSearchCriteria = {},
    page: number = 0,
    size: number = 15,
    sort: string = 'banque.nom,asc'
  ): Observable<PageResponse<CautionCommissionConfigResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (criteria.banqueId) params = params.set('banqueId', criteria.banqueId.toString());
    if (criteria.search)   params = params.set('search', criteria.search);

    return this.http.get<PageResponse<CautionCommissionConfigResponse>>(this.apiUrl, { params });
  }

  create(dto: CautionCommissionConfigRequest): Observable<CautionCommissionConfigResponse> {
    return this.http.post<CautionCommissionConfigResponse>(this.apiUrl, dto);
  }

  update(id: number, dto: CautionCommissionConfigRequest): Observable<CautionCommissionConfigResponse> {
    return this.http.put<CautionCommissionConfigResponse>(`${this.apiUrl}/${id}`, dto);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
