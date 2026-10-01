import { inject, Injectable } from '@angular/core';
import {environment} from "../../../../../../../environments/environment";
import {HttpClient, HttpParams} from "@angular/common/http";
import {Observable} from "rxjs";
import {RecapitulatifSearchDto} from "../dtos/RecapitulatifSearchDto";
import {AgregatedRecapDto} from "../dtos/AgregatedRecapDto";
import {PeriodeRecap} from "../dtos/PeriodeRecap";
import {RecapSourceDto} from "../dtos/RecapSourceDto";

@Injectable({
  providedIn: 'root'
})
export class RecapService {
  private readonly baseUrl = `${environment.apiUrl}recapitulatif`;

  private readonly http = inject(HttpClient);

  /**
   * Retrieves a paginated and sortable list of outstanding encaissements/décaissements for a
   * specific bank. The two screens (Décaissements/Encaissements) were merged into one on
   * 06/09/2026 — `searchDto.sens` now carries the optional sens filter that used to be fixed by
   * the route (undefined/null = both sens, interleaved).
   *
   * @param banqueId The ID of the bank to filter by.
   * @param searchDto
   * @param page The page number to retrieve (default: 0).
   * @param size The number of items per page (default: 10).
   * @param sort The sorting criteria (e.g., 'dateReference,desc' or 'debit,desc').
   * @returns Observable<AgregatedRecapDto> An observable of the pageable response.
   */
  getRecapitulatif(
    banqueId: number,
    searchDto: RecapitulatifSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateReference,desc'
  ): Observable<AgregatedRecapDto> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    params = this.appendSearchParams(params, searchDto);

    const url = `${this.baseUrl}/banque/${banqueId}`;

    return this.http.get<AgregatedRecapDto>(url, { params });
  }

  /**
   * Indicateurs par source (cartes de l'écran Récapitulatif) — mêmes critères que
   * {@link getRecapitulatif}, sans pagination.
   */
  getRecapitulatifParSource(banqueId: number, searchDto: RecapitulatifSearchDto): Observable<RecapSourceDto[]> {
    const params = this.appendSearchParams(new HttpParams(), searchDto);
    return this.http.get<RecapSourceDto[]>(`${this.baseUrl}/banque/${banqueId}/par-source`, { params });
  }

  /**
   * Exporte en PDF le récapitulatif des encours prévisionnels du mois pour une banque
   * (mêmes critères que {@link getRecapitulatif}, sans pagination).
   */
  exportRecapitulatifPdf(banqueId: number, searchDto: RecapitulatifSearchDto): Observable<Blob> {
    const params = this.appendSearchParams(new HttpParams(), searchDto);

    const url = `${this.baseUrl}/banque/${banqueId}/export/pdf`;
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

  /**
   * Construit les HttpParams communs à {@link getRecapitulatif} et {@link exportRecapitulatifPdf}
   * à partir du DTO de recherche — factorisé pour ne pas dupliquer cette logique deux fois comme
   * c'était le cas du temps des deux écrans séparés.
   */
  private appendSearchParams(params: HttpParams, searchDto: RecapitulatifSearchDto): HttpParams {
    if (searchDto.transactionSources && searchDto.transactionSources.length > 0) {
      searchDto.transactionSources.forEach(source => {
        params = params.append('transactionSources', source);
      });
    }

    if (searchDto.numero && searchDto.numero.length > 0) {
      params = params.append('numero', searchDto.numero);
    }

    if (searchDto.libelleOuObservation && searchDto.libelleOuObservation.length > 0) {
      params = params.append('libelleOuObservation', searchDto.libelleOuObservation);
    }

    if (searchDto.sens) {
      params = params.append('sens', searchDto.sens);
    }

    if (searchDto.dateReferenceDepuis) {
      params = params.append('dateReferenceDepuis', this.toIsoDate(searchDto.dateReferenceDepuis));
    }

    if (searchDto.dateReferenceJusqua) {
      params = params.append('dateReferenceJusqua', this.toIsoDate(searchDto.dateReferenceJusqua));
    }

    if (searchDto.montant !== null && searchDto.montant !== undefined) {
      params = params.append('montant', searchDto.montant.toString());
    }

    if (searchDto.commissionOperation) {
      params = params.append('commissionOperation', searchDto.commissionOperation);
    }

    if (searchDto.impotEtTaxeMotif) {
      params = params.append('impotEtTaxeMotif', searchDto.impotEtTaxeMotif);
    }

    return params;
  }

  /** Formate un objet Date en chaîne ISO yyyy-MM-dd pour le backend Spring. */
  private toIsoDate(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }
}
