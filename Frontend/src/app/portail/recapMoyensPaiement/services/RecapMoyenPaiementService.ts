import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { RecapMoyenPaiementSearchDto } from '../dtos/RecapMoyenPaiementSearchDto';
import { AgregatedRecapMoyenPaiementDto } from '../dtos/AgregatedRecapMoyenPaiementDto';

@Injectable({
  providedIn: 'root'
})
export class RecapMoyenPaiementService {

  private readonly baseUrl = `${environment.apiUrl}recap-moyens-paiement`;

  constructor(private http: HttpClient) {
  }

  getEncaissements(
    searchDto: RecapMoyenPaiementSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateValeur,desc'
  ): Observable<AgregatedRecapMoyenPaiementDto> {
    const params = this.buildParams(searchDto, page, size, sort);
    return this.http.get<AgregatedRecapMoyenPaiementDto>(`${this.baseUrl}/encaissement`, { params });
  }

  getDecaissements(
    searchDto: RecapMoyenPaiementSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateValeur,desc'
  ): Observable<AgregatedRecapMoyenPaiementDto> {
    const params = this.buildParams(searchDto, page, size, sort);
    return this.http.get<AgregatedRecapMoyenPaiementDto>(`${this.baseUrl}/decaissement`, { params });
  }

  exportEncaissementsPdf(searchDto: RecapMoyenPaiementSearchDto): Observable<Blob> {
    const params = this.buildFilterParams(searchDto);
    return this.http.get(`${this.baseUrl}/encaissement/export/pdf`, { params, responseType: 'blob' });
  }

  exportDecaissementsPdf(searchDto: RecapMoyenPaiementSearchDto): Observable<Blob> {
    const params = this.buildFilterParams(searchDto);
    return this.http.get(`${this.baseUrl}/decaissement/export/pdf`, { params, responseType: 'blob' });
  }

  private buildParams(searchDto: RecapMoyenPaiementSearchDto, page: number, size: number, sort: string): HttpParams {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);
    return this.appendFilters(params, searchDto);
  }

  private buildFilterParams(searchDto: RecapMoyenPaiementSearchDto): HttpParams {
    return this.appendFilters(new HttpParams(), searchDto);
  }

  private appendFilters(params: HttpParams, searchDto: RecapMoyenPaiementSearchDto): HttpParams {
    if (searchDto.banqueId !== null && searchDto.banqueId !== undefined) {
      params = params.set('banqueId', searchDto.banqueId.toString());
    }
    if (searchDto.sources && searchDto.sources.length > 0) {
      searchDto.sources.forEach(source => {
        params = params.append('sources', source);
      });
    }
    if (searchDto.statuts && searchDto.statuts.length > 0) {
      searchDto.statuts.forEach(statut => {
        params = params.append('statuts', statut);
      });
    }
    if (searchDto.libelle) {
      params = params.set('libelle', searchDto.libelle);
    }
    if (searchDto.numero) {
      params = params.set('numero', searchDto.numero);
    }
    if (searchDto.montant !== null && searchDto.montant !== undefined) {
      params = params.set('montant', searchDto.montant.toString());
    }
    if (searchDto.dateOperationFrom) {
      params = params.set('dateOperationFrom', this.toIsoDate(searchDto.dateOperationFrom));
    }
    if (searchDto.dateOperationTo) {
      params = params.set('dateOperationTo', this.toIsoDate(searchDto.dateOperationTo));
    }
    if (searchDto.dateValeurFrom) {
      params = params.set('dateValeurFrom', this.toIsoDate(searchDto.dateValeurFrom));
    }
    if (searchDto.dateValeurTo) {
      params = params.set('dateValeurTo', this.toIsoDate(searchDto.dateValeurTo));
    }
    return params;
  }

  private toIsoDate(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }
}
