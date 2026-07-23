import {Injectable} from '@angular/core';
import { toLocalDateString } from '../../global/types/date.utils';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../../../environments/environment';
import {MiseADispositionSearchDto} from "../dtos/MiseADispositionSearchDto";
import {PageResponse} from "../../global/types/PageResponse";
import {MiseADispositionResponseDto} from "../dtos/MiseADispositionResponseDto";
import {EncoursMiseAdisposition} from "../dtos/EncoursMiseAdisposition";
import {MiseAdispositionRequestDto} from "../dtos/MiseAdispositionRequestDto";


@Injectable({
  providedIn: 'root'
})
export class MiseADispositionService {

  private apiUrl = `${environment.apiUrl}misesADisposition`;

  constructor(private http: HttpClient) {
  }

  createMiseAdisposition(requestDto:MiseAdispositionRequestDto):Observable<MiseADispositionResponseDto>{
    return this.http.post<MiseADispositionResponseDto>(this.apiUrl,requestDto);
  }

  updateMiseAdisposition(id:number,requestDto:MiseAdispositionRequestDto):Observable<MiseADispositionResponseDto>{
    return this.http.put<MiseADispositionResponseDto>(`${this.apiUrl}/${id}`,requestDto);
  }

  getMiseAdispositionById(id:number):Observable<MiseADispositionResponseDto>{
    return this.http.get<MiseADispositionResponseDto>(`${this.apiUrl}/${id}`);
  }

  deleteMiseAdisposition(id:number):Observable<void>{
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
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
    if (searchDto.statut !== undefined && searchDto.statut !== null) {
      params = params.set('statut', searchDto.statut);
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
    if (searchDto.emetteur) {
      params = params.set('emetteur', searchDto.emetteur);
    }
    if (searchDto.banqueName) {
      params = params.set('banqueName', searchDto.banqueName);
    }
    if (searchDto.statut !== undefined && searchDto.statut !== null) {
      params = params.set('statut', searchDto.statut);
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

    if (searchDto.emetteur) {
      params = params.set('emetteur', searchDto.emetteur);
    }

    if(searchDto.dateEmission){
      params = params.set('dateEmission', toLocalDateString(searchDto.dateEmission)!);
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

  exportDecaissementsPdf(searchDto: MiseADispositionSearchDto): Observable<Blob> {
    let params = new HttpParams();
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
    if (searchDto.statut !== undefined && searchDto.statut !== null) {
      params = params.set('statut', searchDto.statut);
    }
    return this.http.get(`${this.apiUrl}/decaissements/export/pdf`, {params, responseType: 'blob'});
  }

  exportEncoursDecaissementPdf(banqueId: number, searchDto: MiseADispositionSearchDto): Observable<Blob> {
    let params = new HttpParams();
    if (searchDto.numero) {
      params = params.set('numero', searchDto.numero);
    }
    if (searchDto.beneficiaire) {
      params = params.set('beneficiaire', searchDto.beneficiaire);
    }
    if ((searchDto as any).montant) {
      params = params.set('montant', (searchDto as any).montant.toString());
    }
    return this.http.get(`${this.apiUrl}/encours/decaissements/${banqueId}/export/pdf`, {params, responseType: 'blob'});
  }

  exportEncoursEncaissementPdf(banqueId: number, searchDto: MiseADispositionSearchDto): Observable<Blob> {
    let params = new HttpParams();
    if (searchDto.numero) {
      params = params.set('numero', searchDto.numero);
    }
    if (searchDto.beneficiaire) {
      params = params.set('beneficiaire', searchDto.beneficiaire);
    }
    if ((searchDto as any).montant) {
      params = params.set('montant', (searchDto as any).montant.toString());
    }
    return this.http.get(`${this.apiUrl}/encours/encaissements/${banqueId}/export/pdf`, {params, responseType: 'blob'});
  }

}
