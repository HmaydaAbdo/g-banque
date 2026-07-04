import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LeasingConfigRequest } from '../dtos/LeasingConfigRequest';
import { LeasingConfigResponse } from '../dtos/LeasingConfigResponse';
import { environment } from '../../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class LeasingConfigService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}leasing-config`;

  getConfig(): Observable<LeasingConfigResponse> {
    return this.http.get<LeasingConfigResponse>(this.apiUrl);
  }

  update(dto: LeasingConfigRequest): Observable<LeasingConfigResponse> {
    return this.http.put<LeasingConfigResponse>(this.apiUrl, dto);
  }
}
