
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import {environment} from "../../../../environments/environment";
import {EcheanceCMTResponseDto} from "../dtos/EcheanceCMTResponseDto";
import {TableauAmortissementCMTResponseDto} from "../dtos/TableauAmortissementCMTResponseDto";
import {EcheanceCMTSearchDto} from "../dtos/EcheanceCMTSearchDto";
import {Decimal} from "decimal.js";
import {EncoursCMTRequestDTO} from "../../etatsDesEncours/views/lesEncours/decaissement/cmt/dtos/EncoursCMTRequestDTO";
import {EncoursCMTResponseDTO} from "../../etatsDesEncours/views/lesEncours/decaissement/cmt/dtos/EncoursCMTResponseDTO";


@Injectable({
  providedIn: 'root'
})
export class EcheanceCmtService {

  private apiUrl = `${environment.apiUrl}echeances-cmt`;

  constructor(private http: HttpClient) { }

  /**
   * Helper to convert plain numbers/strings to BigDecimal objects.
   */
  private convertToBigDecimal(value: any): Decimal {
    return value !== undefined && value !== null ? new Decimal(value) : new Decimal(0);
  }

  /**
   * Processes a single EcheanceCMTResponseDto to convert numeric fields.
   */
  private processEcheanceResponse(echeance: EcheanceCMTResponseDto): EcheanceCMTResponseDto {
    return {
      ...echeance,
      montant: this.convertToBigDecimal(echeance.montant),
    };
  }

  /**
   * Processes the full amortization table response.
   */
  private processTableauAmortissementResponse(tableau: TableauAmortissementCMTResponseDto): TableauAmortissementCMTResponseDto {
    return {
      ...tableau,
      montantTotalAPayer: this.convertToBigDecimal(tableau.montantTotalAPayer),
      montantTotalDejaPaye: this.convertToBigDecimal(tableau.montantTotalDejaPaye),
      montantRestantAPayer: this.convertToBigDecimal(tableau.montantRestantAPayer),
      echeances: {
        ...tableau.echeances,
        content: tableau.echeances.content.map(e => this.processEcheanceResponse(e))
      }
    };
  }

  /**
   * Handles HTTP errors centrally.
   */
  private handleError(error: HttpErrorResponse): Observable<never> {
    // Same error handling logic as the CmtService
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
   * Fetches the amortization table for a specific credit.
   */
  getTableauAmortissementForCredit(
    creditId: number,
    searchDto: EcheanceCMTSearchDto,
    pageable: { page?: number, size?: number, sort?: string } = {}
  ): Observable<TableauAmortissementCMTResponseDto> {
    let params = new HttpParams();

    if (searchDto.statutPaiement) {
      params = params.set('statutPaiement', searchDto.statutPaiement);
    }

    if (searchDto.year) {
      params = params.set('year', searchDto.year.toString());
    }

    if (searchDto.month) {
      params = params.set('month', searchDto.month.toString());
    }

    if (searchDto.numeroEcheance){
      params=params.set('numeroEcheance', searchDto.numeroEcheance.toString());
    }

    params = params.set('page', pageable.page?.toString() || '0');
    params = params.set('size', pageable.size?.toString() || '10');
    params = params.set('sort', pageable.sort || 'id,asc');

    return this.http.get<TableauAmortissementCMTResponseDto>(`${this.apiUrl}/tableau-amortissement/${creditId}`, { params }).pipe(
      map(response => this.processTableauAmortissementResponse(response)),
      catchError(this.handleError)
    );
  }

  /**
   * Marks a specific installment as paid.
   */
  markEcheanceAsPaid(id: number): Observable<EcheanceCMTResponseDto> {
    const url = `${this.apiUrl}/${id}/mark-as-paid`;
    return this.http.put<EcheanceCMTResponseDto>(url, {}).pipe(
      map(response => this.processEcheanceResponse(response)),
      catchError(this.handleError)
    );
  }

  /**
   * Récupère les encours du crédit moyen terme en fonction des critères de recherche.
   * @param request Le DTO de requête contenant les filtres (banqueId, periode, organismeLeasingId, etc.).
   * @returns Un Observable de l'objet de réponse des encours.
   */
  getEncoursCMT(request: EncoursCMTRequestDTO): Observable<EncoursCMTResponseDTO> {
    let params = new HttpParams();

    // Ajout des paramètres obligatoires
    params = params.append('banqueId', request.banqueId.toString());
    params = params.append('periode', request.periode);


    if (request.page !== undefined) {
      params = params.append('page', request.page.toString());
    }
    if (request.size !== undefined) {
      params = params.append('size', request.size.toString());
    }
    if (request.sort) {
      params = params.append('sort', request.sort);
    }

    if( request.designation ){
      params = params.append('designation', request.designation);
    }

    return this.http.get<EncoursCMTResponseDTO>(`${this.apiUrl}/encours`, { params }).pipe(
      catchError(this.handleError)
    );
  }
}
