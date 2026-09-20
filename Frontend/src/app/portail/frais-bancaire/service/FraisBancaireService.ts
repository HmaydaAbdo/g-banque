import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

import { FraisBancaireRequestDto } from '../dtos/FraisBancaireRequestDto';
import { FraisBancaireResponseDto } from '../dtos/FraisBancaireResponseDto';
import { FraisBancaireSearchDto } from '../dtos/FraisBancaireSearchDto';
import { PageResponse } from '../../global/types/PageResponse';
import { toLocalDateString } from '../../global/types/date.utils';

@Injectable({
  providedIn: 'root'
})
export class FraisBancaireService {

  /** Base API URL for Frais Bancaire endpoints */
  private readonly apiUrl = `${environment.apiUrl}frais-bancaire`;

  constructor(private http: HttpClient) {}

  /**
   * Creates a new Frais Bancaire record.
   *
   * @param requestDto Data required to create the Frais Bancaire.
   * @returns Observable emitting the created FraisBancaireResponseDto.
   */
  createFraisBancaire(requestDto: FraisBancaireRequestDto): Observable<FraisBancaireResponseDto> {
    return this.http.post<FraisBancaireResponseDto>(this.apiUrl, requestDto);
  }

  /**
   * Retrieves a Frais Bancaire record by its ID.
   *
   * @param id Identifier of the Frais Bancaire to retrieve.
   * @returns Observable emitting the FraisBancaireResponseDto.
   */
  getById(id: number): Observable<FraisBancaireResponseDto> {
    return this.http.get<FraisBancaireResponseDto>(`${this.apiUrl}/${id}`);
  }

  /**
   * Searches for Frais Bancaires with pagination and sorting.
   *
   * @param searchDto Criteria for filtering Frais Bancaire records.
   * @param page Current page number (0-indexed).
   * @param size Number of records per page.
   * @param sort Sorting criteria, e.g., 'dateEmission,desc'.
   * @returns Observable emitting a paginated list of FraisBancaireResponseDto.
   */
  searchFraisBancaire(
    searchDto: FraisBancaireSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<PageResponse<FraisBancaireResponseDto>> {

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return this.http.post<PageResponse<FraisBancaireResponseDto>>(
      `${this.apiUrl}/search`, this.toSearchPayload(searchDto), { params }
    );
  }

  /**
   * Convertit les champs {@code Date} de {@link FraisBancaireSearchDto} en chaînes `yyyy-MM-dd`
   * avant envoi en corps JSON — un objet {@code Date} sérialisé nativement produit un datetime
   * ISO complet que {@code LocalDate} côté backend ne sait pas parser.
   */
  private toSearchPayload(searchDto: FraisBancaireSearchDto): any {
    return {
      ...searchDto,
      dateEmissionDebut: toLocalDateString(searchDto.dateEmissionDebut),
      dateEmissionFin: toLocalDateString(searchDto.dateEmissionFin),
      dateOperationDebut: toLocalDateString(searchDto.dateOperationDebut),
      dateOperationFin: toLocalDateString(searchDto.dateOperationFin),
      dateValeurDebut: toLocalDateString(searchDto.dateValeurDebut),
      dateValeurFin: toLocalDateString(searchDto.dateValeurFin),
    };
  }

  /**
   * Exporte en PDF l'intégralité des frais bancaires correspondant aux filtres fournis (mêmes
   * critères que {@link searchFraisBancaire}, sans pagination).
   */
  exportFraisBancairePdf(searchDto: FraisBancaireSearchDto): Observable<Blob> {
    let params = new HttpParams();

    if (searchDto.banqueId !== null && searchDto.banqueId !== undefined) {
      params = params.set('banqueId', searchDto.banqueId.toString());
    }
    if (searchDto.libelle) {
      params = params.set('libelle', searchDto.libelle);
    }
    if (searchDto.montant !== null && searchDto.montant !== undefined) {
      params = params.set('montant', searchDto.montant.toString());
    }
    if (searchDto.status) {
      params = params.set('status', searchDto.status);
    }
    params = this.appendDateRangeParams(params, searchDto);

    return this.http.get(`${this.apiUrl}/export/pdf`, { params, responseType: 'blob' });
  }

  /** Ajoute les 3 plages de dates (émission, opération, valeur) si renseignées. */
  private appendDateRangeParams(params: HttpParams, searchDto: FraisBancaireSearchDto): HttpParams {
    if (searchDto.dateEmissionDebut) {
      params = params.set('dateEmissionDebut', toLocalDateString(searchDto.dateEmissionDebut)!);
    }
    if (searchDto.dateEmissionFin) {
      params = params.set('dateEmissionFin', toLocalDateString(searchDto.dateEmissionFin)!);
    }
    if (searchDto.dateOperationDebut) {
      params = params.set('dateOperationDebut', toLocalDateString(searchDto.dateOperationDebut)!);
    }
    if (searchDto.dateOperationFin) {
      params = params.set('dateOperationFin', toLocalDateString(searchDto.dateOperationFin)!);
    }
    if (searchDto.dateValeurDebut) {
      params = params.set('dateValeurDebut', toLocalDateString(searchDto.dateValeurDebut)!);
    }
    if (searchDto.dateValeurFin) {
      params = params.set('dateValeurFin', toLocalDateString(searchDto.dateValeurFin)!);
    }
    return params;
  }

  /**
   * Updates an existing Frais Bancaire record.
   *
   * @param id Identifier of the Frais Bancaire to update.
   * @param requestDto Updated data for the Frais Bancaire.
   * @returns Observable emitting the updated FraisBancaireResponseDto.
   */
  updateFraisBancaire(id: number, requestDto: FraisBancaireRequestDto): Observable<FraisBancaireResponseDto> {
    return this.http.put<FraisBancaireResponseDto>(`${this.apiUrl}/${id}`, requestDto);
  }

  /**
   * Deletes a Frais Bancaire record by its ID.
   *
   * @param id Identifier of the Frais Bancaire to delete.
   * @returns Observable emitting void upon success.
   */
  deleteFraisBancaire(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  toggleStatus(id: number): Observable<FraisBancaireResponseDto> {
    return this.http.patch<FraisBancaireResponseDto>(`${this.apiUrl}/${id}/toggle-status`, {});
  }
}
