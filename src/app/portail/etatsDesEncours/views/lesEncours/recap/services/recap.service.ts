import { inject, Injectable } from '@angular/core';
import {environment} from "../../../../../../../environments/environment";
import {HttpClient, HttpParams} from "@angular/common/http";
import {Observable} from "rxjs";
import {DecaissementSearchDto} from "../dtos/DecaissementSearchDto";
import {AgregatedRecapDto} from "../dtos/AgregatedRecapDto";
import {PeriodeRecap} from "../dtos/PeriodeRecap";

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

    if (searchDto.dateReferenceJusqua) {
      params = params.append('dateReferenceJusqua', this.toIsoDate(searchDto.dateReferenceJusqua));
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

    if (searchDto.dateReferenceJusqua) {
      params = params.append('dateReferenceJusqua', this.toIsoDate(searchDto.dateReferenceJusqua));
    }
    // Construct the URL with the banqueId as a path variable
    const url = `${this.baseUrl}/encaissement/banque/${banqueId}`;

    return this.http.get<AgregatedRecapDto>(url, { params });
  }

  /**
   * Exporte en PDF le récapitulatif des décaissements prévisionnels du mois pour une banque
   * (mêmes critères que {@link getRecapitulatifDecaissement}, sans pagination).
   */
  exportRecapitulatifDecaissementPdf(banqueId: number, searchDto: DecaissementSearchDto): Observable<Blob> {
    let params = new HttpParams();

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

    if (searchDto.dateReferenceJusqua) {
      params = params.append('dateReferenceJusqua', this.toIsoDate(searchDto.dateReferenceJusqua));
    }

    const url = `${this.baseUrl}/decaissement/banque/${banqueId}/export/pdf`;
    return this.http.get(url, { params, responseType: 'blob' });
  }

  /**
   * Exporte en PDF le récapitulatif des encaissements prévisionnels du mois pour une banque
   * (mêmes critères que {@link getRecapitulatifEncaissement}, sans pagination).
   */
  exportRecapitulatifEncaissementPdf(banqueId: number, searchDto: DecaissementSearchDto): Observable<Blob> {
    let params = new HttpParams();

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

    if (searchDto.dateReferenceJusqua) {
      params = params.append('dateReferenceJusqua', this.toIsoDate(searchDto.dateReferenceJusqua));
    }

    const url = `${this.baseUrl}/encaissement/banque/${banqueId}/export/pdf`;
    return this.http.get(url, { params, responseType: 'blob' });
  }


  /**
   * Downloads the global summary report (PDF) for a specific bank.
   *
   * @param banqueId The ID of the bank.
   * @param periode 'A_CE_JOUR' (encours déjà échus/émis) ou 'FIN_DE_MOIS' (défaut backend).
   * @returns Observable<Blob> An observable of the PDF file as a Blob.
   */
  downloadRecapGlobalByBank(banqueId: number, periode: PeriodeRecap = 'FIN_DE_MOIS'): Observable<Blob> {
    const url = `${this.baseUrl}/${banqueId}/pdf`;
    const params = new HttpParams().set('periode', periode);
    return this.http.get(url, { params, responseType: 'blob' });
  }

  /**
   * Downloads the consolidated global summary report (PDF) for all banks.
   *
   * @param periode 'A_CE_JOUR' (encours déjà échus/émis) ou 'FIN_DE_MOIS' (défaut backend).
   * @returns Observable<Blob> An observable of the PDF file as a Blob.
   */
  downloadGlobalRecapForAllBanks(periode: PeriodeRecap = 'FIN_DE_MOIS'): Observable<Blob> {
    const url = `${this.baseUrl}/global/pdf`;
    const params = new HttpParams().set('periode', periode);
    return this.http.get(url, { params, responseType: 'blob' });
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

  /** Formate un objet Date en chaîne ISO yyyy-MM-dd pour le backend Spring. */
  private toIsoDate(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }
}
