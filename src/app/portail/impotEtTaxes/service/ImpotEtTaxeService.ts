import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ImpotEtTaxeRequestDto } from "../dtos/ImpotEtTaxeRequestDto";
import { ImpotEtTaxeResponseDto } from "../dtos/ImpotEtTaxeResponseDto";
import { ImpotEtTaxeSearchDto } from "../dtos/ImpotEtTaxeSearchDto";
import { EncoursImpotEtTaxeDto } from "../dtos/EncoursImpotEtTaxeDto";
import { ImpotEtTaxeFusionRequest } from "../dtos/ImpotEtTaxeFusionRequest";

@Injectable({
  providedIn: 'root'
})
export class ImpotEtTaxeService {

  private apiUrl = `${environment.apiUrl}impots-taxes`;
  private readonly http = inject(HttpClient);

  // --- Create ---
  createImpotEtTaxe(requestDto: ImpotEtTaxeRequestDto): Observable<ImpotEtTaxeResponseDto> {
    return this.http.post<ImpotEtTaxeResponseDto>(this.apiUrl, requestDto);
  }

  // --- Get by ID ---
  getImpotEtTaxeById(id: number): Observable<ImpotEtTaxeResponseDto> {
    return this.http.get<ImpotEtTaxeResponseDto>(`${this.apiUrl}/${id}`);
  }

  // --- Search with pagination + aggregate total ---
  searchImpotEtTaxes(
    searchDto: ImpotEtTaxeSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ): Observable<EncoursImpotEtTaxeDto> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return this.http.post<EncoursImpotEtTaxeDto>(`${this.apiUrl}/search`, searchDto, { params });
  }

  // --- Update ---
  updateImpotEtTaxe(id: number, requestDto: ImpotEtTaxeRequestDto): Observable<ImpotEtTaxeResponseDto> {
    return this.http.put<ImpotEtTaxeResponseDto>(`${this.apiUrl}/${id}`, requestDto);
  }

  // --- Delete ---
  deleteImpotEtTaxe(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // --- Get outstanding impots et taxes ---
  getOutstandingImpotEtTaxes(
    banqueId: number,
    searchDto: ImpotEtTaxeSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission'
  ): Observable<EncoursImpotEtTaxeDto> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (searchDto.libelle) {
      params = params.set('libelle', searchDto.libelle);
    }

    if (searchDto.motif) {
      params = params.set('motif', searchDto.motif);
    }

    if (searchDto.montant) {
      params = params.set('montant', searchDto.montant.toString());
    }

    return this.http.get<EncoursImpotEtTaxeDto>(`${this.apiUrl}/encours/${banqueId}`, { params });
  }

  toggleStatus(id: number): Observable<ImpotEtTaxeResponseDto> {
    return this.http.patch<ImpotEtTaxeResponseDto>(`${this.apiUrl}/${id}/toggle-status`, {});
  }

  // --- Fusion ---
  fusionner(request: ImpotEtTaxeFusionRequest): Observable<ImpotEtTaxeResponseDto> {
    return this.http.post<ImpotEtTaxeResponseDto>(`${this.apiUrl}/fusionner`, request);
  }

  /**
   * Exporte en PDF l'intégralité des impôts/taxes correspondant aux filtres fournis (mêmes
   * critères que {@link searchImpotEtTaxes}, sans pagination — le PDF contient tous les résultats
   * filtrés, pas seulement la page actuellement affichée).
   */
  exportPdf(searchDto: ImpotEtTaxeSearchDto): Observable<Blob> {
    let params = new HttpParams();

    if (searchDto.banqueId != null) params = params.set('banqueId', searchDto.banqueId.toString());
    if (searchDto.libelle)          params = params.set('libelle', searchDto.libelle);
    if (searchDto.motif)            params = params.set('motif', searchDto.motif);
    if (searchDto.montant != null)  params = params.set('montant', searchDto.montant.toString());
    if (searchDto.status)           params = params.set('status', searchDto.status);

    return this.http.get(`${this.apiUrl}/export/pdf`, { params, responseType: 'blob' });
  }
}
