import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { OrganismeLeasingResponse } from "../dtos/OrganismeLeasingResponse";
import { OrganismeLeasingRequest } from "../dtos/OrganismeLeasingRequest";
import { OrganismeLeasingSearch } from "../dtos/OrganismeLeasingSearch";
import { environment } from "../../../../../environments/environment";
import { PageResponse } from "../../../global/types/PageResponse";


@Injectable({
  providedIn: 'root'
})
export class OrganismeLeasingService {

  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}OrganismesLeasing`;

  searchOrganismeLeasings(
    searchCriteria?: OrganismeLeasingSearch,
    page: number = 0,
    size: number = 10,
    sort?: string
  ): Observable<PageResponse<OrganismeLeasingResponse>> {
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());

    if (sort) {
      params = params.append('sort', sort);
    }
    if (searchCriteria?.nom) {
      params = params.append('nom', searchCriteria.nom);
    }

    return this.http.get<PageResponse<OrganismeLeasingResponse>>(this.apiUrl, { params });
  }

  getOrganismeLeasingById(id: number): Observable<OrganismeLeasingResponse> {
    return this.http.get<OrganismeLeasingResponse>(`${this.apiUrl}/${id}`);
  }

  createOrganismeLeasing(data: OrganismeLeasingRequest): Observable<OrganismeLeasingResponse> {
    return this.http.post<OrganismeLeasingResponse>(this.apiUrl, data);
  }

  updateOrganismeLeasing(id: number, data: OrganismeLeasingRequest): Observable<OrganismeLeasingResponse> {
    return this.http.put<OrganismeLeasingResponse>(`${this.apiUrl}/${id}`, data);
  }

  deleteOrganismeLeasing(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
