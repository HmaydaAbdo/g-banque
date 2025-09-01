  import { Injectable } from '@angular/core';
  import {LeasingTransactionRequest} from "../../dtos/LeasingTransactionRequest";
  import {HttpClient, HttpParams} from "@angular/common/http";
  import {environment} from "../../../../../../environments/environment";
  import {Observable} from "rxjs";
  import {LeasingTransactionResponse} from "../../dtos/LeasingTransactionResponse";
  import {CmtTransactionRequest} from "../../dtos/CmtTransactionRequest";
  import {CmtTransactionResponseDto} from "../../dtos/CmtTransactionResponseDto";
  import {PageResponse} from "../../../../global/types/PageResponse";
  import {TransactionResponseDto} from "../../dtos/TransactionResponseDto";

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




  }
