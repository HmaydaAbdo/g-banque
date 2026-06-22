import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { PageResponse } from '../../../global/types/PageResponse';
import { UserResponse } from '../dtos/UserResponse';
import { CreateUserRequest } from '../dtos/CreateUserRequest';
import { UserSearchCriteria } from '../dtos/UserSearchCriteria';
import { UserDepartmentRoleResponse } from '../dtos/UserDepartmentRoleResponse';
import { DepartmentRoleRequest } from '../dtos/DepartmentRoleRequest';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}users`;

  search(
    criteria: UserSearchCriteria,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ): Observable<PageResponse<UserResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (criteria.username) {
      params = params.set('username', criteria.username);
    }
    if (criteria.enabled !== undefined && criteria.enabled !== null) {
      params = params.set('enabled', criteria.enabled.toString());
    }
    if (criteria.systemAdmin !== undefined && criteria.systemAdmin !== null) {
      params = params.set('systemAdmin', criteria.systemAdmin.toString());
    }
    if (criteria.departmentId !== undefined && criteria.departmentId !== null) {
      params = params.set('departmentId', criteria.departmentId.toString());
    }

    return this.http.get<PageResponse<UserResponse>>(this.apiUrl, { params });
  }

  getById(userId: number): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.apiUrl}/${userId}`);
  }

  create(request: CreateUserRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(this.apiUrl, request);
  }

  delete(userId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${userId}`);
  }

  getDepartmentRoles(userId: number): Observable<UserDepartmentRoleResponse[]> {
    return this.http.get<UserDepartmentRoleResponse[]>(`${this.apiUrl}/${userId}/department-roles`);
  }

  assignDepartmentRole(userId: number, request: DepartmentRoleRequest): Observable<UserDepartmentRoleResponse> {
    return this.http.post<UserDepartmentRoleResponse>(`${this.apiUrl}/${userId}/department-roles`, request);
  }

  removeDepartmentRole(userId: number, departmentId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${userId}/department-roles/${departmentId}`);
  }

  setSystemAdmin(userId: number, isSystemAdmin: boolean): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.apiUrl}/${userId}/system-admin`, { isSystemAdmin });
  }

  setEnabled(userId: number, enabled: boolean): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.apiUrl}/${userId}/enabled`, { enabled });
  }

  updatePassword(userId: number, password: string): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${userId}/password`, { password });
  }
}
