
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {environment} from "../../../../environments/environment";
import {TransactionSearchDto} from "../dtos/TransactionSearchDto";
import {PageResponse} from "../../global/types/PageResponse";
import {TransactionResponseDto} from "../dtos/TransactionResponseDto";
import {AgreratedTransactionResponseDto} from "../dtos/AgreratedTransactionResponseDto";
import {TransactionUpdateRequestDto} from "../dtos/TransactionUpdateRequestDto";


@Injectable({
  providedIn: 'root'
})
export class TransactionService {

  private readonly baseUrl = `${environment.apiUrl}transactions`;

  constructor(private http: HttpClient) { }

  /**
   * Récupère l'historique de toutes les dtos avec des options de pagination, de tri et de recherche.
   * @param searchDto Les critères de recherche.
   * @param page Le numéro de page (commence à 0).
   * @param size Le nombre d'éléments par page.
   * @param sort La chaîne de tri (ex: 'dateEmission,desc').
   * @returns Observable<PageResponse<TransactionResponseDto>>
   */
  getTransactionHistory(
    searchDto: TransactionSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<PageResponse<TransactionResponseDto>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    // Ajout dynamique des paramètres de recherche si les valeurs sont présentes dans le DTO
    if (searchDto.libelle) {
      params = params.set('libelle', searchDto.libelle);
    }
    if (searchDto.banqueId) {
      params = params.set('banqueId', searchDto.banqueId.toString());
    }

    if (searchDto.transactionSource && searchDto.transactionSource.length > 0) {
      searchDto.transactionSource.forEach(source => {
        params = params.append('transactionSources', source);
      });
    }
    return this.http.get<PageResponse<TransactionResponseDto>>(`${this.baseUrl}/history`, { params });
  }

  getTransactionById(id: number): Observable<TransactionResponseDto> {
    return this.http.get<TransactionResponseDto>(`${this.baseUrl}/${id}`);
  }

  /**
   * Récupère une liste paginée de dtos pour une banque spécifique pour le mois en cours.
   * @param banqueId L'ID de la banque.
   * @param searchDto Les critères de recherche (libelle).
   * @param page Le numéro de page (commence à 0).
   * @param size Le nombre d'éléments par page.
   * @param sort La chaîne de tri (ex: 'createdAt,desc').
   * @returns Observable<PageResponse<TransactionResponseDto>>
   */
  getTransactionsByBanque(
    banqueId: number,
    searchDto: TransactionSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'createdAt,desc'
  ): Observable<AgreratedTransactionResponseDto> {
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

    if (searchDto.transactionSource && searchDto.transactionSource.length > 0) {
      searchDto.transactionSource.forEach(source => {
        params = params.append('transactionSources', source);
      });
    }

    return this.http.get<AgreratedTransactionResponseDto>(`${this.baseUrl}/${banqueId}/transactions`, { params });
  }

  updateTransaction(
    id: number,
    dto: TransactionUpdateRequestDto
  ): Observable<TransactionResponseDto> {
    return this.http.put<TransactionResponseDto>(`${this.baseUrl}/${id}`, dto);
  }

  /**
   * Supprime une transaction par son ID.
   * @param id L'ID de la transaction à supprimer.
   * @returns Observable<void>
   */
  deleteTransaction(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }


}
