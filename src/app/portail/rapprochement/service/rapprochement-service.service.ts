import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {environment} from "../../../../environments/environment";
import {RapprochementRequest} from "../dtos/RapprochementRequest";
import {RapprochementResponse} from "../dtos/RapprochementResponse";
import {RapprochementSearchDto} from "../dtos/RapprochementSearchDto";
import {PageResponse} from "../../global/types/PageResponse";
import {TransactionSearchDto} from "../../transactions/dtos/TransactionSearchDto";
import {toLocalDateString} from "../../global/types/date.utils";

@Injectable({
  providedIn: 'root'
})
export class RapprochementService {

  private readonly baseUrl = `${environment.apiUrl}rapprochement`;

  constructor(private http: HttpClient) { }


  cloturerRapprochement(requestDto: RapprochementRequest): Observable<RapprochementResponse> {
    return this.http.post<RapprochementResponse>(
      `${this.baseUrl}/cloturer`,
      requestDto
    );
  }


  getRapprochementById(
    rapprochementId: number,
    searchDto: TransactionSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<RapprochementResponse> {

    const params = this.buildParams(searchDto, page, size, sort);

    return this.http.get<RapprochementResponse>(
      `${this.baseUrl}/${rapprochementId}`,
      { params: params }
    );
  }
  searchRapprochements(
    searchDto: RapprochementSearchDto,
    page: number = 0,
    size: number = 10
  ): Observable<PageResponse<RapprochementResponse>> {

    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (searchDto.banqueId) {
      params = params.set('banqueId', searchDto.banqueId.toString());
    }

    if (searchDto.dateRapprochement) {
      params = params.set('dateRapprochement', searchDto.dateRapprochement); // Doit être au format 'MM/YYYY'
    }

    return this.http.get<PageResponse<RapprochementResponse>>(
      this.baseUrl,
      { params: params }
    );
  }

  downloadReport(rapprochementId: number, reportType: 'FINANCE' | 'COMPTABILITE'): Observable<Blob> {
    const url = `${this.baseUrl}/${rapprochementId}/report/pdf`;

    const params = new HttpParams().set('reportType', reportType);

    return this.http.get(url, {
      responseType: 'blob',
      params: params
    });
  }

  annulerTransaction(transactionId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/annuler/${transactionId}`
    );
  }

  private buildParams(searchDto: TransactionSearchDto, page: number, size: number, sort: string): HttpParams {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (searchDto.libelle) {
      params = params.set('libelle', searchDto.libelle);
    }

    if (searchDto.observation) {
      params = params.set('observation', searchDto.observation);
    }


    // Gestion propre du tableau Enum pour le backend Spring
    if (searchDto.transactionSources && searchDto.transactionSources.length > 0) {
      searchDto.transactionSources.forEach(source => {
        params = params.append('transactionSources', source);
      });
    }

    if (searchDto.montant !== undefined && searchDto.montant !== null) {
      params = params.set('montant', searchDto.montant.toString());
    }


    if (searchDto.dateOperationDebut) {
      params = params.set(
        'dateOperationDebut',
        toLocalDateString(new Date(searchDto.dateOperationDebut))!
      );
    }

    if (searchDto.dateOperationFin) {
      params = params.set(
        'dateOperationFin',
        toLocalDateString(new Date(searchDto.dateOperationFin))!
      );
    }
    return params;
  }

}
