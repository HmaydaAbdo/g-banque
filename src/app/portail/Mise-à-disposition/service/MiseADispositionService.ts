import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../../../environments/environment';
import {MiseADispositionSearchDto} from "../dtos/MiseADispositionSearchDto";
import {PageResponse} from "../../global/types/PageResponse";
import {MiseADispositionResponseDto} from "../dtos/MiseADispositionResponseDto";
import {EncoursMiseAdisposition} from "../dtos/EncoursMiseAdisposition";


@Injectable({
  providedIn: 'root'
})
export class MiseADispositionService {

  private apiUrl = `${environment.apiUrl}misesADisposition`;

  constructor(private http: HttpClient) {
  }

  /**
   * Searches for MiseADisposition entities with pagination and filtering.
   *
   * @param searchDto The DTO containing search criteria.
   * @param page The page number to retrieve (0-indexed).
   * @param size The number of items per page.
   * @param sort The sorting criteria (e.g., 'dateEmission,desc').
   * @returns An Observable of a paginated list of MiseADispositionResponseDto.
   */
  searchMisesADisposition(
    searchDto: MiseADispositionSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<PageResponse<MiseADispositionResponseDto>> {

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
    if (searchDto.cinBeneficiaire) {
      params = params.set('cinBeneficiaire', searchDto.cinBeneficiaire);
    }
    if (searchDto.banqueName) {
      params = params.set('banqueName', searchDto.banqueName);
    }
    if (searchDto.active !== undefined && searchDto.active !== null) {
      params = params.set('active', searchDto.active.toString());
    }

    return this.http.get<PageResponse<MiseADispositionResponseDto>>(this.apiUrl, {params});
  }

  /**
   * Toggles the active status of a MiseADisposition by its unique numero.
   *
   * @param numero The unique identifier of the MiseADisposition to toggle.
   * @returns An Observable of the updated MiseADispositionResponseDto.
   */
  toggleMiseADispositionStatus(numero: string): Observable<MiseADispositionResponseDto> {
    const url = `${this.apiUrl}/toggle/${numero}`;
    return this.http.patch<MiseADispositionResponseDto>(url, {});
  }


  /**
   * Retrieves a paginated list of outstanding MiseADisposition entities for a specific bank.
   *
   * @param banqueId The ID of the bank.
   * @param searchDto
   * @param page The page number to retrieve (0-indexed).
   * @param size The number of items per page.
   * @param sort The sorting criteria (e.g., 'dateEmission,desc').
   * @returns An Observable of a paginated list of MiseADispositionResponseDto.
   */
  getOutstandingMisesADispositionByBank(
    banqueId: number,
    searchDto: MiseADispositionSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<EncoursMiseAdisposition> {

    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (searchDto.numero) {
      params = params.set('numero', searchDto.numero);
    }
    if (searchDto.beneficiaire) {
        params = params.set('beneficiaire', searchDto.beneficiaire);
    }
    if (searchDto.montant) {
      params = params.set('montant', searchDto.montant.toString());
    }

    const url = `${this.apiUrl}/encours/${banqueId}`;
    return this.http.get<EncoursMiseAdisposition>(url, {params});
  }
}
