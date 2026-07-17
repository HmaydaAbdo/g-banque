import { inject, Injectable } from '@angular/core';
import {environment} from "../../../../../../../environments/environment";
import {HttpClient, HttpParams} from "@angular/common/http";
import {Observable} from "rxjs";
import {DecaissementSearchDto} from "../dtos/DecaissementSearchDto";
import {AgregatedRecapDto} from "../dtos/AgregatedRecapDto";

@Injectable({
  providedIn: 'root'
})
export class RecapService {
  private readonly baseUrl = `${environment.apiUrl}recapitulatif`;

  private readonly http = inject(HttpClient);

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
        params = params.append('observation', searchDto.observation);
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


  /**
   * Downloads the global summary report (PDF) for a specific bank.
   *
   * @param banqueId The ID of the bank.
   * @returns Observable<Blob> An observable of the PDF file as a Blob.
   */
  downloadRecapGlobalByBank(banqueId: number): Observable<Blob> {
    const url = `${this.baseUrl}/${banqueId}/pdf`;
    return this.http.get(url, { responseType: 'blob' });
  }

  /**
   * Downloads the consolidated global summary report (PDF) for all banks.
   *
   * @returns Observable<Blob> An observable of the PDF file as a Blob.
   */
  downloadGlobalRecapForAllBanks(): Observable<Blob> {
    const url = `${this.baseUrl}/global/pdf`;
    return this.http.get(url, { responseType: 'blob' });
  }

  /**
   * Télécharge le PDF du Récap Général Détaillé pour une banque.
   */
  downloadRecapGeneralDetaillePdf(banqueId: number): Observable<Blob> {
    const url = `${environment.apiUrl}recap-general-detaille/banque/${banqueId}/pdf`;
    return this.http.get(url, { responseType: 'blob' });
  }

  /**
   * Télécharge le PDF du Récap Général Détaillé pour toutes les banques.
   */
  downloadRecapGeneralDetailleAllBanquesPdf(): Observable<Blob> {
    const url = `${environment.apiUrl}recap-general-detaille/all/pdf`;
    return this.http.get(url, { responseType: 'blob' });
  }
}
