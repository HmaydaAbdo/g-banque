import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { CreditMoyenTermeResponseDto } from "../dtos/CreditMoyenTermeResponseDto";
import { CreditMoyenTermeSearchDto } from "../dtos/CreditMoyenTermeSearchDto";
import { PageResponse } from "../../global/types/PageResponse";
import { CreditMoyenTermeRequestDto } from "../dtos/CreditMoyenTermeRequestDto";

@Injectable({
  providedIn: 'root'
})
export class CmtService {

  private apiUrl = `${environment.apiUrl}credits-cmt`;

  constructor(private http: HttpClient) { }



  /**
   * Fetches a paginated list of ACTIVE credits.
   */
  searchActiveCredits(searchDto: CreditMoyenTermeSearchDto): Observable<PageResponse<CreditMoyenTermeResponseDto>> {
    const params = this.buildSearchParams(searchDto);
    return this.http.get<PageResponse<CreditMoyenTermeResponseDto>>(`${this.apiUrl}/actifs`, { params }).pipe(
      map(pageResponse => ({
        ...pageResponse,
        content: pageResponse.content.map(credit => this.processCmtResponse(credit))
      })),
      catchError(this.handleError)
    );
  }

  /**
   * Fetches a paginated list of credits PENDING CLOSURE.
   */
  searchPendingClosureCredits(searchDto: CreditMoyenTermeSearchDto): Observable<PageResponse<CreditMoyenTermeResponseDto>> {
    const params = this.buildSearchParams(searchDto);
    return this.http.get<PageResponse<CreditMoyenTermeResponseDto>>(`${this.apiUrl}/en-attente-de-main-levees`, { params }).pipe(
      map(pageResponse => ({
        ...pageResponse,
        content: pageResponse.content.map(credit => this.processCmtResponse(credit))
      })),
      catchError(this.handleError)
    );
  }

  /**
   * Fetches a paginated list of ARCHIVED credits.
   */
  searchArchivedCredits(searchDto: CreditMoyenTermeSearchDto): Observable<PageResponse<CreditMoyenTermeResponseDto>> {
    const params = this.buildSearchParams(searchDto);
    return this.http.get<PageResponse<CreditMoyenTermeResponseDto>>(`${this.apiUrl}/archives`, { params }).pipe(
      map(pageResponse => ({
        ...pageResponse,
        content: pageResponse.content.map(credit => this.processCmtResponse(credit))
      })),
      catchError(this.handleError)
    );
  }

  /**
   * Fetches a single credit by ID.
   */
  getCreditById(id: number): Observable<CreditMoyenTermeResponseDto> {
    return this.http.get<CreditMoyenTermeResponseDto>(`${this.apiUrl}/${id}`).pipe(
      map(response => this.processCmtResponse(response)),
      catchError(this.handleError)
    );
  }

  /**
   * Creates a new credit.
   */
  createCredit(request: CreditMoyenTermeRequestDto): Observable<CreditMoyenTermeResponseDto> {
    return this.http.post<CreditMoyenTermeResponseDto>(this.apiUrl, request).pipe(
      map(response => this.processCmtResponse(response)),
      catchError(this.handleError)
    );
  }

  /**
   * Updates an existing credit.
   */
  updateCredit(id: number, request: CreditMoyenTermeRequestDto): Observable<CreditMoyenTermeResponseDto> {
    return this.http.put<CreditMoyenTermeResponseDto>(`${this.apiUrl}/${id}`, request).pipe(
      map(response => this.processCmtResponse(response)),
      catchError(this.handleError)
    );
  }

  /**
   * Archives a credit.
   */
  archiveCredit(id: number): Observable<CreditMoyenTermeResponseDto> {
    return this.http.put<CreditMoyenTermeResponseDto>(`${this.apiUrl}/${id}/archiver`, {}).pipe(
      map(response => this.processCmtResponse(response)),
      catchError(this.handleError)
    );
  }

  /**
   * Deletes a credit by ID.
   */
  deleteCredit(id: number): Observable<{ msg: string }> {
    return this.http.delete<{ msg: string }>(`${this.apiUrl}/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Processes a single CreditMoyenTermeResponseDto to convert numeric fields.
   */
  private processCmtResponse(credit: CreditMoyenTermeResponseDto): CreditMoyenTermeResponseDto {
    return {
      ...credit,
      nominal: credit.nominal,
      echeance: credit.echeance,
      taux: credit.taux,
    };
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
   * Builds the HTTP parameters from the search DTO.
   * This centralizes the logic for pagination, sorting, and filters.
   */
  private buildSearchParams(searchDto: CreditMoyenTermeSearchDto): HttpParams {
    let params = new HttpParams();
    if (searchDto.designation) {
      params = params.set('designation', searchDto.designation);
    }
    if (searchDto.banqueId) {
      params = params.set('banqueId', searchDto.banqueId);
    }
    params = params.set('page', searchDto.page?.toString() || '0');
    params = params.set('size', searchDto.size?.toString() || '10');
    params = params.set('sort', searchDto.sort || 'id,asc');
    return params;
  }
}
