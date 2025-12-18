import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { ContratLeasingRequestDTO, ContratLeasingResponseDTO } from "../dtos/contrat-leasing";
import {PageResponse} from "../../../global/types/PageResponse";
import {ContratLeasingSearchDTO} from "../dtos/contrat-leasing-search";
import {environment} from "../../../../../environments/environment";



@Injectable({
  providedIn: 'root'
})
export class ContratLeasingService {


  private apiUrl = `${environment.apiUrl}contratsLeasing`;

  constructor(private http: HttpClient) { }

  /**
   * Helper to convert plain numbers/strings to Decimal objects.
   * Handles null or undefined values gracefully.
   */


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
   * Retrieves a paginated list of active leasing contracts.
   * Calls the GET /contratsLeasing/actifs endpoint.
   */
  getActiveContrats(
    searchDTO: ContratLeasingSearchDTO,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ): Observable<PageResponse<ContratLeasingResponseDTO>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (searchDTO.numeroContrat) { params = params.set('numeroContrat', searchDTO.numeroContrat); }
    if (searchDTO.immobilisation) { params = params.set('immobilisation', searchDTO.immobilisation); }
    if (searchDTO.typeContrat) { params = params.set('typeContrat', searchDTO.typeContrat); }
    if (searchDTO.organismeLeasingId) { params = params.set('organismeLeasingId', searchDTO.organismeLeasingId.toString()); }
    if (searchDTO.banqueDePrelevementId) { params = params.set('banqueDePrelevementId', searchDTO.banqueDePrelevementId.toString()); }

    return this.http.get<PageResponse<ContratLeasingResponseDTO>>(`${this.apiUrl}/actifs`, { params }).pipe(
      map(pageResponse => ({
        ...pageResponse,
        content: pageResponse.content.map(contrat => this.processContratLeasingResponse(contrat))
      })),
      catchError(this.handleError)
    );
  }

  /**
   * Retrieves a paginated list of contracts pending lien release.
   * Calls the GET /contratsLeasing/en-attente-main-levee endpoint.
   */
  getPendingLienReleaseContrats(
    searchDTO: ContratLeasingSearchDTO,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ): Observable<PageResponse<ContratLeasingResponseDTO>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (searchDTO.numeroContrat) { params = params.set('numeroContrat', searchDTO.numeroContrat); }
    if (searchDTO.immobilisation) { params = params.set('immobilisation', searchDTO.immobilisation); }
    if (searchDTO.typeContrat) { params = params.set('typeContrat', searchDTO.typeContrat); }
    if (searchDTO.organismeLeasingId) { params = params.set('organismeLeasingId', searchDTO.organismeLeasingId.toString()); }
    if (searchDTO.banqueDePrelevementId) { params = params.set('banqueDePrelevementId', searchDTO.banqueDePrelevementId.toString()); }

    return this.http.get<PageResponse<ContratLeasingResponseDTO>>(`${this.apiUrl}/en-attente-main-levee`, { params }).pipe(
      map(pageResponse => ({
        ...pageResponse,
        content: pageResponse.content.map(contrat => this.processContratLeasingResponse(contrat))
      })),
      catchError(this.handleError)
    );
  }

  /**
   * Retrieves a paginated list of inactive leasing contracts.
   * Calls the GET /contratsLeasing/inactifs endpoint.
   */
  getInactiveContrats(
    searchDTO: ContratLeasingSearchDTO,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ): Observable<PageResponse<ContratLeasingResponseDTO>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (searchDTO.numeroContrat) { params = params.set('numeroContrat', searchDTO.numeroContrat); }
    if (searchDTO.immobilisation) { params = params.set('immobilisation', searchDTO.immobilisation); }
    if (searchDTO.typeContrat) { params = params.set('typeContrat', searchDTO.typeContrat); }
    if (searchDTO.organismeLeasingId) { params = params.set('organismeLeasingId', searchDTO.organismeLeasingId.toString()); }
    if (searchDTO.banqueDePrelevementId) { params = params.set('banqueDePrelevementId', searchDTO.banqueDePrelevementId.toString()); }

    return this.http.get<PageResponse<ContratLeasingResponseDTO>>(`${this.apiUrl}/inactifs`, { params }).pipe(
      map(pageResponse => ({
        ...pageResponse,
        content: pageResponse.content.map(contrat => this.processContratLeasingResponse(contrat))
      })),
      catchError(this.handleError)
    );
  }

  /**
   *  marquer un contrat comme inactif.
   * @param id L'ID du contrat à marquer comme inactif.
   * @returns Un Observable de ContratLeasingResponseDTO du contrat mis à jour.
   */
  markAsInactive(id: number): Observable<ContratLeasingResponseDTO> {
    return this.http.put<ContratLeasingResponseDTO>(`${this.apiUrl}/${id}/markAsInactive`, {}).pipe(
      map(response => this.processContratLeasingResponse(response)),
      catchError(this.handleError)
    );
  }
  getContratLeasingById(id: number): Observable<ContratLeasingResponseDTO> {
    return this.http.get<ContratLeasingResponseDTO>(`${this.apiUrl}/${id}`).pipe(
      map(response => this.processContratLeasingResponse(response)),
      catchError(this.handleError)
    );
  }


  /**
   * Crée un nouveau contrat de leasing.
   * @param contrat Le DTO de requête du contrat à créer.
   * @returns Un Observable de ContratLeasingResponseDTO.
   */
  createContratLeasing(contrat: ContratLeasingRequestDTO): Observable<ContratLeasingResponseDTO> {
    return this.http.post<ContratLeasingResponseDTO>(this.apiUrl, contrat).pipe(
      map(response => this.processContratLeasingResponse(response)),
      catchError(this.handleError)
    );
  }


  updateContratLeasing(id: number, contrat: ContratLeasingRequestDTO): Observable<ContratLeasingResponseDTO> {
    return this.http.put<ContratLeasingResponseDTO>(`${this.apiUrl}/${id}`, contrat).pipe(
      map(response => this.processContratLeasingResponse(response)),
      catchError(this.handleError)
    );
  }



  deleteContratLeasing(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Upload ou remplace un fichier PDF pour un contrat.
   * @param contratId L'ID du contrat
   * @param file Le fichier PDF à uploader
   * @returns Un Observable avec le message de succès
   */
  uploadPdfToContrat(contratId: number, file: File): Observable<string> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post(`${this.apiUrl}/${contratId}/pdf`, formData, {
      responseType: 'text'
    }).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Télécharge le fichier PDF d'un contrat.
   * @param contratId L'ID du contrat
   * @returns Un Observable avec le Blob du PDF
   */
  downloadPdfFromContrat(contratId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${contratId}/pdf`, {
      responseType: 'blob'
    }).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Méthode helper pour déclencher le téléchargement du PDF dans le navigateur.
   * @param contratId L'ID du contrat
   * @param fileName Nom du fichier (optionnel, par défaut "contrat.pdf")
   */
  downloadPdfFile(contratId: number, fileName: string = 'contrat.pdf'): void {
    this.downloadPdfFromContrat(contratId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = fileName;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: (error) => {
        console.error('Erreur lors du téléchargement du PDF:', error);
      }
    });
  }

  /**
   * Supprime le fichier PDF d'un contrat.
   * @param contratId L'ID du contrat
   * @returns Un Observable<void>
   */
  deletePdfFromContrat(contratId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${contratId}/pdf`).pipe(
      catchError(this.handleError)
    );
  }

}
