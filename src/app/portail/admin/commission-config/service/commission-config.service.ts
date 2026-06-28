import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CommissionConfigRequest } from '../dtos/CommissionConfigRequest';
import { CommissionConfigResponse } from '../dtos/CommissionConfigResponse';
import {environment} from "../../../../../environments/environment";

@Injectable({ providedIn: 'root' })
export class CommissionConfigService {
  private http   = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}commission-config`;

  findAll(): Observable<CommissionConfigResponse[]> {
    return this.http.get<CommissionConfigResponse[]>(this.apiUrl);
  }

  create(dto: CommissionConfigRequest): Observable<CommissionConfigResponse> {
    return this.http.post<CommissionConfigResponse>(this.apiUrl, dto);
  }

  update(id: number, dto: CommissionConfigRequest): Observable<CommissionConfigResponse> {
    return this.http.put<CommissionConfigResponse>(`${this.apiUrl}/${id}`, dto);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
