import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {environment} from "../../../../environments/environment";
import {RapprochementRequest} from "../dtos/RapprochementRequest";
import {RapprochementResponse} from "../dtos/RapprochementResponse";
import {RapprochementSearchDto} from "../dtos/RapprochementSearchDto";
import {PageResponse} from "../../global/types/PageResponse";
import {TransactionSearchDto} from "../../transactions/dtos/TransactionSearchDto";
import {toLocalDateString} from "../../global/types/date.utils";
import {RapprochementActifsDto} from "../dtos/RapprochementActifsDto";

@Injectable({
  providedIn: 'root'
})
export class RapprochementService {

  private readonly baseUrl = `${environment.apiUrl}rapprochement`;
  private readonly http = inject(HttpClient);


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

    if (searchDto.statutRapprochement) {
      params = params.set('statutRapprochement', searchDto.statutRapprochement); 
    }

    return this.http.get<PageResponse<RapprochementResponse>>(
      this.baseUrl,
      { params: params }
    );
  }

  /**
   * Télécharge le rapport PDF d'un rapprochement. Si {@code searchDto} est fourni, seules les
   * transactions correspondant aux critères (libellé, observation, montant, sources, dates
   * d'opération — mêmes filtres que l'écran détail) sont incluses ; sans filtre, le rapport
   * contient toutes les transactions du rapprochement (comportement historique inchangé).
   */
  downloadReport(rapprochementId: number, reportType: 'FINANCE' | 'COMPTABILITE', searchDto?: TransactionSearchDto): Observable<Blob> {
    const url = `${this.baseUrl}/${rapprochementId}/report/pdf`;

    let params = new HttpParams().set('reportType', reportType);
    if (searchDto) {
      params = this.appendFilterParams(params, searchDto);
    }

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

  getActifs(banqueId: number): Observable<RapprochementActifsDto> {
    return this.http.get<RapprochementActifsDto>(
      `${this.baseUrl}/banque/${banqueId}/actifs`
    );
  }

  cloturerDefinitivement(rapprochementId: number): Observable<RapprochementResponse> {
    return this.http.post<RapprochementResponse>(
      `${this.baseUrl}/${rapprochementId}/cloturer-definitivement`,
      {}
    );
  }

  private buildParams(searchDto: TransactionSearchDto, page: number, size: number, sort: string): HttpParams {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return this.appendFilterParams(params, searchDto);
  }

  /**
   * Ajoute les critères de filtre de {@link TransactionSearchDto} (libellé, observation, montant,
   * sources, dates d'opération) à des `HttpParams` existants — factorisé entre la recherche
   * paginée ({@link buildParams}) et l'export PDF filtré ({@link downloadReport}).
   */
  private appendFilterParams(params: HttpParams, searchDto: TransactionSearchDto): HttpParams {
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

    if (searchDto.dateEmissionDebut) {
      params = params.set(
        'dateEmissionDebut',
        toLocalDateString(new Date(searchDto.dateEmissionDebut))!
      );
    }

    if (searchDto.dateEmissionFin) {
      params = params.set(
        'dateEmissionFin',
        toLocalDateString(new Date(searchDto.dateEmissionFin))!
      );
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

    if (searchDto.dateValeurDebut) {
      params = params.set(
        'dateValeurDebut',
        toLocalDateString(new Date(searchDto.dateValeurDebut))!
      );
    }

    if (searchDto.dateValeurFin) {
      params = params.set(
        'dateValeurFin',
        toLocalDateString(new Date(searchDto.dateValeurFin))!
      );
    }
    return params;
  }

}
