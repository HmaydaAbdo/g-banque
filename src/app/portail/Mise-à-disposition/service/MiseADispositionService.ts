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


  getDecaissementsForMiseADisposition(
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

    return this.http.get<PageResponse<MiseADispositionResponseDto>>(this.apiUrl+"/decaissements", {params});
  }


  getEncaissementsForMiseADisposition(
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

    return this.http.get<PageResponse<MiseADispositionResponseDto>>(this.apiUrl+"/encaissements", {params});
  }


  toggleMiseADispositionStatus(id: number): Observable<MiseADispositionResponseDto> {
    const url = `${this.apiUrl}/toggle/${id}`;
    return this.http.patch<MiseADispositionResponseDto>(url, {});
  }



  getEncoursDecaissementsForMiseADispositionByBank(
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

    const url = `${this.apiUrl}/encours/decaissements/${banqueId}`;
    return this.http.get<EncoursMiseAdisposition>(url, {params});
  }

  getEncoursEncaissementsForMiseADispositionByBank(
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

    const url = `${this.apiUrl}/encours/encaissements/${banqueId}`;
    return this.http.get<EncoursMiseAdisposition>(url, {params});
  }
}
