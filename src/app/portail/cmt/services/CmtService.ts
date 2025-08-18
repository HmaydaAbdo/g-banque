import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import {Decimal} from "decimal.js";
import {CreditMoyenTermeResponseDto} from "../dtos/CreditMoyenTermeResponseDto";
import {CreditMoyenTermeSearchDto} from "../dtos/CreditMoyenTermeSearchDto";
import {PageResponse} from "../../global/types/PageResponse";
import {CreditMoyenTermeRequestDto} from "../dtos/CreditMoyenTermeRequestDto";


@Injectable({
  providedIn: 'root'
})
export class CmtService {

  private apiUrl = `${environment.apiUrl}credits-cmt`;

  constructor(private http: HttpClient) { }

  /**
   * Helper to convert plain numbers/strings to BigDecimal objects.
   */
  private convertToBigDecimal(value: any): Decimal {
    return value !== undefined && value !== null ? new Decimal(value) : new Decimal(0);
  }

  /**
   * Processes a single CreditMoyenTermeResponseDto to convert numeric fields.
   */
  private processCmtResponse(credit: CreditMoyenTermeResponseDto): CreditMoyenTermeResponseDto {
    return {
      ...credit,
      nominal: this.convertToBigDecimal(credit.nominal),
      echeance: this.convertToBigDecimal(credit.echeance),
      taux: this.convertToBigDecimal(credit.taux),
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
   * Searches for credits with optional filters.
   */
  searchCredits(searchDto: CreditMoyenTermeSearchDto): Observable<PageResponse<CreditMoyenTermeResponseDto>> {
    let params = new HttpParams();
    if (searchDto.designation) {
      params = params.set('designation', searchDto.designation);
    }

    if(searchDto.banqueId){
      params = params.set('banqueId', searchDto.banqueId);
    }

    // ⭐ Correction : Vérifier si la valeur n'est pas undefined ou null avant de la définir
    if (searchDto.completed !== undefined && searchDto.completed !== null) {
      params = params.set('completed', searchDto.completed.toString());
    }

    params = params.set('page', searchDto.page?.toString() || '0');
    params = params.set('size', searchDto.size?.toString() || '10');
    params = params.set('sort', searchDto.sort || 'id,asc');

    return this.http.get<PageResponse<CreditMoyenTermeResponseDto>>(this.apiUrl, { params }).pipe(
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
   * Deletes a credit by ID.
   */
  deleteCredit(id: number): Observable<{ msg:string }> {
    return this.http.delete<{ msg:string }>(`${this.apiUrl}/${id}`).pipe(
      catchError(this.handleError)
    );
  }
}
