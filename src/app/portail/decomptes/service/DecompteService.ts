import {Injectable} from "@angular/core";
import {environment} from "../../../../environments/environment";
import {HttpClient, HttpParams} from "@angular/common/http";
import {DroitConstateRequestDto} from "../../droitsConstate/dtos/DroitConstateRequestDto";
import {Observable} from "rxjs";
import {DroitConstateResponseDto} from "../../droitsConstate/dtos/DroitConstateResponseDto";
import {DroitConstateSearchDto} from "../../droitsConstate/dtos/DroitConstateSearchDto";
import {PageResponse} from "../../global/types/PageResponse";
import {EncoursDroitConstateDto} from "../../droitsConstate/dtos/EncoursDroitConstateDto";
import {DecompteRequestDto} from "../dtos/DecompteRequestDto";
import {DecompteResponseDto} from "../dtos/DecompteResponseDto";
import {DecompteSearchDto} from "../dtos/DecompteSearchDto";
import {EncoursDecompteDto} from "../dtos/EncoursDecompteDto";
import {EncoursDecompteSearchDto} from "../dtos/EncoursDecompteSearchDto";

@Injectable({
  providedIn: 'root',
})

export class DecompteService {


  private apiUrl = `${environment.apiUrl}decomptes`;

  constructor(private http: HttpClient) {}


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

    // Append search criteria to params if they exist
    if (searchCriteria.marcheId) {
      params = params.set('marcheId', searchCriteria.marcheId.toString());
    }
    if (searchCriteria.banqueId) {
      params = params.set('banqueId', searchCriteria.banqueId.toString());
    }
    if (searchCriteria.status) {
      params = params.set('status', searchCriteria.status);
    }

    return this.http.get<PageResponse<DecompteResponseDto>>(this.apiUrl, { params });
  }

  /**
   * Changes the status of a Décompte record to 'PAID'.
   *
   * @param id The ID of the record to update.
   * @returns An Observable of the updated record's response DTO.
   */
  markAsPaid(id: number): Observable<DecompteResponseDto> {
    return this.http.patch<DecompteResponseDto>(`${this.apiUrl}/${id}/status/paid`, {});
  }

  /**
   * Retrieves the encours (in progress) décomptes records for a specific banque.
   *
   * @param banqueId The ID of the banque.
   * @param searchDto
   * @param page The page number (0-indexed).
   * @param size The number of elements per page.
   * @param sort The sorting string (e.g., 'dateEmission,asc').
   * @returns An Observable of a paginated list of décomptes response DTOs.
   */
  getEncoursDecomptes(
    banqueId: number,
    searchDto:EncoursDecompteSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,asc'
  ): Observable<EncoursDecompteDto> {

    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if(searchDto.marcheName){
      params = params.set('marcheName', searchDto.marcheName);
    }

    if (searchDto.marcheNumero) {
      params = params.set('marcheNumero', searchDto.marcheNumero);
    }

    if(searchDto.montant){
      params = params.set('montant', searchDto.montant.toString());
    }

    return this.http.get<EncoursDecompteDto>(
      `${this.apiUrl}/encours/${banqueId}`,
      { params }
    );
  }


}
