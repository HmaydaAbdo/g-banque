import {Injectable} from '@angular/core';
import { toLocalDateString } from '../../global/types/date.utils';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../../../environments/environment';
import {PageResponse} from "../../global/types/PageResponse";
import {LcnResponseDTO} from "../dtos/LcnResponseDTO";
import {LcnSearchDTO} from "../dtos/LcnSearchDTO";
import {EncoursLcnResponse} from "../dtos/EncoursLcnResponse";
import {LcnRequestDto} from "../dtos/LcnRequestDto";


@Injectable({
  providedIn: 'root'
})
export class LcnService {

  private apiUrl = `${environment.apiUrl}lcns`;

  constructor(private http: HttpClient) {
  }



  createLcn(requestDto: LcnRequestDto): Observable<LcnResponseDTO> {
    return this.http.post<LcnResponseDTO>(this.apiUrl, requestDto);
  }

  updateLcn(id: number, requestDto: LcnRequestDto): Observable<LcnResponseDTO> {
    return this.http.put<LcnResponseDTO>(`${this.apiUrl}/${id}`, requestDto);
  }

  getLcnById(id:number): Observable<LcnResponseDTO> {
    return this.http.get<LcnResponseDTO>(`${this.apiUrl}/${id}`);
  }

  deleteLcn(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getDecaissementLcns(
    searchDto: LcnSearchDTO,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<PageResponse<LcnResponseDTO>> {

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

    return this.http.get<PageResponse<LcnResponseDTO>>(this.apiUrl+"/decaissements", {params});
  }

  getEncaissementLcns(
    searchDto: LcnSearchDTO,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<PageResponse<LcnResponseDTO>> {

    // Initialize HttpParams with pagination and sorting info
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    // Append search criteria if they are present
    if (searchDto.dateEmission) {
      params = params.set('dateEmission', toLocalDateString(searchDto.dateEmission)!);
    }

    if(searchDto.numero){
      params = params.set('numero', searchDto.numero)
    }
    if (searchDto.dateEcheance) {
      params = params.set('dateEcheance', toLocalDateString(searchDto.dateEcheance)!);
    }
    if (searchDto.emetteur) {
      params = params.set('emetteur', searchDto.emetteur);
    }
    if (searchDto.montant) {
      params = params.set('montant', searchDto.montant.toString());
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

    return this.http.get<PageResponse<LcnResponseDTO>>(this.apiUrl+"/encaissements", {params});
  }


  toggleLcnStatus(id: number): Observable<LcnResponseDTO> {
    const url = `${this.apiUrl}/toggle/${id}`;
    return this.http.patch<LcnResponseDTO>(url, {});
  }


  encoursDecaissementLcnsByBank(
    banqueId: number,
    searchDto: LcnSearchDTO,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEcheance,desc'
  ): Observable<EncoursLcnResponse> {

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
    return this.http.get<EncoursLcnResponse>(url, {params});
  }

  encoursEncaissementLcnsByBank(
    banqueId: number,
    searchDto: LcnSearchDTO,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEcheance,desc'
  ): Observable<EncoursLcnResponse> {

    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (searchDto.emetteur) {
      params = params.set('emetteur', searchDto.emetteur.toString());
    }

    if (searchDto.numero) {
      params = params.set('numero', searchDto.numero);
    }
    if (searchDto.dateEcheance) {
      params = params.set('dateEcheance', toLocalDateString(searchDto.dateEcheance)!);
    }
    if (searchDto.dateEmission) {
      params = params.set('dateEmission', toLocalDateString(searchDto.dateEmission)!);
    }

    if (searchDto.montant) {
      params = params.set('montant', searchDto.montant.toString());
    }

    const url = `${this.apiUrl}/encaissements/encours/${banqueId}`;
    return this.http.get<EncoursLcnResponse>(url, {params});
  }
}
