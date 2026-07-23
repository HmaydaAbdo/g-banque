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

    return this.http.post<EncoursCommissionDto>(`${this.apiUrl}/search`, searchDto, { params });
  }

  // --- Update ---
  updateCommission(id: number, requestDto: CommissionRequestDto): Observable<CommissionResponseDto> {
    return this.http.put<CommissionResponseDto>(`${this.apiUrl}/${id}`, requestDto);
  }

  // --- Delete ---
  deleteCommission(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // --- Get outstanding commissions ---
  getOutstandingCommissions(
    banqueId: number,
    searchDto: CommissionSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission'
  ): Observable<EncoursCommissionDto> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    // Ajouter les propriétés de CommissionSearchDto aux paramètres de la requête
    if (searchDto.libelle) {
      params = params.set('libelle', searchDto.libelle);
    }

    if(searchDto.operation){
      params = params.set('operation', searchDto.operation);
    }

    if (searchDto.montant) {
      params = params.set('montant', searchDto.montant.toString());
    }
    // Ajoutez ici d'autres propriétés si nécessaire (e.g., dateDebut, dateFin, etc.)

    return this.http.get<EncoursCommissionDto>(`${this.apiUrl}/encours/${banqueId}`, { params });
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

    return this.http.get(`${this.apiUrl}/export/pdf`, { params, responseType: 'blob' });
  }

  /**
   * Exporte en PDF l'intégralité de l'encours de commissions d'une banque correspondant aux
   * filtres fournis (mêmes critères que {@link getOutstandingCommissions}, sans pagination — le
   * PDF contient tous les résultats filtrés, pas seulement la page actuellement affichée).
   */
  exportEncoursPdf(banqueId: number, searchDto: CommissionSearchDto): Observable<Blob> {
    let params = new HttpParams();

    if (searchDto.libelle)         params = params.set('libelle', searchDto.libelle);
    if (searchDto.operation)       params = params.set('operation', searchDto.operation);
    if (searchDto.montant != null) params = params.set('montant', searchDto.montant.toString());

    return this.http.get(`${this.apiUrl}/encours/${banqueId}/export/pdf`, { params, responseType: 'blob' });
  }
}
