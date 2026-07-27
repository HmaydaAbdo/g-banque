import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from "../../../../environments/environment";
import { TransactionSearchDto } from "../dtos/TransactionSearchDto";
import { PageResponse } from "../../global/types/PageResponse";
import { TransactionResponseDto } from "../dtos/TransactionResponseDto";
import { AgreratedTransactionResponseDto } from "../dtos/AgreratedTransactionResponseDto";
import {TransactionUpdateRequestDto} from "../dtos/TransactionUpdateRequestDto";
import {toLocalDateString} from "../../global/types/date.utils";

@Injectable({
  providedIn: 'root'
})
export class TransactionService {

  private readonly baseUrl = `${environment.apiUrl}transactions`;

  constructor(private http: HttpClient) { }

  /**
   * Récupère l'historique global des transactions avec filtres et pagination.
   */
  getTransactionHistory(
    searchDto: TransactionSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<PageResponse<TransactionResponseDto>> {
    const params = this.buildParams(searchDto, page, size, sort);
    return this.http.get<PageResponse<TransactionResponseDto>>(`${this.baseUrl}/history`, { params });
  }

  /**
   * Exporte en PDF l'intégralité de l'historique des transactions correspondant aux filtres
   * fournis (mêmes critères que {@link getTransactionHistory}, sans pagination).
   */
  exportTransactionHistoryPdf(searchDto: TransactionSearchDto): Observable<Blob> {
    const params = this.buildFilterParams(searchDto);
    return this.http.get(`${this.baseUrl}/history/export/pdf`, { params, responseType: 'blob' });
  }

  /**
   * Récupère une transaction spécifique par son ID.
   */
  getTransactionById(id: number): Observable<TransactionResponseDto> {
    return this.http.get<TransactionResponseDto>(`${this.baseUrl}/${id}`);
  }

  /**
   * Récupère les transactions agrégées (totaux + page) pour une banque spécifique.
   */
  getTransactionsByBanque(
    banqueId: number,
    searchDto: TransactionSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'createdAt,desc'
  ): Observable<AgreratedTransactionResponseDto> {
    const params = this.buildParams(searchDto, page, size, sort);
    return this.http.get<AgreratedTransactionResponseDto>(`${this.baseUrl}/${banqueId}/transactions`, { params });
  }


  /**
   * Exporte en PDF les transactions d'une banque (mêmes critères que {@link getTransactionsByBanque},
   * sans pagination) — écran "Rapprochement en cours".
   */
  exportBankTransactionsPdf(banqueId: number, searchDto: TransactionSearchDto): Observable<Blob> {
    const params = this.buildFilterParams(searchDto);
    return this.http.get(`${this.baseUrl}/${banqueId}/transactions/export/pdf`, { params, responseType: 'blob' });
  }


  updateTransactionById(id: number, updateTransactionDto: TransactionUpdateRequestDto): Observable<TransactionResponseDto> {
    return this.http.patch<TransactionResponseDto>(`${this.baseUrl}/${id}`, updateTransactionDto);
  }


  /**
   * Méthode privée utilitaire pour construire les HttpParams paginés.
   * Elle gère les tableaux (transactionSources) et les types primitifs.
   */
  private buildParams(searchDto: TransactionSearchDto, page: number, size: number, sort: string): HttpParams {
    let params = this.buildFilterParams(searchDto)
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return params;
  }

  /**
   * Construit les HttpParams des seuls critères de filtrage (sans pagination) — partagée entre
   * {@link getTransactionHistory} et {@link exportTransactionHistoryPdf}.
   */
  private buildFilterParams(searchDto: TransactionSearchDto): HttpParams {
    let params = new HttpParams();

    if (searchDto.libelle) {
      params = params.set('libelle', searchDto.libelle);
    }

    if (searchDto.observation) {
      params = params.set('observation', searchDto.observation);
    }

    if (searchDto.banqueId) {
      params = params.set('banqueId', searchDto.banqueId.toString());
    }

    if (searchDto.rapprochementId) {
      params = params.set('rapprochementId', searchDto.rapprochementId.toString());
    }

    // Gestion du statut du rapprochement
    if (searchDto.statutRapprochement) {
      params = params.set('statutRapprochement', searchDto.statutRapprochement);
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

    params = this.appendDateRangeParams(params, searchDto);
    return params;
  }

  /** Ajoute les 3 plages de dates (émission, opération, valeur) si renseignées. */
  private appendDateRangeParams(params: HttpParams, searchDto: TransactionSearchDto): HttpParams {
    if (searchDto.dateEmissionDebut) {
      params = params.set('dateEmissionDebut', toLocalDateString(new Date(searchDto.dateEmissionDebut))!);
    }
    if (searchDto.dateEmissionFin) {
      params = params.set('dateEmissionFin', toLocalDateString(new Date(searchDto.dateEmissionFin))!);
    }
    if (searchDto.dateOperationDebut) {
      params = params.set('dateOperationDebut', toLocalDateString(new Date(searchDto.dateOperationDebut))!);
    }
    if (searchDto.dateOperationFin) {
      params = params.set('dateOperationFin', toLocalDateString(new Date(searchDto.dateOperationFin))!);
    }
    if (searchDto.dateValeurDebut) {
      params = params.set('dateValeurDebut', toLocalDateString(new Date(searchDto.dateValeurDebut))!);
    }
    if (searchDto.dateValeurFin) {
      params = params.set('dateValeurFin', toLocalDateString(new Date(searchDto.dateValeurFin))!);
    }
    return params;
  }
}
