import {Injectable} from "@angular/core";
import {environment} from "../../../../environments/environment";
import {HttpClient, HttpParams} from "@angular/common/http";
import {PrelevementRequestDto} from "../dtos/PrelevementRequestDto";
import {Observable} from "rxjs";
import {PrelevementResponseDto} from "../dtos/PrelevementResponseDto";
import {PageResponse} from "../../global/types/PageResponse";
import {PrelevementSearchDto} from "../dtos/PrelevementSearchDto";
import {EncoursPrelevement} from "../dtos/EncoursPrelevement";


@Injectable({
  providedIn: 'root'
})
export class PrelevementService {


  private readonly apiUrl = `${environment.apiUrl}prelevements`;

  constructor(private http: HttpClient) {}

  createPrelevement(requestDto: PrelevementRequestDto): Observable<PrelevementResponseDto> {
    return this.http.post<PrelevementResponseDto>(this.apiUrl, requestDto);
  }


  getById(id: number): Observable<PrelevementResponseDto> {
    return this.http.get<PrelevementResponseDto>(`${this.apiUrl}/${id}`);
  }


  searchPrelevements(
    searchDto: PrelevementSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<PageResponse<PrelevementResponseDto>> {

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return this.http.post<PageResponse<PrelevementResponseDto>>(`${this.apiUrl}/search`, searchDto, { params });
  }

  updatePrelevement(id: number, requestDto: PrelevementRequestDto): Observable<PrelevementResponseDto> {
    return this.http.put<PrelevementResponseDto>(`${this.apiUrl}/${id}`, requestDto);
  }


  deletePrelevement(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }


  getOutstandingPrelevement(
    banqueId: number,
    searchDto?: PrelevementSearchDto,
    page: number = 0,
    size: number = 10,
    sort: string = 'dateEmission,desc'
  ): Observable<EncoursPrelevement> {

    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (searchDto) {
      Object.entries(searchDto).forEach(([key, value]) => {
        if (value !== null && value !== undefined && value !== '') {
          params = params.set(key, String(value));
        }
      });
    }

    return this.http.get<EncoursPrelevement>(`${this.apiUrl}/encours/${banqueId}`, { params });
  }
}
