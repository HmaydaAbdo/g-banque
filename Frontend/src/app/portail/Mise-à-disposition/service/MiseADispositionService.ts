import {Injectable} from '@angular/core';
import { toLocalDateString } from '../../global/types/date.utils';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../../../environments/environment';
import {MiseADispositionSearchDto} from "../dtos/MiseADispositionSearchDto";
import {PageResponse} from "../../global/types/PageResponse";
import {MiseADispositionResponseDto} from "../dtos/MiseADispositionResponseDto";


@Injectable({
  providedIn: 'root'
})
export class MiseADispositionService {

  private apiUrl = `${environment.apiUrl}misesADisposition`;

  constructor(private http: HttpClient) {
  }

  getMiseAdispositionById(id:number):Observable<MiseADispositionResponseDto>{
    return this.http.get<MiseADispositionResponseDto>(`${this.apiUrl}/${id}`);
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
    params = this.appendDateRangeParams(params, searchDto);

    return this.http.get<PageResponse<MiseADispositionResponseDto>>(this.apiUrl+"/decaissements", {params});
  }


  /** Ajoute les 3 plages de dates (émission, opération, valeur) si renseignées. */
  private appendDateRangeParams(params: HttpParams, criteria: MiseADispositionSearchDto): HttpParams {
    if (criteria.dateEmissionFrom) params = params.set('dateEmissionFrom', toLocalDateString(criteria.dateEmissionFrom)!);
    if (criteria.dateEmissionTo) params = params.set('dateEmissionTo', toLocalDateString(criteria.dateEmissionTo)!);
    if (criteria.dateOperationFrom) params = params.set('dateOperationFrom', toLocalDateString(criteria.dateOperationFrom)!);
    if (criteria.dateOperationTo) params = params.set('dateOperationTo', toLocalDateString(criteria.dateOperationTo)!);
    if (criteria.dateValeurFrom) params = params.set('dateValeurFrom', toLocalDateString(criteria.dateValeurFrom)!);
    if (criteria.dateValeurTo) params = params.set('dateValeurTo', toLocalDateString(criteria.dateValeurTo)!);
    return params;
  }


  toggleMiseADispositionStatus(id: number): Observable<MiseADispositionResponseDto> {
    const url = `${this.apiUrl}/toggle/${id}`;
    return this.http.patch<MiseADispositionResponseDto>(url, {});
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
    params = this.appendDateRangeParams(params, searchDto);
    return this.http.get(`${this.apiUrl}/decaissements/export/pdf`, {params, responseType: 'blob'});
  }

}
