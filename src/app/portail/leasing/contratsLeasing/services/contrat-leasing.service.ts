import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ContratLeasingRequestDTO, ContratLeasingResponseDTO } from "../dtos/contrat-leasing";
import { PageResponse } from "../../../global/types/PageResponse";
import { ContratLeasingSearchDTO } from "../dtos/contrat-leasing-search";
import { environment } from "../../../../../environments/environment";



@Injectable({
  providedIn: 'root'
})
export class ContratLeasingService {

  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}contratsLeasing`;

  /**
   * Processes a single ContratLeasingResponseDTO to convert all numeric fields
   * that should be Decimal into Decimal objects.
   * This is crucial because HttpClient deserializes JSON numbers into JS numbers,
   * which can lose precision.
   */
  private processContratLeasingResponse(contrat: ContratLeasingResponseDTO): ContratLeasingResponseDTO {
    if (contrat.phases && Array.isArray(contrat.phases)) {
      contrat.phases = contrat.phases.map(phase => {
        return {
          ...phase,
          mensualiteTTC: phase.mensualiteTTC,
        };
      });
    }
    return contrat;
  }

  /**
   * Helper method to build HttpParams from search criteria and pagination settings.
   */
  private buildSearchParams(
    searchDTO: ContratLeasingSearchDTO,
    page: number,
    size: number,
    sort: string
  ): HttpParams {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (searchDTO.numeroContrat) {
      params = params.set('numeroContrat', searchDTO.numeroContrat);
    }
    if (searchDTO.immobilisation) {
      params = params.set('immobilisation', searchDTO.immobilisation);
    }
    if (searchDTO.typeContrat) {
      params = params.set('typeContrat', searchDTO.typeContrat);
    }
    if (searchDTO.organismeLeasingId) {
      params = params.set('organismeLeasingId', searchDTO.organismeLeasingId.toString());
    }
    if (searchDTO.banqueDePrelevementId) {
      params = params.set('banqueDePrelevementId', searchDTO.banqueDePrelevementId.toString());
    }
    if (searchDTO.montantEcheance !== undefined && searchDTO.montantEcheance !== null) {
      params = params.set('montantEcheance', searchDTO.montantEcheance.toString());
    }

    return params;
  }

  getActiveContrats(
    searchDTO: ContratLeasingSearchDTO,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ): Observable<PageResponse<ContratLeasingResponseDTO>> {
    const params = this.buildSearchParams(searchDTO, page, size, sort);
    return this.http.get<PageResponse<ContratLeasingResponseDTO>>(`${this.apiUrl}/actifs`, { params }).pipe(
      map(pageResponse => ({
        ...pageResponse,
        content: pageResponse.content.map(contrat => this.processContratLeasingResponse(contrat))
      }))
    );
  }

  getPendingLienReleaseContrats(
    searchDTO: ContratLeasingSearchDTO,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ): Observable<PageResponse<ContratLeasingResponseDTO>> {
    const params = this.buildSearchParams(searchDTO, page, size, sort);
    return this.http.get<PageResponse<ContratLeasingResponseDTO>>(`${this.apiUrl}/en-attente-main-levee`, { params }).pipe(
      map(pageResponse => ({
        ...pageResponse,
        content: pageResponse.content.map(contrat => this.processContratLeasingResponse(contrat))
      }))
    );
  }

  getInactiveContrats(
    searchDTO: ContratLeasingSearchDTO,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ): Observable<PageResponse<ContratLeasingResponseDTO>> {
    const params = this.buildSearchParams(searchDTO, page, size, sort);
    return this.http.get<PageResponse<ContratLeasingResponseDTO>>(`${this.apiUrl}/inactifs`, { params }).pipe(
      map(pageResponse => ({
        ...pageResponse,
        content: pageResponse.content.map(contrat => this.processContratLeasingResponse(contrat))
      }))
    );
  }

  markAsInactive(id: number): Observable<ContratLeasingResponseDTO> {
    return this.http.put<ContratLeasingResponseDTO>(`${this.apiUrl}/${id}/markAsInactive`, {}).pipe(
      map(response => this.processContratLeasingResponse(response))
    );
  }

  getContratLeasingById(id: number): Observable<ContratLeasingResponseDTO> {
    return this.http.get<ContratLeasingResponseDTO>(`${this.apiUrl}/${id}`).pipe(
      map(response => this.processContratLeasingResponse(response))
    );
  }

  createContratLeasing(contrat: ContratLeasingRequestDTO): Observable<ContratLeasingResponseDTO> {
    return this.http.post<ContratLeasingResponseDTO>(this.apiUrl, contrat).pipe(
      map(response => this.processContratLeasingResponse(response))
    );
  }

  updateContratLeasing(id: number, contrat: ContratLeasingRequestDTO): Observable<ContratLeasingResponseDTO> {
    return this.http.put<ContratLeasingResponseDTO>(`${this.apiUrl}/${id}`, contrat).pipe(
      map(response => this.processContratLeasingResponse(response))
    );
  }

  deleteContratLeasing(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
