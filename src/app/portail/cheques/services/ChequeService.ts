import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../../../environments/environment';
import {ChequeSearchDto} from "../dtos/ChequeSearchDto";
import {PageResponse} from "../../global/types/PageResponse";
import {ChequeResponseDto} from "../dtos/ChequeResponseDto";


@Injectable({
  providedIn: 'root'
})
export class ChequeService {

  private apiUrl = `${environment.apiUrl}cheques`;

  constructor(private http: HttpClient) {
  }

  /**
   * Searches for cheques with pagination and filtering.
   *
   * @param searchDto The DTO containing search criteria.
   * @param page The page number to retrieve (0-indexed).
   * @param size The number of items per page.
   * @param sort The sorting criteria (e.g., 'dateEmission,desc').
   * @returns An Observable of a paginated list of ChequeResponseDto.
   */
  searchCheques(
    searchDto: ChequeSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<PageResponse<ChequeResponseDto>> {

    // Initialize HttpParams with pagination and sorting info
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    // Append search criteria if they are present
    if (searchDto.numero) {
      params = params.set('numero', searchDto.numero);
    }
    if (searchDto.beneficiaire) {
      params = params.set('beneficiaire', searchDto.beneficiaire);
    }
    if (searchDto.nomBanque) {
      params = params.set('nomBanque', searchDto.nomBanque);
    }
    if (searchDto.active !== undefined && searchDto.active !== null) {
      params = params.set('active', searchDto.active.toString());
    }

    return this.http.get<PageResponse<ChequeResponseDto>>(this.apiUrl, {params});
  }

  /**
   * Toggles the active status of a cheque by its unique numero.
   *
   * @param numero The unique identifier of the cheque to toggle.
   * @returns An Observable of the updated ChequeResponseDto.
   */
  toggleChequeStatus(numero: string): Observable<ChequeResponseDto> {
    const url = `${this.apiUrl}/toggle/${numero}`;
    return this.http.patch<ChequeResponseDto>(url, {});
  }

  /**
   * Retrieves a paginated list of outstanding cheques for a specific bank.
   *
   * @param banqueId The ID of the bank to search for.
   * @param page The page number to retrieve (0-indexed).
   * @param size The number of items per page.
   * @param sort The sorting criteria (e.g., 'dateEmission,desc').
   * @returns An Observable of a paginated list of ChequeResponseDto.
   */
  getOutstandingChequesByBank(
    banqueId: number,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<PageResponse<ChequeResponseDto>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    const url = `${this.apiUrl}/encours/${banqueId}`;
    return this.http.get<PageResponse<ChequeResponseDto>>(url, {params});
  }
}
