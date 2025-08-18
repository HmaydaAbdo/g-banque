import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { OrganismeLeasingResponse } from "../dtos/OrganismeLeasingResponse";
import { OrganismeLeasingRequest } from "../dtos/OrganismeLeasingRequest";
import { OrganismeLeasingSearch } from "../dtos/OrganismeLeasingSearch";
import { environment } from "../../../../../environments/environment";
import { PageResponse } from "../../../global/types/PageResponse";


@Injectable({
  providedIn: 'root'
})
export class OrganismeLeasingService {

  private apiUrl = `${environment.apiUrl}0rganismesLeasing`;

  constructor(private http: HttpClient) { }

  /**
   * Centralized error handling for all API calls made by this service.
   * This method intercepts HTTP errors, logs them, and transforms them
   * into a more user-friendly error message to be handled by the component.
   * @param error The HttpErrorResponse object.
   * @returns An Observable that emits an error, effectively stopping the stream.
   */
  private handleError(error: HttpErrorResponse): Observable<never> {
    let userFriendlyMessage = 'Une erreur inattendue est survenue. Veuillez réessayer plus tard.';
    console.error('Detailed API Error:', error); // Log the full error for internal debugging

    if (error.error instanceof ErrorEvent) {
      // A client-side or network error occurred. Handle it accordingly.
      userFriendlyMessage = `Erreur réseau: ${error.error.message}. Vérifiez votre connexion.`;
    } else {
      // The backend returned an unsuccessful response code.
      // The response body may contain clues as to what went wrong.
      const backendError = error.error;

      if (backendError && backendError.message) {
        // Use the message provided by the backend
        userFriendlyMessage = backendError.message;
      } else if (typeof error.error === 'string' && error.error.trim() !== '') {
        // Sometimes the backend sends a simple string error
        userFriendlyMessage = error.error;
      } else {
        // Fallback for unexpected backend error structures
        userFriendlyMessage = `Erreur serveur (${error.status}): ${error.statusText || 'Réponse inattendue du serveur.'}`;
      }
    }

    // Re-throw the error with the user-friendly message.
    // The consuming component will catch this error via its 'error' callback.
    return throwError(() => new Error(userFriendlyMessage));
  }

  /**
   * Fetches all OrganismeLeasings with pagination and optional search criteria.
   * Corresponds to GET /0organismesLeasing
   * @param searchCriteria Optional search object (nom).
   * @param page The page number (0-indexed). Defaults to 0.
   * @param size The number of items per page. Defaults to 10.
   * @param sort Optional sort string (e.g., "nom,asc", "dateCreation,desc").
   * @returns An Observable of PageResponse<OrganismeLeasingResponse>.
   */
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


    return this.http.get<PageResponse<OrganismeLeasingResponse>>(this.apiUrl, { params }).pipe(
      catchError(this.handleError.bind(this))
    );
  }

  /**
   * Fetches a single OrganismeLeasing by its ID.
   * Corresponds to GET /0organismesLeasing/{id}
   * @param id The ID of the OrganismeLeasing.
   * @returns An Observable of OrganismeLeasingResponse.
   */
  getOrganismeLeasingById(id: number): Observable<OrganismeLeasingResponse> {
    return this.http.get<OrganismeLeasingResponse>(`${this.apiUrl}/${id}`).pipe(
      catchError(this.handleError.bind(this))
    );
  }

  /**
   * Creates a new OrganismeLeasing.
   * Corresponds to POST /0organismesLeasing
   * @param data The OrganismeLeasingRequest DTO containing the new OrganismeLeasing data.
   * @returns An Observable of the created OrganismeLeasingResponse.
   */
  createOrganismeLeasing(data: OrganismeLeasingRequest): Observable<OrganismeLeasingResponse> {
    return this.http.post<OrganismeLeasingResponse>(this.apiUrl, data).pipe(
      catchError(this.handleError.bind(this))
    );
  }

  /**
   * Updates an existing OrganismeLeasing.
   * Corresponds to PUT /0organismesLeasing/{id}
   * @param id The ID of the OrganismeLeasing to update.
   * @param data The OrganismeLeasingRequest DTO with updated information.
   * @returns An Observable of the updated OrganismeLeasingResponse.
   */
  updateOrganismeLeasing(id: number, data: OrganismeLeasingRequest): Observable<OrganismeLeasingResponse> {
    return this.http.put<OrganismeLeasingResponse>(`${this.apiUrl}/${id}`, data).pipe(
      catchError(this.handleError.bind(this))
    );
  }

  /**
   * Deletes an OrganismeLeasing by its ID.
   * Corresponds to DELETE /0organismesLeasing/{id}
   * @param id The ID of the OrganismeLeasing to delete.
   * @returns An Observable of void (no content).
   */
  deleteOrganismeLeasing(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      catchError(this.handleError.bind(this))
    );
  }
}
