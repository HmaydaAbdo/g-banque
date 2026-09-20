import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../../../environments/environment';
import {CommissionRequestDto} from "../dtos/CommissionRequestDto";
import {CommissionResponseDto} from "../dtos/commission-response.dto";
import {CommissionSearchDto} from "../dtos/CommissionSearchDto";
import {EncoursCommissionDto} from "../dtos/EncoursCommissionDto";
import {CommissionFusionRequest} from "../dtos/CommissionFusionRequest";
import {CommissionFusionResponse} from "../dtos/CommissionFusionResponse";
import {toLocalDateString} from "../../global/types/date.utils";


@Injectable({
  providedIn: 'root'
})
export class CommissionService {

  private apiUrl = `${environment.apiUrl}commissions`;

  constructor(private http: HttpClient) {
  }

  // --- Create ---
  createCommission(requestDto: CommissionRequestDto): Observable<CommissionResponseDto> {
    return this.http.post<CommissionResponseDto>(this.apiUrl, requestDto);
  }

  // --- Get by ID ---
  getCommissionById(id: number): Observable<CommissionResponseDto> {
    return this.http.get<CommissionResponseDto>(`${this.apiUrl}/${id}`);
  }

  // --- Search with pagination + aggregate total ---
  searchCommissions(
    searchDto: CommissionSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ): Observable<EncoursCommissionDto> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return this.http.post<EncoursCommissionDto>(`${this.apiUrl}/search`, this.toSearchPayload(searchDto), { params });
  }

  /**
   * Convertit les champs {@code Date} de {@link CommissionSearchDto} en chaînes `yyyy-MM-dd` avant
   * envoi en corps JSON — un objet {@code Date} sérialisé nativement produit un datetime ISO
   * complet que {@code LocalDate} côté backend ne sait pas parser.
   */
  private toSearchPayload(searchDto: CommissionSearchDto): any {
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
  updateCommission(id: number, requestDto: CommissionRequestDto): Observable<CommissionResponseDto> {
    return this.http.put<CommissionResponseDto>(`${this.apiUrl}/${id}`, requestDto);
  }

  // --- Delete ---
  deleteCommission(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  toggleCommissionStatus(id: number): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/toggle-status`, {});
  }

  // --- Fusion ---
  fusionner(request: CommissionFusionRequest): Observable<CommissionFusionResponse> {
    return this.http.post<CommissionFusionResponse>(`${this.apiUrl}/fusionner`, request);
  }

  /**
   * Exporte en PDF l'intégralité des commissions correspondant aux filtres fournis
   * (mêmes critères que {@link searchCommissions}, sans pagination — le PDF contient
   * tous les résultats filtrés, pas seulement la page actuellement affichée).
   */
  exportPdf(searchDto: CommissionSearchDto): Observable<Blob> {
    let params = new HttpParams();

    if (searchDto.banqueId != null) params = params.set('banqueId', searchDto.banqueId.toString());
    if (searchDto.libelle)          params = params.set('libelle', searchDto.libelle);
    if (searchDto.operation)        params = params.set('operation', searchDto.operation);
    if (searchDto.montant != null)  params = params.set('montant', searchDto.montant.toString());
    if (searchDto.status)           params = params.set('status', searchDto.status);
    if (searchDto.natureId != null) params = params.set('natureId', searchDto.natureId.toString());
    params = this.appendDateRangeParams(params, searchDto);

    return this.http.get(`${this.apiUrl}/export/pdf`, { params, responseType: 'blob' });
  }

  /** Ajoute les 3 plages de dates (émission, opération, valeur) si renseignées. */
  private appendDateRangeParams(params: HttpParams, searchDto: CommissionSearchDto): HttpParams {
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
