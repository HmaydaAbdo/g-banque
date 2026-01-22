import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from "../../../../environments/environment";
import { TransactionSearchDto } from "../dtos/TransactionSearchDto";
import { PageResponse } from "../../global/types/PageResponse";
import { TransactionResponseDto } from "../dtos/TransactionResponseDto";
import { AgreratedTransactionResponseDto } from "../dtos/AgreratedTransactionResponseDto";

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
   * Méthode privée utilitaire pour construire les HttpParams.
   * Elle gère les tableaux (transactionSources) et les types primitifs.
   */
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

    if (searchDto.banqueId) {
      params = params.set('banqueId', searchDto.banqueId.toString());
    }

    // Gestion du flag de confirmation (isConfirmed côté front -> confirmed côté backend API)
    if (searchDto.isConfirmed !== undefined && searchDto.isConfirmed !== null) {
      params = params.set('confirmed', searchDto.isConfirmed.toString());
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

    return params;
  }
}
