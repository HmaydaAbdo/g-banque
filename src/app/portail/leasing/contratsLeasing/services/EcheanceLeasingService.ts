import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from "../../../../../environments/environment";
import { EcheanceLeasingResponse } from "../dtos/EcheanceLeasingResponse";
import { EcheanceLeasingUpdateRequest } from "../dtos/EcheanceLeasingUpdateRequest";
import { TableauAmortissement } from "../dtos/table-amortissement";
import { EcheanceLeasingSearchDTO } from "../dtos/echeance-leasing-search";
import { EncoursLeasingRequestDTO } from "../dtos/encours-leasing-request.dto";
import { EncoursLeasingResponseDTO } from "../dtos/encours-leasing-response.dto";


@Injectable({
  providedIn: 'root'
})
export class EcheanceLeasingService {

  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}echeances-leasing`;

  /**
   * Fetches the amortization table for a given contract ID.
   * Supports pagination, sorting, and search filters for individual installments.
   */
  getTableauAmortissementForContrat(
    contratId: number,
    searchCriteria?: EcheanceLeasingSearchDTO,
    page: number = 0,
    size: number = 10,
    sortBy?: string,
    sortDirection?: 'asc' | 'desc'
  ): Observable<TableauAmortissement> {
    let params = new HttpParams();

    if (searchCriteria) {
      if (searchCriteria.numeroEcheance != null) {
        params = params.set('numeroEcheance', searchCriteria.numeroEcheance.toString());
      }
      if (searchCriteria.month != null) {
        params = params.set('month', searchCriteria.month.toString());
      }
      if (searchCriteria.year != null) {
        params = params.set('year', searchCriteria.year.toString());
      }
      if (searchCriteria.statutPaiement != null) {
        params = params.set('statutPaiement', searchCriteria.statutPaiement);
      }
    }

    params = params.set('page', page.toString());
    params = params.set('size', size.toString());
    if (sortBy && sortDirection) {
      params = params.set('sort', `${sortBy},${sortDirection}`);
    } else if (sortBy) {
      params = params.set('sort', sortBy);
    }

    const url = `${this.apiUrl}/tableau-amortissement/${contratId}`;
    return this.http.get<TableauAmortissement>(url, { params });
  }

  /**
   * Exporte en PDF le tableau d'amortissement complet d'un contrat (mêmes critères que
   * {@link getTableauAmortissementForContrat}, sans pagination).
   */
  exportTableauAmortissementPdf(contratId: number, searchCriteria?: EcheanceLeasingSearchDTO): Observable<Blob> {
    let params = new HttpParams();

    if (searchCriteria) {
      if (searchCriteria.numeroEcheance != null) {
        params = params.set('numeroEcheance', searchCriteria.numeroEcheance.toString());
      }
      if (searchCriteria.month != null) {
        params = params.set('month', searchCriteria.month.toString());
      }
      if (searchCriteria.year != null) {
        params = params.set('year', searchCriteria.year.toString());
      }
      if (searchCriteria.statutPaiement != null) {
        params = params.set('statutPaiement', searchCriteria.statutPaiement);
      }
    }

    const url = `${this.apiUrl}/tableau-amortissement/${contratId}/export/pdf`;
    return this.http.get(url, { params, responseType: 'blob' });
  }

  /**
   * Marks a specific EcheanceLeasing installment as paid.
   */
  toggleStatus(id: number): Observable<EcheanceLeasingResponse> {
    const url = `${this.apiUrl}/${id}/toggle-status`;
    return this.http.put<EcheanceLeasingResponse>(url, {});
  }

  /**
   * Met à jour une échéance au statut PREVU (admin uniquement).
   * Bloqué côté backend si le statut est PAYE.
   */
  updateEcheance(id: number, dto: EcheanceLeasingUpdateRequest): Observable<EcheanceLeasingResponse> {
    return this.http.put<EcheanceLeasingResponse>(`${this.apiUrl}/${id}`, dto);
  }

  /**
   * Récupère les encours de leasing en fonction des critères de recherche.
   */
  getEncoursLeasing(request: EncoursLeasingRequestDTO): Observable<EncoursLeasingResponseDTO> {
    let params = new HttpParams();

    params = params.append('banqueId', request.banqueId.toString());

    if (request.page !== undefined) {
      params = params.append('page', request.page.toString());
    }
    if (request.size !== undefined) {
      params = params.append('size', request.size.toString());
    }
    if (request.sort) {
      params = params.append('sort', request.sort);
    }
    if (request.organismeLeasingId) {
      params = params.append('organismeLeasingId', request.organismeLeasingId.toString());
    }
    if (request.numeroContrat) {
      params = params.append('numeroContrat', request.numeroContrat.toString());
    }
    if (request.montant) {
      params = params.append('montant', request.montant.toString());
    }

    return this.http.get<EncoursLeasingResponseDTO>(`${this.apiUrl}/encours`, { params });
  }
}
