import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CmtConfigRequest } from '../dtos/CmtConfigRequest';
import { CmtConfigResponse } from '../dtos/CmtConfigResponse';
import { environment } from '../../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class CmtConfigService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}cmt-config`;

  getConfig(): Observable<CmtConfigResponse> {
    return this.http.get<CmtConfigResponse>(this.apiUrl);
  }

  update(dto: CmtConfigRequest): Observable<CmtConfigResponse> {
    return this.http.put<CmtConfigResponse>(this.apiUrl, dto);
  }
}
