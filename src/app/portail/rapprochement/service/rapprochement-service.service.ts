import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {environment} from "../../../../environments/environment";
import {RapprochementRequest} from "../dtos/RapprochementRequest";
import {RapprochementResponse} from "../dtos/RapprochementResponse";
import {RapprochementSearchDto} from "../dtos/RapprochementSearchDto";
import {PageResponse} from "../../global/types/PageResponse";

@Injectable({
  providedIn: 'root'
})
export class RapprochementService {

  private readonly baseUrl = `${environment.apiUrl}rapprochement`;

  constructor(private http: HttpClient) { }


  cloturerRapprochement(requestDto: RapprochementRequest): Observable<RapprochementResponse> {
    return this.http.post<RapprochementResponse>(
      `${this.baseUrl}/cloturer`,
      requestDto
    );
  }


  getRapprochementById(rapprochementId: number, page: number = 0, size: number = 10): Observable<RapprochementResponse> {
    let params = new HttpParams();
    params = params.append('page', page.toString());
    params = params.append('size', size.toString());

    return this.http.get<RapprochementResponse>(
      `${this.baseUrl}/${rapprochementId}`,
      { params: params }
    );
  }

  searchRapprochements(
    searchDto: RapprochementSearchDto,
    page: number = 0,
    size: number = 10
  ): Observable<PageResponse<RapprochementResponse>> {

    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (searchDto.banqueId) {
      params = params.set('banqueId', searchDto.banqueId.toString());
    }

    if (searchDto.dateRapprochement) {
      params = params.set('dateRapprochement', searchDto.dateRapprochement); // Doit être au format 'MM/YYYY'
    }

    return this.http.get<PageResponse<RapprochementResponse>>(
      this.baseUrl,
      { params: params }
    );
  }

  downloadReport(rapprochementId: number): Observable<Blob> {
    const url = `${this.baseUrl}/${rapprochementId}/report/pdf`;

    return this.http.get(url, {
      responseType: 'blob'
    });
  }
}
