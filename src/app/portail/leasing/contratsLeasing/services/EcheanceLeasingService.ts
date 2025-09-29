import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from "../../../../../environments/environment";
import { EcheanceLeasingResponse } from "../dtos/EcheanceLeasingResponse";
import { TableauAmortissement } from "../dtos/table-amortissement";
import { EcheanceLeasingSearchDTO } from "../dtos/echeance-leasing-search";
import {EncoursLeasingRequestDTO} from "../../../etatsDesEncours/views/lesEncours/decaissement/leasing/dtos/encours-leasing-request.dto";
import {EncoursLeasingResponseDTO} from "../../../etatsDesEncours/views/lesEncours/decaissement/leasing/dtos/encours-leasing-response.dto";


@Injectable({
  providedIn: 'root'
})
export class EcheanceLeasingService {

  // Updated base URL as per your input: "echeances-leasing"
  private apiUrl = `${environment.apiUrl}echeances-leasing`;

  constructor(private http: HttpClient) { }

  /**
   * Handles HTTP errors centrally.
   */
  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'An unknown error occurred!';
    if (error.error instanceof ErrorEvent) {
      errorMessage = `Client-side Error: ${error.error.message}`;
    } else {
      if (error.error && error.error.message) {
        errorMessage = error.error.message;
      } else if (error.error && typeof error.error === 'string') {
        errorMessage = error.error;
      } else {
        errorMessage = `Server Error: ${error.status} ${error.statusText || ''}`;
      }
    }
    console.error('API Error:', errorMessage, error);
    return throwError(() => new Error(errorMessage));
  }

  /**
   * Fetches the amortization table for a given contract ID.
   * Supports pagination, sorting, and search filters for individual installments.
   *
   * @param contratId The ID of the leasing contract.
   * @param searchCriteria Optional search filters for the installments.
   * @param page Optional page number (0-indexed).
   * @param size Optional number of items per page.
   * @param sortBy Optional field to sort by (e.g., 'dateEcheance').
   * @param sortDirection Optional sort direction ('asc' or 'desc').
   * @returns An Observable of TableauAmortissement.
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

    // Add search criteria to HttpParams
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

    // Add pagination and sorting parameters
    params = params.set('page', page.toString());
    params = params.set('size', size.toString());
    if (sortBy && sortDirection) {
      params = params.set('sort', `${sortBy},${sortDirection}`);
    } else if (sortBy) {
      params = params.set('sort', sortBy);
    }


    const url = `${this.apiUrl}/tableau-amortissement/${contratId}`;
    return this.http.get<TableauAmortissement>(url, { params }).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Marks a specific EcheanceLeasing installment as paid.
   *
   * @param id The ID of the installment to mark as paid.
   * @returns An Observable of the updated EcheanceLeasingResponseDTO.
   */
  toggleStatus(id: number): Observable<EcheanceLeasingResponse> {
    const url = `${this.apiUrl}/${id}/toggle-status`;
    return this.http.put<EcheanceLeasingResponse>(url, {}).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Récupère les encours de leasing en fonction des critères de recherche.
   * @param request Le DTO de requête contenant les filtres (banqueId, periode, organismeLeasingId, etc.).
   * @returns Un Observable de l'objet de réponse des encours.
   */
  getEncoursLeasing(request: EncoursLeasingRequestDTO): Observable<EncoursLeasingResponseDTO> {
    let params = new HttpParams();

    // Ajout des paramètres obligatoires
    params = params.append('banqueId', request.banqueId.toString());
    params = params.append('periode', request.periode);

    // Ajout des paramètres optionnels

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

    if(request.numeroContrat){
      params = params.append('numeroContrat', request.numeroContrat.toString());
    }

    return this.http.get<EncoursLeasingResponseDTO>(`${this.apiUrl}/encours`, { params }).pipe(
      catchError(this.handleError)
    );
  }
}
