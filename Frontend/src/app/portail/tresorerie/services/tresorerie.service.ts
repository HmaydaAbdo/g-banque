import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { HorizonPrevision } from '../../etatsDesEncours/views/lesEncours/recap/dtos/PeriodeRecap';
import { PlanTresorerie } from '../dtos/PlanTresorerie';

/** Accès au backend de la Trésorerie prévisionnelle (/tresorerie-previsionnelle). */
@Injectable({
  providedIn: 'root'
})
export class TresorerieService {

  private readonly baseUrl = `${environment.apiUrl}tresorerie-previsionnelle`;
  private readonly http = inject(HttpClient);

  /**
   * Plan de toutes les banques (détail par banque + un total par devise) jusqu'à l'horizon demandé,
   * avec ou sans les estimations.
   */
  getPlan(periode: HorizonPrevision, certainesSeulement = false): Observable<PlanTresorerie> {
    const params = new HttpParams().set('periode', periode).set('certainesSeulement', certainesSeulement);
    return this.http.get<PlanTresorerie>(this.baseUrl, { params });
  }

  /** PDF du plan : la banque demandée, ou, sans banque, le total de la devise (le dirham par défaut). */
  exportPdf(periode: HorizonPrevision, certainesSeulement: boolean, banqueId: number | null, devise?: string): Observable<Blob> {
    let params = new HttpParams().set('periode', periode).set('certainesSeulement', certainesSeulement);
    if (banqueId !== null) {
      params = params.set('banqueId', banqueId);
    } else if (devise) {
      params = params.set('devise', devise);
    }
    return this.http.get(`${this.baseUrl}/pdf`, { params, responseType: 'blob' });
  }
}
