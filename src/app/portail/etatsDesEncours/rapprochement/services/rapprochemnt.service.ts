  import { Injectable } from '@angular/core';
  import {LeasingTransactionRequest} from "../dtos/LeasingTransactionRequest";
  import {HttpClient, HttpParams} from "@angular/common/http";
  import {environment} from "../../../../../environments/environment";
  import {Observable} from "rxjs";
  import {LeasingTransactionResponse} from "../dtos/LeasingTransactionResponse";
  import {CmtTransactionRequest} from "../dtos/CmtTransactionRequest";
  import {CmtTransactionResponseDto} from "../dtos/CmtTransactionResponseDto";
  import {PageResponse} from "../../../global/types/PageResponse";
  import {TransactionResponseDto} from "../dtos/TransactionResponseDto";

  @Injectable({
    providedIn: 'root'
  })
  export class RapprochemntService {


    private readonly baseUrl =  `${environment.apiUrl}rapprochement`

    constructor(private http: HttpClient) { }

    /**
     * Envoie une transaction d'échéance leasing au backend pour rapprochement
     * @param leasingTransactionRequest Les données de la transaction à rapprocher
     * @returns Observable<LeasingTransactionResponse>
     */
    echeanceLeasingToRapprochemnt(
      leasingTransactionRequest: LeasingTransactionRequest
    ): Observable<LeasingTransactionResponse> {
      return this.http.post<LeasingTransactionResponse>(`${this.baseUrl}/process/echeance-leasing`,
        leasingTransactionRequest
      );
    }


    /**
     * Envoie une transaction d'échéance CMT au backend pour rapprochement
     * @param cmtTransactionRequest Les données de la transaction à rapprocher
     * @returns Observable<CmtTransactionResponseDto>
     */
    echeanceCmtToRapprochement(
      cmtTransactionRequest: CmtTransactionRequest
    ): Observable<CmtTransactionResponseDto> {
      return this.http.post<CmtTransactionResponseDto>(`${this.baseUrl}/process/echeance-cmt`,
        cmtTransactionRequest
      );
    }

    /**
     * Récupère une liste paginée de transactions pour une banque spécifique.
     * @param banqueId L'ID de la banque.
     * @param libelle Un libellé de recherche optionnel.
     * @param page Le numéro de page (commence à 0).
     * @param size Le nombre d'éléments par page.
     * @param sort La chaîne de tri (ex: 'id,asc').
     * @returns Observable<PageResponse<TransactionResponseDto>>
     */
    getTransactions(
      banqueId: number,
      libelle?: string,
      page: number = 0,
      size: number = 10,
      sort: string = 'createdAt,desc'
    ): Observable<PageResponse<TransactionResponseDto>> {
      let params = new HttpParams()
        .set('page', page.toString())
        .set('size', size.toString())
        .set('sort', sort);

      if (libelle) {
        params = params.set('libelle', libelle);
      }

      return this.http.get<PageResponse<TransactionResponseDto>>(`${this.baseUrl}/${banqueId}/transactions`, { params });
    }

    /**
     * Récupère une liste paginée de transactions pour toutes les banques (récapitulatif bancaire).
     * @param libelle Un libellé de recherche optionnel.
     * @param page Le numéro de page (commence à 0).
     * @param size Le nombre d'éléments par page.
     * @param sort La chaîne de tri (ex: 'id,asc').
     * @returns Observable<PageResponse<TransactionResponseDto>>
     */
    getRecapitulatifBancaire(
      libelle?: string,
      page: number = 0,
      size: number = 10,
      sort: string = 'createdAt,desc'
    ): Observable<PageResponse<TransactionResponseDto>> {
      let params = new HttpParams()
        .set('page', page.toString())
        .set('size', size.toString())
        .set('sort', sort);

      if (libelle) {
        params = params.set('libelle', libelle);
      }

      return this.http.get<PageResponse<TransactionResponseDto>>(`${this.baseUrl}/recapitulatif-bancaire`, { params });
    }
  }
