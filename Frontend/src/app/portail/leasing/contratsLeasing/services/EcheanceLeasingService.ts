import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from "../../../../../environments/environment";
import { EcheanceLeasingResponse } from "../dtos/EcheanceLeasingResponse";
import { EcheanceLeasingUpdateRequest } from "../dtos/EcheanceLeasingUpdateRequest";
import { TableauAmortissement } from "../dtos/table-amortissement";
import { EcheanceLeasingSearchDTO } from "../dtos/echeance-leasing-search";
import { toLocalDateString } from "../../../global/types/date.utils";
import { AvanceLeasingResponse, AvanceMoyenPaiementRequest, MoyenPaiementAvance } from "../dtos/avance-leasing";


@Injectable({
  providedIn: 'root'
})
export class EcheanceLeasingService {

  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}echeances-leasing`;

  /**
   * Fetches the amortization table for a given contract ID.
   * Supports pagination, sorting, and search filters for individual installments.
   */
  getTableauAmortissementForContrat(
    contratId: number,
    searchCriteria?: EcheanceLeasingSearchDTO,
    page: number = 0,
    size: number = 10,
    sortBy?: string,
    sortDirection?: 'asc' | 'desc'
  ): Observable<TableauAmortissement> {
    let params = this.buildFilterParams(searchCriteria);

    params = params.set('page', page.toString());
    params = params.set('size', size.toString());
    if (sortBy && sortDirection) {
      params = params.set('sort', `${sortBy},${sortDirection}`);
    } else if (sortBy) {
      params = params.set('sort', sortBy);
    }

    const url = `${this.apiUrl}/tableau-amortissement/${contratId}`;
    return this.http.get<TableauAmortissement>(url, { params });
  }

  /**
   * Exporte en PDF le tableau d'amortissement complet d'un contrat (mêmes critères que
   * {@link getTableauAmortissementForContrat}, sans pagination).
   */
  exportTableauAmortissementPdf(contratId: number, searchCriteria?: EcheanceLeasingSearchDTO): Observable<Blob> {
    const params = this.buildFilterParams(searchCriteria);
    const url = `${this.apiUrl}/tableau-amortissement/${contratId}/export/pdf`;
    return this.http.get(url, { params, responseType: 'blob' });
  }

  /**
   * Construit les paramètres de filtre (sans pagination) à partir du DTO de recherche.
   */
  private buildFilterParams(searchCriteria?: EcheanceLeasingSearchDTO): HttpParams {
    let params = new HttpParams();
    if (!searchCriteria) return params;

    if (searchCriteria.numeroEcheance != null) {
      params = params.set('numeroEcheance', searchCriteria.numeroEcheance.toString());
    }
    if (searchCriteria.statutPaiement != null) {
      params = params.set('statutPaiement', searchCriteria.statutPaiement);
    }
    if (searchCriteria.dateEcheanceDebut) {
      params = params.set('dateEcheanceDebut', toLocalDateString(searchCriteria.dateEcheanceDebut)!);
    }
    if (searchCriteria.dateEcheanceFin) {
      params = params.set('dateEcheanceFin', toLocalDateString(searchCriteria.dateEcheanceFin)!);
    }
    if (searchCriteria.dateOperationDebut) {
      params = params.set('dateOperationDebut', toLocalDateString(searchCriteria.dateOperationDebut)!);
    }
    if (searchCriteria.dateOperationFin) {
      params = params.set('dateOperationFin', toLocalDateString(searchCriteria.dateOperationFin)!);
    }
    if (searchCriteria.dateValeurDebut) {
      params = params.set('dateValeurDebut', toLocalDateString(searchCriteria.dateValeurDebut)!);
    }
    if (searchCriteria.dateValeurFin) {
      params = params.set('dateValeurFin', toLocalDateString(searchCriteria.dateValeurFin)!);
    }

    return params;
  }

  /**
   * Récupère une échéance de leasing par son identifiant.
   * Utilisé par l'écran de récapitulatif unifié pour résoudre le contrat de leasing
   * (contratLeasingId, numéro de contrat, montants HT/TVA) avant l'ouverture du
   * formulaire de validation — sans dupliquer cette logique côté frontend.
   */
  getEcheanceLeasingById(id: number): Observable<EcheanceLeasingResponse> {
    return this.http.get<EcheanceLeasingResponse>(`${this.apiUrl}/${id}`);
  }

  /** Avance (échéance n°1) d'un contrat et son moyen de paiement — réservé aux contrats qui ont une avance. */
  getAvance(contratId: number): Observable<AvanceLeasingResponse> {
    return this.http.get<AvanceLeasingResponse>(`${this.apiUrl}/avance/${contratId}`);
  }

  /** Décaissements non annulés du montant exact de l'avance et pas encore rattachés à une avance. */
  getMoyensPaiementDisponibles(contratId: number): Observable<MoyenPaiementAvance[]> {
    return this.http.get<MoyenPaiementAvance[]>(`${this.apiUrl}/avance/${contratId}/moyens-disponibles`);
  }

  /** Rattache un chèque, une LCN ou un virement à l'avance du contrat. */
  rattacherMoyenPaiement(contratId: number, dto: AvanceMoyenPaiementRequest): Observable<AvanceLeasingResponse> {
    return this.http.put<AvanceLeasingResponse>(`${this.apiUrl}/avance/${contratId}/moyen-paiement`, dto);
  }

  /** Détache le moyen de paiement de l'avance (refusé s'il est déjà réglé). */
  detacherMoyenPaiement(contratId: number): Observable<AvanceLeasingResponse> {
    return this.http.delete<AvanceLeasingResponse>(`${this.apiUrl}/avance/${contratId}/moyen-paiement`);
  }

  /**
   * Marks a specific EcheanceLeasing installment as paid.
   */
  toggleStatus(id: number): Observable<EcheanceLeasingResponse> {
    const url = `${this.apiUrl}/${id}/toggle-status`;
    return this.http.put<EcheanceLeasingResponse>(url, {});
  }

  /**
   * Met à jour une échéance au statut PREVU (admin uniquement).
   * Bloqué côté backend si le statut est PAYE.
   */
  updateEcheance(id: number, dto: EcheanceLeasingUpdateRequest): Observable<EcheanceLeasingResponse> {
    return this.http.put<EcheanceLeasingResponse>(`${this.apiUrl}/${id}`, dto);
  }
}
