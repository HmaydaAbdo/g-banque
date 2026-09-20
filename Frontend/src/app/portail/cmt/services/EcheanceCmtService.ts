
import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable,  } from 'rxjs';
import {environment} from "../../../../environments/environment";
import {EcheanceCMTResponseDto} from "../dtos/EcheanceCMTResponseDto";
import {TableauAmortissementCMTResponseDto} from "../dtos/TableauAmortissementCMTResponseDto";
import {EcheanceCMTSearchDto} from "../dtos/EcheanceCMTSearchDto";
import {EcheanceCMTUpdateRequest} from "../dtos/EcheanceCMTUpdateRequest";
import {toLocalDateString} from "../../global/types/date.utils";


@Injectable({
  providedIn: 'root'
})
export class EcheanceCmtService {

  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}echeances-cmt`;



  /**
   * Fetches the amortization table for a specific credit.
   */
  getTableauAmortissementForCredit(
    creditId: number,
    searchDto: EcheanceCMTSearchDto,
    pageable: { page?: number, size?: number, sort?: string } = {}
  ): Observable<TableauAmortissementCMTResponseDto> {
    let params = this.buildFilterParams(searchDto);

    params = params.set('page', pageable.page?.toString() || '0');
    params = params.set('size', pageable.size?.toString() || '10');
    params = params.set('sort', pageable.sort || 'id,asc');

    return this.http.get<TableauAmortissementCMTResponseDto>(`${this.apiUrl}/tableau-amortissement/${creditId}`, { params })
  }

  /**
   * Exporte en PDF l'échéancier complet (filtré, sans pagination) d'un crédit à moyen terme,
   * avec les mêmes critères que {@link getTableauAmortissementForCredit}.
   */
  exportTableauAmortissementPdf(creditId: number, searchDto: EcheanceCMTSearchDto): Observable<Blob> {
    const params = this.buildFilterParams(searchDto);
    return this.http.get(`${this.apiUrl}/tableau-amortissement/${creditId}/export/pdf`, { params, responseType: 'blob' });
  }

  /**
   * Construit les paramètres de filtre (sans pagination) à partir du DTO de recherche.
   */
  private buildFilterParams(searchDto: EcheanceCMTSearchDto): HttpParams {
    let params = new HttpParams();

    if (searchDto.statutPaiement) {
      params = params.set('statutPaiement', searchDto.statutPaiement);
    }
    if (searchDto.numeroEcheance) {
      params = params.set('numeroEcheance', searchDto.numeroEcheance.toString());
    }
    if (searchDto.dateEcheanceDebut) {
      params = params.set('dateEcheanceDebut', toLocalDateString(searchDto.dateEcheanceDebut)!);
    }
    if (searchDto.dateEcheanceFin) {
      params = params.set('dateEcheanceFin', toLocalDateString(searchDto.dateEcheanceFin)!);
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
   * Récupère une échéance de crédit à moyen terme par son identifiant.
   * Utilisé par l'écran de récapitulatif unifié pour résoudre le crédit CMT
   * (creditMoyenTermeId, désignation, montant TTC) avant l'ouverture du
   * formulaire de validation — sans dupliquer cette logique côté frontend.
   */
  getEcheanceCMTById(id: number): Observable<EcheanceCMTResponseDto> {
    return this.http.get<EcheanceCMTResponseDto>(`${this.apiUrl}/${id}`);
  }

  /**
   * Marks a specific installment as paid.
   */
  toggleStatus(id: number): Observable<EcheanceCMTResponseDto> {
    const url = `${this.apiUrl}/${id}/toggle-status`;
    return this.http.put<EcheanceCMTResponseDto>(url, {})
  }

  /**
   * Met à jour une échéance au statut PREVU (admin uniquement).
   * Bloqué côté backend si le statut est PAYE.
   */
  updateEcheance(id: number, dto: EcheanceCMTUpdateRequest): Observable<EcheanceCMTResponseDto> {
    return this.http.put<EcheanceCMTResponseDto>(`${this.apiUrl}/${id}`, dto);
  }



}
