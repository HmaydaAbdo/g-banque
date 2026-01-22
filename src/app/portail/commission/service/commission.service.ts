import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../../../environments/environment';
import {CommissionRequestDto} from "../dtos/CommissionRequestDto";
import {CommissionResponseDto} from "../dtos/commission-response.dto";
import {CommissionSearchDto} from "../dtos/CommissionSearchDto";
import {PageResponse} from "../../global/types/PageResponse";
import {EncoursCommissionDto} from "../dtos/EncoursCommissionDto";


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

  // --- Search with pagination ---
  searchCommissions(
    searchDto: CommissionSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ): Observable<PageResponse<CommissionResponseDto>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return this.http.post<PageResponse<CommissionResponseDto>>(`${this.apiUrl}/search`, searchDto, {params});
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
}
