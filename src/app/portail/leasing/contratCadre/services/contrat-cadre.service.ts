import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ContratCadreRequest } from '../dtos/ContratCadreRequest';
import { ContratCadreResponse } from '../dtos/ContratCadreResponse';
import { ContratCadreSearch } from '../dtos/ContratCadreSearch';
import { ContratCadreUpdate } from '../dtos/ContratCadreUpdate';
import { environment } from '../../../../../environments/environment';
import { PageResponse } from '../../../global/types/PageResponse';

@Injectable({ providedIn: 'root' })
export class ContratCadreService {

  private readonly http   = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}contrats-cadres`;

  search(
    criteria: ContratCadreSearch = {},
    page = 0,
    size = 10,
    sort?: string
  ): Observable<PageResponse<ContratCadreResponse>> {
    let params = new HttpParams()
      .append('page', page.toString())
      .append('size', size.toString());

    if (sort)                    params = params.append('sort', sort);
    if (criteria.organismeId)    params = params.append('organismeId', criteria.organismeId.toString());
    if (criteria.statut)         params = params.append('statut', criteria.statut);

    return this.http.get<PageResponse<ContratCadreResponse>>(this.apiUrl, { params });
  }

  /**
   * Exporte en PDF l'intégralité des contrats cadres correspondant aux filtres fournis
   * (mêmes critères que {@link search}, sans pagination).
   */
  exportPdf(criteria: ContratCadreSearch = {}): Observable<Blob> {
    let params = new HttpParams();
    if (criteria.organismeId) params = params.append('organismeId', criteria.organismeId.toString());
    if (criteria.statut)      params = params.append('statut', criteria.statut);
    return this.http.get(`${this.apiUrl}/export/pdf`, { params, responseType: 'blob' });
  }

  create(request: ContratCadreRequest): Observable<ContratCadreResponse> {
    return this.http.post<ContratCadreResponse>(this.apiUrl, request);
  }

  getById(id: number): Observable<ContratCadreResponse> {
    return this.http.get<ContratCadreResponse>(`${this.apiUrl}/${id}`);
  }

  findByOrganisme(organismeId: number): Observable<ContratCadreResponse[]> {
    return this.http.get<ContratCadreResponse[]>(`${this.apiUrl}/organisme/${organismeId}`);
  }

  update(id: number, payload: ContratCadreUpdate): Observable<ContratCadreResponse> {
    return this.http.put<ContratCadreResponse>(`${this.apiUrl}/${id}`, payload);
  }

  archiver(id: number): Observable<ContratCadreResponse> {
    return this.http.patch<ContratCadreResponse>(`${this.apiUrl}/${id}/archiver`, {});
  }
}
