import { Injectable } from '@angular/core';
import {environment} from "../../../../../../../environments/environment";
import {HttpClient, HttpParams} from "@angular/common/http";
import {Observable} from "rxjs";
import {PageResponse} from "../../../../../global/types/PageResponse";
import {RecaputilatifItem} from "../dtos/RecaputilatifItem";
import {DecaissementSearchDto} from "../dtos/DecaissementSearchDto";
import {AgregatedRecapDto} from "../dtos/AgregatedRecapDto";

@Injectable({
  providedIn: 'root'
})
export class RecapService {
  private readonly baseUrl = `${environment.apiUrl}recapitulatif`;

  constructor(private http: HttpClient) { }

  /**
   * Retrieves a paginated and sortable list of outstanding disbursements for a specific bank.
   *
   * @param banqueId The ID of the bank to filter by.
   * @param searchDto
   * @param page The page number to retrieve (default: 0).
   * @param size The number of items per page (default: 10).
   * @param sort The sorting criteria (e.g., 'dateEmission,asc' or 'debit,desc').
   * @returns Observable<Page<RecaputilatifItem>> An observable of the pageable response.
   */
  /**
   * Retrieves a paginated and sortable list of outstanding disbursements for a specific bank.
   *
   * @param banqueId The ID of the bank to filter by.
   * @param searchDto
   * @param page The page number to retrieve (default: 0).
   * @param size The number of items per page (default: 10).
   * @param sort The sorting criteria (e.g., 'dateEmission,asc' or 'debit,desc').
   * @returns Observable<Page<RecaputilatifItem>> An observable of the pageable response.
   */
  getRecapitulatifDecaissement(
    banqueId: number,
    searchDto: DecaissementSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,asc'
  ): Observable<AgregatedRecapDto> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);


    if (searchDto.transactionSources && searchDto.transactionSources.length > 0) {
      searchDto.transactionSources.forEach(source => {
        params = params.append('transactionSources', source);
      });
    }

    if (searchDto.libelle && searchDto.libelle.length > 0) {
      params = params.append('libelle', searchDto.libelle);
    }

    if (searchDto.observation && searchDto.observation.length > 0) {
        params = params.append('observations', searchDto.observation);
    }



    const url = `${this.baseUrl}/decaissement/banque/${banqueId}`;

    return this.http.get<AgregatedRecapDto>(url, { params });
  }
  /**
   * Retrieves a paginated and sortable list of outstanding disbursements for a specific bank.
   *
   * @param banqueId The ID of the bank to filter by.
   * @param searchDto
   * @param page The page number to retrieve (default: 0).
   * @param size The number of items per page (default: 10).
   * @param sort The sorting criteria (e.g., 'dateEmission,asc' or 'debit,desc').
   * @returns Observable<Page<RecaputilatifItem>> An observable of the pageable response.
   */
  getRecapitulatifEncaissement(
    banqueId: number,
    searchDto: DecaissementSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,asc'
  ): Observable<AgregatedRecapDto> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (searchDto.transactionSources && searchDto.transactionSources.length > 0) {
      searchDto.transactionSources.forEach(source => {
        params = params.append('transactionSources', source);
      });
    }

    if (searchDto.libelle && searchDto.libelle.length > 0) {
      params = params.append('libelle', searchDto.libelle);
    }

    if (searchDto.observation && searchDto.observation.length > 0) {
      params = params.append('observation', searchDto.observation);
    }
    // Construct the URL with the banqueId as a path variable
    const url = `${this.baseUrl}/encaissement/banque/${banqueId}`;

    return this.http.get<AgregatedRecapDto>(url, { params });
  }
}
