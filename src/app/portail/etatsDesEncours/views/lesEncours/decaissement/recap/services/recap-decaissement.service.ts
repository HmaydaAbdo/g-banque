import { Injectable } from '@angular/core';
import {environment} from "../../../../../../../../environments/environment";
import {HttpClient, HttpParams} from "@angular/common/http";
import {Observable} from "rxjs";
import {PageResponse} from "../../../../../../global/types/PageResponse";
import {RecaputilatifDecaissementItem} from "../dtos/RecaputilatifDecaissementItem";

@Injectable({
  providedIn: 'root'
})
export class RecapDecaissementService {
  private readonly baseUrl = `${environment.apiUrl}recapitulatif-decaissement`;

  constructor(private http: HttpClient) { }

  /**
   * Retrieves a paginated and sortable list of outstanding disbursements for a specific bank.
   *
   * @param banqueId The ID of the bank to filter by.
   * @param page The page number to retrieve (default: 0).
   * @param size The number of items per page (default: 10).
   * @param sort The sorting criteria (e.g., 'dateEmission,asc' or 'debit,desc').
   * @returns Observable<Page<RecaputilatifDecaissementItem>> An observable of the pageable response.
   */
  getRecapitulatifDecaissement(
    banqueId: number,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,asc'
  ): Observable<PageResponse<RecaputilatifDecaissementItem>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    // Construct the URL with the banqueId as a path variable
    const url = `${this.baseUrl}/banque/${banqueId}`;

    return this.http.get<PageResponse<RecaputilatifDecaissementItem>>(url, { params });
  }
}
