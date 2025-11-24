import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../../../environments/environment';
import {VirementSearchDto} from "../dtos/VirementSearchDto";
import {PageResponse} from "../../global/types/PageResponse";
import {VirementResponseDto} from "../dtos/VirementResponseDto";
import {EncoursVirementResponseDto} from "../dtos/EncoursVirementResponseDto";
import {EncoursVirementsSearchDto} from "../dtos/EncoursVirementsSearchDto";

@Injectable({
  providedIn: 'root'
})
export class VirementService {

  private apiUrl = `${environment.apiUrl}virements`;

  constructor(private http: HttpClient) {
  }

  /**
   * Searches for virements with pagination and filtering.
   *
   * @param searchDto The DTO containing search criteria.
   * @param page The page number to retrieve (0-indexed).
   * @param size The number of items per page.
   * @param sort The sorting criteria (e.g., 'dateEmission,desc').
   * @returns An Observable of a paginated list of VirementResponseDto.
   */
  searchVirements(
    searchDto: VirementSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<PageResponse<VirementResponseDto>> {

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
    if (searchDto.banqueBeneficiaire) {
      params = params.set('banqueBeneficiaire', searchDto.banqueBeneficiaire);
    }
    if (searchDto.nomBanqueEmettrice) {
      params = params.set('nomBanqueEmettrice', searchDto.nomBanqueEmettrice);
    }
    if (searchDto.ribBeneficiaire) {
      params = params.set('ribBeneficiaire', searchDto.ribBeneficiaire);
    }
    if (searchDto.active !== undefined && searchDto.active !== null) {
      params = params.set('active', searchDto.active.toString());
    }

    if (searchDto.type !== undefined && searchDto.type !== null) {
      params = params.set('type', searchDto.type.toString());
    }

    return this.http.get<PageResponse<VirementResponseDto>>(this.apiUrl, {params});
  }

  /**
   * Toggles the active status of a virement by its unique numero.
   *
   * @param numero The unique identifier of the virement to toggle.
   * @returns An Observable of the updated VirementResponseDto.
   */
  toggleVirementStatus(numero: string): Observable<VirementResponseDto> {
    const url = `${this.apiUrl}/toggle/${numero}`;
    return this.http.patch<VirementResponseDto>(url, {});
  }

  /**
   * Retrieves a paginated list of outstanding virements for a specific bank.
   *
   * @param banqueId The ID of the bank.
   * @param searchDto
   * @param page The page number to retrieve (0-indexed).
   * @param size The number of items per page.
   * @param sort The sorting criteria (e.g., 'dateEmission,desc').
   * @returns An Observable of a paginated list of VirementResponseDto.
   */
  getEncoursDecaissementVirementsByBank(
    banqueId: number,
    searchDto: EncoursVirementsSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<EncoursVirementResponseDto> {
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
    const url = `${this.apiUrl}/decaissement/encours/${banqueId}`;
    return this.http.get<EncoursVirementResponseDto>(url, {params});
  }


  getEncoursEncaissementVirementsByBank(
    banqueId: number,
    searchDto: EncoursVirementsSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<EncoursVirementResponseDto> {
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
    const url = `${this.apiUrl}/encaissement/encours/${banqueId}`;
    return this.http.get<EncoursVirementResponseDto>(url, {params});
  }
}
