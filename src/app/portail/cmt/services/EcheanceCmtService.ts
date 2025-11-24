
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable,  } from 'rxjs';
import {environment} from "../../../../environments/environment";
import {EcheanceCMTResponseDto} from "../dtos/EcheanceCMTResponseDto";
import {TableauAmortissementCMTResponseDto} from "../dtos/TableauAmortissementCMTResponseDto";
import {EcheanceCMTSearchDto} from "../dtos/EcheanceCMTSearchDto";
import {EncoursCMTRequestDTO} from "../dtos/EncoursCMTRequestDTO";
import {EncoursCMTResponseDTO} from "../dtos/EncoursCMTResponseDTO";


@Injectable({
  providedIn: 'root'
})
export class EcheanceCmtService {

  private apiUrl = `${environment.apiUrl}echeances-cmt`;

  constructor(private http: HttpClient) { }



  /**
   * Fetches the amortization table for a specific credit.
   */
  getTableauAmortissementForCredit(
    creditId: number,
    searchDto: EcheanceCMTSearchDto,
    pageable: { page?: number, size?: number, sort?: string } = {}
  ): Observable<TableauAmortissementCMTResponseDto> {
    let params = new HttpParams();

    if (searchDto.statutPaiement) {
      params = params.set('statutPaiement', searchDto.statutPaiement);
    }

    if (searchDto.year) {
      params = params.set('year', searchDto.year.toString());
    }

    if (searchDto.month) {
      params = params.set('month', searchDto.month.toString());
    }

    if (searchDto.numeroEcheance){
      params=params.set('numeroEcheance', searchDto.numeroEcheance.toString());
    }

    params = params.set('page', pageable.page?.toString() || '0');
    params = params.set('size', pageable.size?.toString() || '10');
    params = params.set('sort', pageable.sort || 'id,asc');

    return this.http.get<TableauAmortissementCMTResponseDto>(`${this.apiUrl}/tableau-amortissement/${creditId}`, { params })
  }

  /**
   * Marks a specific installment as paid.
   */
  toggleStatus(id: number): Observable<EcheanceCMTResponseDto> {
    const url = `${this.apiUrl}/${id}/toggle-status`;
    return this.http.put<EcheanceCMTResponseDto>(url, {})
  }

  /**
   * Récupère les encours du crédit moyen terme en fonction des critères de recherche.
   * @param request Le DTO de requête contenant les filtres (banqueId, periode, organismeLeasingId, etc.).
   * @returns Un Observable de l'objet de réponse des encours.
   */
  getEncoursCMT(request: EncoursCMTRequestDTO): Observable<EncoursCMTResponseDTO> {
    let params = new HttpParams();

    // Ajout des paramètres obligatoires
    params = params.append('banqueId', request.banqueId.toString());



    if (request.page !== undefined) {
      params = params.append('page', request.page.toString());
    }
    if (request.size !== undefined) {
      params = params.append('size', request.size.toString());
    }
    if (request.sort) {
      params = params.append('sort', request.sort);
    }

    if( request.designation ){
      params = params.append('designation', request.designation);
    }

    if( request.montant ){
      params = params.append('montant', request.montant.toString());
    }

    return this.http.get<EncoursCMTResponseDTO>(`${this.apiUrl}/encours`, { params });
  }






}
