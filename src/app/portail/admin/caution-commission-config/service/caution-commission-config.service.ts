import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CautionCommissionConfigRequest } from '../dtos/CautionCommissionConfigRequest';
import { CautionCommissionConfigResponse } from '../dtos/CautionCommissionConfigResponse';
import { environment } from '../../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class CautionCommissionConfigService {
  private http   = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}cautions/commission-config`;

  findAll(): Observable<CautionCommissionConfigResponse[]> {
    return this.http.get<CautionCommissionConfigResponse[]>(this.apiUrl);
  }

  create(dto: CautionCommissionConfigRequest): Observable<CautionCommissionConfigResponse> {
    return this.http.post<CautionCommissionConfigResponse>(this.apiUrl, dto);
  }

  update(id: number, dto: CautionCommissionConfigRequest): Observable<CautionCommissionConfigResponse> {
    return this.http.put<CautionCommissionConfigResponse>(`${this.apiUrl}/${id}`, dto);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
