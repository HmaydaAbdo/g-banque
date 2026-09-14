import {Injectable} from '@angular/core';
import { toLocalDateString } from '../../global/types/date.utils';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../../../environments/environment';
import {ChequeSearchDto} from "../dtos/ChequeSearchDto";
import {PageResponse} from "../../global/types/PageResponse";
import {ChequeResponseDto} from "../dtos/ChequeResponseDto";
import {EncoursChequeResponseDto} from "../dtos/EncoursChequeResponseDto";
import {EncoursChequeSearchDto} from "../dtos/EncoursChequeSearchDto";
import {ChequeRequestDto} from "../dtos/ChequeRequestDto";


@Injectable({
  providedIn: 'root'
})
export class ChequeService {

  private apiUrl = `${environment.apiUrl}cheques`;

  constructor(private http: HttpClient) {
  }

  createCheque(requestDto:ChequeRequestDto):Observable<ChequeResponseDto>{
    return this.http.post<ChequeResponseDto>(this.apiUrl,requestDto);
  }

  updateCheque(id:number,requestDto:ChequeRequestDto):Observable<ChequeResponseDto>{
    return this.http.put<ChequeResponseDto>(`${this.apiUrl}/${id}`,requestDto);
  }

  getChequeById(id:number):Observable<ChequeResponseDto>{
    return this.http.get<ChequeResponseDto>(`${this.apiUrl}/${id}`);
  }

  deleteCheque(id:number):Observable<void>{
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }


  getDecaissementCheques(
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
    if (searchDto.statut !== undefined && searchDto.statut !== null) {
      params = params.set('statut', searchDto.statut);
    }
    params = this.appendDateRangeParams(params, searchDto);

    return this.http.get<PageResponse<ChequeResponseDto>>(this.apiUrl+"/decaissements", {params});
  }


  getEncaissementCheques(
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
    if(searchDto.numero){
      params = params.set('numero', searchDto.numero)
    }

    if (searchDto.montant) {
      params = params.set('montant', searchDto.montant);
    }

    if (searchDto.emetteur) {
      params = params.set('emetteur', searchDto.emetteur);
    }
    if (searchDto.nomBanque) {
      params = params.set('nomBanque', searchDto.nomBanque);
    }
    if (searchDto.statut !== undefined && searchDto.statut !== null) {
      params = params.set('statut', searchDto.statut);
    }
    params = this.appendDateRangeParams(params, searchDto);

    return this.http.get<PageResponse<ChequeResponseDto>>(this.apiUrl+"/encaissements", {params});
  }

  /** Ajoute les 4 plages de dates (émission, échéance, opération, valeur) si renseignées. */
  private appendDateRangeParams(params: HttpParams, criteria: ChequeSearchDto): HttpParams {
    if (criteria.dateEmissionFrom) params = params.set('dateEmissionFrom', this.toIsoDate(criteria.dateEmissionFrom));
    if (criteria.dateEmissionTo) params = params.set('dateEmissionTo', this.toIsoDate(criteria.dateEmissionTo));
    if (criteria.dateEcheanceFrom) params = params.set('dateEcheanceFrom', this.toIsoDate(criteria.dateEcheanceFrom));
    if (criteria.dateEcheanceTo) params = params.set('dateEcheanceTo', this.toIsoDate(criteria.dateEcheanceTo));
    if (criteria.dateOperationFrom) params = params.set('dateOperationFrom', this.toIsoDate(criteria.dateOperationFrom));
    if (criteria.dateOperationTo) params = params.set('dateOperationTo', this.toIsoDate(criteria.dateOperationTo));
    if (criteria.dateValeurFrom) params = params.set('dateValeurFrom', this.toIsoDate(criteria.dateValeurFrom));
    if (criteria.dateValeurTo) params = params.set('dateValeurTo', this.toIsoDate(criteria.dateValeurTo));
    return params;
  }

  /** Formate un objet Date en chaîne ISO yyyy-MM-dd pour Spring. */
  private toIsoDate(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }


  toggleChequeStatus(id: number): Observable<ChequeResponseDto> {
    const url = `${this.apiUrl}/toggle/${id}`;
    return this.http.patch<ChequeResponseDto>(url, {});
  }

  /**
   * Retrieves a paginated list of outstanding cheques for a specific bank.
   *
   * @param banqueId The ID of the bank to search for.
   * @param searchDto
   * @param page The page number to retrieve (0-indexed).
   * @param size The number of items per page.
   * @param sort The sorting criteria (e.g., 'dateEmission,desc').
   * @returns An Observable of a paginated list of ChequeResponseDto.
   */
  getEncoursDecaissementChequesByBank(
    banqueId: number,
    searchDto: EncoursChequeSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<EncoursChequeResponseDto> {

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

    const url = `${this.apiUrl}/decaissements/encours/${banqueId}`;
    return this.http.get<EncoursChequeResponseDto>(url, {params});
  }


  getEncoursEncaissementChequesByBank(
    banqueId: number,
    searchDto: EncoursChequeSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<EncoursChequeResponseDto> {

    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);



    if(searchDto.dateEmission){
      params = params.set('dateEmission', toLocalDateString(searchDto.dateEmission)!)
    }

    if(searchDto.numero){
      params = params.set('numero', searchDto.numero)
    }

    if (searchDto.emetteur) {
      params = params.set('emetteur', searchDto.emetteur);
    }
    if (searchDto.beneficiaire) {
      params = params.set('beneficiaire', searchDto.beneficiaire);
    }

    if (searchDto.montant) {
      params = params.set('montant', searchDto.montant.toString());
    }

    const url = `${this.apiUrl}/encaissements/encours/${banqueId}`;
    return this.http.get<EncoursChequeResponseDto>(url, {params});
  }

  exportEncaissementsPdf(searchDto: ChequeSearchDto): Observable<Blob> {
    let params = new HttpParams();
    if (searchDto.numero) {
      params = params.set('numero', searchDto.numero);
    }
    if (searchDto.montant) {
      params = params.set('montant', searchDto.montant);
    }
    if (searchDto.emetteur) {
      params = params.set('emetteur', searchDto.emetteur);
    }
    if (searchDto.nomBanque) {
      params = params.set('nomBanque', searchDto.nomBanque);
    }
    if (searchDto.statut !== undefined && searchDto.statut !== null) {
      params = params.set('statut', searchDto.statut);
    }
    params = this.appendDateRangeParams(params, searchDto);
    return this.http.get(`${this.apiUrl}/encaissements/export/pdf`, {params, responseType: 'blob'});
  }

  exportDecaissementsPdf(searchDto: ChequeSearchDto): Observable<Blob> {
    let params = new HttpParams();
    if (searchDto.numero) {
      params = params.set('numero', searchDto.numero);
    }
    if (searchDto.beneficiaire) {
      params = params.set('beneficiaire', searchDto.beneficiaire);
    }
    if (searchDto.nomBanque) {
      params = params.set('nomBanque', searchDto.nomBanque);
    }
    if (searchDto.statut !== undefined && searchDto.statut !== null) {
      params = params.set('statut', searchDto.statut);
    }
    params = this.appendDateRangeParams(params, searchDto);
    return this.http.get(`${this.apiUrl}/decaissements/export/pdf`, {params, responseType: 'blob'});
  }

  exportEncoursEncaissementPdf(banqueId: number, searchDto: EncoursChequeSearchDto): Observable<Blob> {
    let params = new HttpParams();
    if (searchDto.dateEmission) {
      params = params.set('dateEmission', toLocalDateString(searchDto.dateEmission)!);
    }
    if (searchDto.numero) {
      params = params.set('numero', searchDto.numero);
    }
    if (searchDto.emetteur) {
      params = params.set('emetteur', searchDto.emetteur);
    }
    if (searchDto.beneficiaire) {
      params = params.set('beneficiaire', searchDto.beneficiaire);
    }
    if (searchDto.montant) {
      params = params.set('montant', searchDto.montant.toString());
    }
    const url = `${this.apiUrl}/encaissements/encours/${banqueId}/export/pdf`;
    return this.http.get(url, {params, responseType: 'blob'});
  }

  exportEncoursDecaissementPdf(banqueId: number, searchDto: EncoursChequeSearchDto): Observable<Blob> {
    let params = new HttpParams();
    if (searchDto.numero) {
      params = params.set('numero', searchDto.numero);
    }
    if (searchDto.beneficiaire) {
      params = params.set('beneficiaire', searchDto.beneficiaire);
    }
    if (searchDto.montant) {
      params = params.set('montant', searchDto.montant.toString());
    }
    const url = `${this.apiUrl}/decaissements/encours/${banqueId}/export/pdf`;
    return this.http.get(url, {params, responseType: 'blob'});
  }
}
