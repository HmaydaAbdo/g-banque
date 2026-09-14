import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../../../environments/environment';
import {VirementSearchDto} from "../dtos/VirementSearchDto";
import {PageResponse} from "../../global/types/PageResponse";
import {VirementResponseDto} from "../dtos/VirementResponseDto";
import {EncoursVirementResponseDto} from "../dtos/EncoursVirementResponseDto";
import {EncoursVirementsSearchDto} from "../dtos/EncoursVirementsSearchDto";
import {VirementRequestDto} from "../dtos/VirementRequestDto";
import {toLocalDateString} from "../../global/types/date.utils";

@Injectable({
  providedIn: 'root'
})

export class VirementService {

  private apiUrl = `${environment.apiUrl}virements`;

  constructor(private http: HttpClient) {
  }


  createVirement(requestDto: VirementRequestDto): Observable<VirementResponseDto> {
    return this.http.post<VirementResponseDto>(this.apiUrl, requestDto);
  }

  updateVirement(id: number, requestDto: VirementRequestDto): Observable<VirementResponseDto> {
    return this.http.put<VirementResponseDto>(`${this.apiUrl}/${id}`, requestDto);
  }

  getVirementById(id:number): Observable<VirementResponseDto> {
    return this.http.get<VirementResponseDto>(`${this.apiUrl}/${id}`);
  }
  deleteVirement(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getDecaissementVirements(
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
    if (searchDto.statut !== undefined && searchDto.statut !== null) {
      params = params.set('statut', searchDto.statut);
    }
    if (searchDto.type) {
      params = params.set('type', searchDto.type);
    }
    if (searchDto.emetteur) {
      params = params.set('emetteur', searchDto.emetteur);
    }
    if (searchDto.montant !== undefined && searchDto.montant !== null) {
      params = params.set('montant', searchDto.montant.toString());
    }
    params = this.appendDateRangeParams(params, searchDto);

    return this.http.get<PageResponse<VirementResponseDto>>(`${this.apiUrl}/decaissements`, {params});
  }



  getEncaissementVirements(
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
    if (searchDto.statut !== undefined && searchDto.statut !== null) {
      params = params.set('statut', searchDto.statut);
    }
    if (searchDto.type) {
      params = params.set('type', searchDto.type);
    }
    if (searchDto.emetteur) {
      params = params.set('emetteur', searchDto.emetteur);
    }
    if (searchDto.montant !== undefined && searchDto.montant !== null) {
      params = params.set('montant', searchDto.montant.toString());
    }
    params = this.appendDateRangeParams(params, searchDto);

    return this.http.get<PageResponse<VirementResponseDto>>(`${this.apiUrl}/encaissements`, {params});
  }

  /** Ajoute les 3 plages de dates (émission, opération, valeur) si renseignées. */
  private appendDateRangeParams(params: HttpParams, criteria: VirementSearchDto): HttpParams {
    if (criteria.dateEmissionFrom) params = params.set('dateEmissionFrom', this.toIsoDate(criteria.dateEmissionFrom));
    if (criteria.dateEmissionTo) params = params.set('dateEmissionTo', this.toIsoDate(criteria.dateEmissionTo));
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


  toggleVirementStatus(id: number): Observable<VirementResponseDto> {
    const url = `${this.apiUrl}/toggle/${id}`;
    return this.http.patch<VirementResponseDto>(url, {});
  }


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

    if (searchDto.dateEmission) {
      params = params.set('dateEmission', toLocalDateString(searchDto.dateEmission)!);
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

    if (searchDto.dateEmission) {
      params = params.set('dateEmission', toLocalDateString(searchDto.dateEmission)!);
    }
    if (searchDto.emetteur){
      params = params.set('emetteur', searchDto.emetteur);
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

  private buildSearchParams(searchDto: VirementSearchDto): HttpParams {
    let params = new HttpParams();
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
    if (searchDto.statut !== undefined && searchDto.statut !== null) {
      params = params.set('statut', searchDto.statut);
    }
    if (searchDto.type) {
      params = params.set('type', searchDto.type);
    }
    if (searchDto.emetteur) {
      params = params.set('emetteur', searchDto.emetteur);
    }
    if (searchDto.montant !== undefined && searchDto.montant !== null) {
      params = params.set('montant', searchDto.montant.toString());
    }
    params = this.appendDateRangeParams(params, searchDto);
    return params;
  }

  exportEncaissementsPdf(searchDto: VirementSearchDto): Observable<Blob> {
    const params = this.buildSearchParams(searchDto);
    return this.http.get(`${this.apiUrl}/encaissements/export/pdf`, {params, responseType: 'blob'});
  }

  exportDecaissementsPdf(searchDto: VirementSearchDto): Observable<Blob> {
    const params = this.buildSearchParams(searchDto);
    return this.http.get(`${this.apiUrl}/decaissements/export/pdf`, {params, responseType: 'blob'});
  }

  exportEncoursEncaissementPdf(banqueId: number, searchDto: EncoursVirementsSearchDto): Observable<Blob> {
    let params = new HttpParams();
    if (searchDto.dateEmission) {
      params = params.set('dateEmission', toLocalDateString(searchDto.dateEmission)!);
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
    const url = `${this.apiUrl}/encaissement/encours/${banqueId}/export/pdf`;
    return this.http.get(url, {params, responseType: 'blob'});
  }

  exportEncoursDecaissementPdf(banqueId: number, searchDto: EncoursVirementsSearchDto): Observable<Blob> {
    let params = new HttpParams();
    if (searchDto.dateEmission) {
      params = params.set('dateEmission', toLocalDateString(searchDto.dateEmission)!);
    }
    if (searchDto.beneficiaire) {
      params = params.set('beneficiaire', searchDto.beneficiaire);
    }
    if (searchDto.montant) {
      params = params.set('montant', searchDto.montant.toString());
    }
    const url = `${this.apiUrl}/decaissement/encours/${banqueId}/export/pdf`;
    return this.http.get(url, {params, responseType: 'blob'});
  }
}
