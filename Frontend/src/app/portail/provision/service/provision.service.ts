import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ProvisionResponseDto } from "../dtos/ProvisionResponseDto";
import { ProvisionSearchDto } from "../dtos/ProvisionSearchDto";
import { ProvisionRequestDto } from "../dtos/ProvisionRequestDto";
import { environment } from "../../../../environments/environment";
import { toLocalDateString } from "../../global/types/date.utils";

@Injectable({
  providedIn: 'root'
})
export class ProvisionService {
  private apiUrl = `${environment.apiUrl}provision`;

  constructor(private http: HttpClient) {
  }

  /**
   * Créer une nouvelle provision
   * POST /provision
   */
  createProvision(dto: ProvisionRequestDto): Observable<ProvisionResponseDto> {
    return this.http.post<ProvisionResponseDto>(this.apiUrl, dto);
  }

  /**
   * Récupérer une provision par ID
   * GET /provision/{id}
   */
  getById(id: number): Observable<ProvisionResponseDto> {
    return this.http.get<ProvisionResponseDto>(`${this.apiUrl}/${id}`);
  }

  /**
   * Rechercher des provisions avec pagination
   * POST /provision/search
   */
  searchProvision(
    searchDto: ProvisionSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return this.http.post<any>(`${this.apiUrl}/search`, this.toSearchPayload(searchDto), { params });
  }

  /**
   * Convertit les champs {@code Date} de {@link ProvisionSearchDto} en chaînes `yyyy-MM-dd` avant
   * envoi en corps JSON — un objet {@code Date} sérialisé nativement produit un datetime ISO
   * complet que {@code LocalDate} côté backend ne sait pas parser.
   */
  private toSearchPayload(searchDto: ProvisionSearchDto): any {
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
   * Mettre à jour une provision
   * PUT /provision/{id}
   */
  updateProvision(id: number, dto: ProvisionRequestDto): Observable<ProvisionResponseDto> {
    return this.http.put<ProvisionResponseDto>(`${this.apiUrl}/${id}`, dto);
  }

  /**
   * Supprimer une provision
   * DELETE /provision/{id}
   */
  deleteProvision(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Basculer le statut d'une provision (PREVU <-> PAYE)
   * PATCH /provision/{id}/toggle-status
   */
  toggleStatus(id: number): Observable<ProvisionResponseDto> {
    return this.http.patch<ProvisionResponseDto>(
      `${this.apiUrl}/${id}/toggle-status`,
      {}
    );
  }

  /**
   * Exporte en PDF l'intégralité des provisions correspondant aux filtres fournis (mêmes critères
   * que {@link searchProvision}, sans pagination).
   * GET /provision/export/pdf
   */
  exportPdf(searchDto: ProvisionSearchDto): Observable<Blob> {
    let params = new HttpParams();

    if (searchDto.banqueId != null) params = params.set('banqueId', searchDto.banqueId.toString());
    if (searchDto.libelle)          params = params.set('libelle', searchDto.libelle);
    if (searchDto.montant != null)  params = params.set('montant', searchDto.montant.toString());
    if (searchDto.status)           params = params.set('status', searchDto.status);
    if (searchDto.motif)            params = params.set('motif', searchDto.motif);
    if (searchDto.sens)             params = params.set('sens', searchDto.sens);
    params = this.appendDateRangeParams(params, searchDto);

    return this.http.get(`${this.apiUrl}/export/pdf`, { params, responseType: 'blob' });
  }

  /** Ajoute les 3 plages de dates (émission, opération, valeur) si renseignées. */
  private appendDateRangeParams(params: HttpParams, searchDto: ProvisionSearchDto): HttpParams {
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
