import {Injectable, inject} from "@angular/core";
import {environment} from "../../../../environments/environment";
import {HttpClient, HttpParams} from "@angular/common/http";
import {Observable} from "rxjs";
import {PageResponse} from "../../global/types/PageResponse";
import {DecompteRequestDto} from "../dtos/DecompteRequestDto";
import {DecompteResponseDto} from "../dtos/DecompteResponseDto";
import {DecompteSearchDto} from "../dtos/DecompteSearchDto";

@Injectable({
  providedIn: 'root',
})

export class DecompteService {

  private readonly http = inject(HttpClient);

  private apiUrl = `${environment.apiUrl}decomptes`;


  /**
   * Creates a new Decompte record.
   *
   * @param request The DTO containing the data for the new record.
   * @returns An Observable of the newly created record's response DTO.
   */
  createDecompte(request: DecompteRequestDto): Observable<DecompteResponseDto> {
    return this.http.post<DecompteResponseDto>(this.apiUrl, request);
  }

  /**
   * Updates an existing Décompte record by its ID.
   *
   * @param id The ID of the record to update.
   * @param request The DTO containing the updated data.
   * @returns An Observable of the updated record's response DTO.
   */
  updateDecompte(id: number, request: DecompteRequestDto): Observable<DecompteResponseDto> {
    return this.http.put<DecompteResponseDto>(`${this.apiUrl}/${id}`, request);
  }

  /**
   * Deletes a Décompte record by its ID.
   *
   * @param id The ID of the record to delete.
   * @returns An Observable of void.
   */
  deleteDecompte(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
     * Retrieves a Décompte record by its ID.
   *
   * @param id The ID of the record to retrieve.
   * @returns An Observable of the Décompte response DTO.
   */
  getDecompteById(id: number): Observable<DecompteResponseDto> {
    return this.http.get<DecompteResponseDto>(`${this.apiUrl}/${id}`);
  }

  /**
   * Searches for Décomptes records with pagination, sorting, and optional criteria.
   *
   * @param searchCriteria The DTO containing the search criteria.
   * @param page The page number (0-indexed).
   * @param size The number of elements per page.
   * @param sort The sorting string (e.g., 'id,asc').
   * @returns An Observable of a paginated list of Décomptes response DTOs.
   */
  getAllDecomptes(
    searchCriteria: DecompteSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ): Observable<PageResponse<DecompteResponseDto>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (searchCriteria.marcheId)
      params = params.set('marcheId', searchCriteria.marcheId.toString());
    if (searchCriteria.banqueId)
      params = params.set('banqueId', searchCriteria.banqueId.toString());
    if (searchCriteria.status)
      params = params.set('status', searchCriteria.status);
    if (searchCriteria.dateEmissionFrom)
      params = params.set('dateEmissionFrom', this.toIsoDate(searchCriteria.dateEmissionFrom));
    if (searchCriteria.dateEmissionTo)
      params = params.set('dateEmissionTo', this.toIsoDate(searchCriteria.dateEmissionTo));
    if (searchCriteria.dateOperationFrom)
      params = params.set('dateOperationFrom', this.toIsoDate(searchCriteria.dateOperationFrom));
    if (searchCriteria.dateOperationTo)
      params = params.set('dateOperationTo', this.toIsoDate(searchCriteria.dateOperationTo));
    if (searchCriteria.dateValeurFrom)
      params = params.set('dateValeurFrom', this.toIsoDate(searchCriteria.dateValeurFrom));
    if (searchCriteria.dateValeurTo)
      params = params.set('dateValeurTo', this.toIsoDate(searchCriteria.dateValeurTo));
    if (searchCriteria.montant != null)
      params = params.set('montant', searchCriteria.montant.toString());

    return this.http.get<PageResponse<DecompteResponseDto>>(this.apiUrl, { params });
  }


  /** Formate un objet Date en chaîne ISO yyyy-MM-dd pour Spring */
  private toIsoDate(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  exportPdf(searchCriteria: DecompteSearchDto): Observable<Blob> {
    let params = new HttpParams();
    if (searchCriteria.marcheId)
      params = params.set('marcheId', searchCriteria.marcheId.toString());
    if (searchCriteria.banqueId)
      params = params.set('banqueId', searchCriteria.banqueId.toString());
    if (searchCriteria.status)
      params = params.set('status', searchCriteria.status);
    if (searchCriteria.dateEmissionFrom)
      params = params.set('dateEmissionFrom', this.toIsoDate(searchCriteria.dateEmissionFrom));
    if (searchCriteria.dateEmissionTo)
      params = params.set('dateEmissionTo', this.toIsoDate(searchCriteria.dateEmissionTo));
    if (searchCriteria.dateOperationFrom)
      params = params.set('dateOperationFrom', this.toIsoDate(searchCriteria.dateOperationFrom));
    if (searchCriteria.dateOperationTo)
      params = params.set('dateOperationTo', this.toIsoDate(searchCriteria.dateOperationTo));
    if (searchCriteria.dateValeurFrom)
      params = params.set('dateValeurFrom', this.toIsoDate(searchCriteria.dateValeurFrom));
    if (searchCriteria.dateValeurTo)
      params = params.set('dateValeurTo', this.toIsoDate(searchCriteria.dateValeurTo));
    if (searchCriteria.montant != null)
      params = params.set('montant', searchCriteria.montant.toString());
    return this.http.get(`${this.apiUrl}/export/pdf`, { params, responseType: 'blob' });
  }

  toggleStatus(id: number): Observable<DecompteResponseDto> {
    return this.http.patch<DecompteResponseDto>(`${this.apiUrl}/${id}/toggle`, {});
  }

}
