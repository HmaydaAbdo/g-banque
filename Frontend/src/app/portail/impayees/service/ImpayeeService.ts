import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ImpayeeRequestDto } from '../dtos/ImpayeeRequestDto';
import { ImpayeeResponseDto } from '../dtos/ImpayeeResponseDto';
import { ImpayeeSearchDto } from '../dtos/ImpayeeSearchDto';
import { PageResponse } from '../../global/types/PageResponse';
import { toLocalDateString } from '../../global/types/date.utils';

@Injectable({
  providedIn: 'root'
})
export class ImpayeeService {

  private apiUrl = `${environment.apiUrl}impayees`;

  constructor(private http: HttpClient) {}

  // --- Create ---
  createImpayee(requestDto: ImpayeeRequestDto): Observable<ImpayeeResponseDto> {
    return this.http.post<ImpayeeResponseDto>(this.apiUrl, requestDto);
  }

  // --- Get by ID ---
  getImpayeeById(id: number): Observable<ImpayeeResponseDto> {
    return this.http.get<ImpayeeResponseDto>(`${this.apiUrl}/${id}`);
  }

  // --- Search with pagination ---
  searchImpayees(
    searchDto: ImpayeeSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ): Observable<PageResponse<ImpayeeResponseDto>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return this.http.post<PageResponse<ImpayeeResponseDto>>(`${this.apiUrl}/search`, this.toSearchPayload(searchDto), { params });
  }

  /**
   * Convertit les champs {@code Date} de {@link ImpayeeSearchDto} en chaînes `yyyy-MM-dd` avant
   * envoi en corps JSON — un objet {@code Date} sérialisé nativement produit un datetime ISO
   * complet que {@code LocalDate} côté backend ne sait pas parser.
   */
  private toSearchPayload(searchDto: ImpayeeSearchDto): any {
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

  // --- Update ---
  updateImpayee(id: number, requestDto: ImpayeeRequestDto): Observable<ImpayeeResponseDto> {
    return this.http.put<ImpayeeResponseDto>(`${this.apiUrl}/${id}`, requestDto);
  }

  // --- Delete ---
  deleteImpayee(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // --- Toggle Status ---
  toggleStatus(id: number): Observable<ImpayeeResponseDto> {
    return this.http.patch<ImpayeeResponseDto>(`${this.apiUrl}/${id}/toggle-status`, {});
  }

  /**
   * Exporte en PDF l'intégralité des impayées correspondant aux filtres fournis (mêmes critères
   * que {@link searchImpayees}, sans pagination — le PDF contient tous les résultats filtrés, pas
   * seulement la page actuellement affichée).
   */
  exportPdf(searchDto: ImpayeeSearchDto): Observable<Blob> {
    let params = new HttpParams();

    if (searchDto.banqueId != null) params = params.set('banqueId', searchDto.banqueId.toString());
    if (searchDto.libelle)          params = params.set('libelle', searchDto.libelle);
    if (searchDto.motif)            params = params.set('motif', searchDto.motif);
    if (searchDto.type)             params = params.set('type', searchDto.type);
    if (searchDto.montant != null)  params = params.set('montant', searchDto.montant.toString());
    if (searchDto.status)           params = params.set('status', searchDto.status);
    params = this.appendDateRangeParams(params, searchDto);

    return this.http.get(`${this.apiUrl}/export/pdf`, { params, responseType: 'blob' });
  }

  /** Ajoute les 3 plages de dates (émission, opération, valeur) si renseignées. */
  private appendDateRangeParams(params: HttpParams, searchDto: ImpayeeSearchDto): HttpParams {
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
}
