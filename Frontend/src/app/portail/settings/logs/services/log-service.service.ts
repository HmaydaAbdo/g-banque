import {inject, Injectable} from '@angular/core';
import {environment} from "../../../../../environments/environment";
import {HttpClient, HttpParams} from "@angular/common/http";
import {Observable} from "rxjs";
import {LogSearchCriteria} from "../dtos/LogSearchCriteria";
import { LogResponse } from '../dtos/LogResponse';
import { LogEntryDetailResponse } from '../dtos/LogEntryDetailResponse';
import {PageResponse} from "../../../global/types/PageResponse";

@Injectable({
  providedIn: 'root'
})
export class LogService {

  private apiUrl = `${environment.apiUrl}logs`;
  http=inject(HttpClient);


  /**
   * Récupère une page de logs avec des critères de recherche, pagination et tri.
   * @param criteria Les critères de recherche.
   * @param page L'index de la page (0-basé).
   * @param size Le nombre d'éléments par page.
   * @param sort La chaîne de tri (ex: "timestamp,desc", "userName,asc").
   * @returns Un Observable de Page<LogResponse>.
   */
  getLogs(criteria: LogSearchCriteria, page: number, size: number, sort: string): Observable<PageResponse<LogResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    // Ajoute les paramètres uniquement s'ils ont une valeur définie (non null, non undefined, non chaîne vide)
    if (criteria.userName) {
      params = params.set('userName', criteria.userName);
    }
    if (criteria.operationType) {
      params = params.set('operationType', criteria.operationType);
    }
    if (criteria.entityName) {
      params = params.set('entityName', criteria.entityName);
    }
    if (criteria.timestampStart) {
      params = params.set('timestampStart', criteria.timestampStart as string);
    }
    if (criteria.timestampEnd) {
      params = params.set('timestampEnd', criteria.timestampEnd as string);
    }

    return this.http.get<PageResponse<LogResponse>>(this.apiUrl, { params });
  }
  /**
   * Supprime un log par son ID.
   * @param id L'ID du log à supprimer.
   * @returns Un Observable<void>.
   */
  deleteLog(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }


  /**
   * Récupère le détail champ-par-champ (avant/après) d'un log.
   *
   * Ce détail n'est jamais présent dans la réponse de {@link getLogs} : il est
   * reconstruit à la demande côté serveur à partir de la révision Envers, et
   * doit donc être demandé explicitement à l'ouverture du popup.
   *
   * @param id L'ID du log.
   * @returns Un Observable de liste de détails.
   */
  getLogDetails(id: number): Observable<LogEntryDetailResponse[]> {
    return this.http.get<LogEntryDetailResponse[]>(`${this.apiUrl}/${id}/details`);
  }

  /**
   * Récupère les noms distincts des entités loggées.
   * @returns Un Observable de liste de chaînes.
   */
  getEntityNames(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/entities`);
  }

  getUsernames(): Observable<string[]> {
    return this.http.get<string[]>(`${environment.apiUrl}users/usernames`);
  }
}
