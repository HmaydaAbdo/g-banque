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

    Object.entries(searchDto).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        let stringValue: string;

        if (key === 'dateEmission' && value instanceof Date) {
          stringValue = toLocalDateString(value)!;
        } else if (key === 'montant' && typeof value === 'number') {
          stringValue = value.toString();
        } else {
          stringValue = value.toString();
        }

        params = params.set(key, stringValue);
      }
    });


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

    Object.entries(searchDto).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        let stringValue: string;

        if (key === 'dateEmission' && value instanceof Date) {
          // CORRECTION: Formatte la Date en YYYY-MM-DD
          stringValue = toLocalDateString(value)!;
        } else if (key === 'montant' && typeof value === 'number') {
          stringValue = value.toString();
        } else {
          stringValue = value.toString();
        }

        params = params.set(key, stringValue);
      }
    });

    return this.http.get<PageResponse<VirementResponseDto>>(`${this.apiUrl}/encaissements`, {params});
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
}
