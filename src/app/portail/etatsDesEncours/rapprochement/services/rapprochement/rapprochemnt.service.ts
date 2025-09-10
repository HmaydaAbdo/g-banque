  import { Injectable } from '@angular/core';
  import {LeasingTransactionRequest} from "../../dtos/leasing/LeasingTransactionRequest";
  import {HttpClient, HttpParams} from "@angular/common/http";
  import {environment} from "../../../../../../environments/environment";
  import {Observable} from "rxjs";
  import {LeasingTransactionResponse} from "../../dtos/leasing/LeasingTransactionResponse";
  import {CmtTransactionRequest} from "../../dtos/cmt/CmtTransactionRequest";
  import {CmtTransactionResponseDto} from "../../dtos/cmt/CmtTransactionResponseDto";
  import {PageResponse} from "../../../../global/types/PageResponse";
  import {TransactionResponseDto} from "../../dtos/transactions/TransactionResponseDto";
  import {ChequeTransactionRequest} from "../../dtos/cheques/ChequeTransactionRequest";
  import {ChequeTransactionResponseDto} from "../../dtos/cheques/ChequeTransactionResponseDto";
  import {LcnTransactionRequest} from "../../dtos/lcn/LcnTransactionRequest";
  import {LcnTransactionResponse} from "../../dtos/lcn/LcnTransactionResponse";
  import {
    MiseAdispositionTransactionRequestDto
  } from "../../dtos/mise_a_disposition/MiseAdispositionTransactionRequestDto";
  import {
    MiseAdispositionTransactionResponseDto
  } from "../../dtos/mise_a_disposition/MiseAdispositionTransactionResponseDto";
  import {VirementTransactionRequestDto} from "../../dtos/virements/VirementTransactionRequestDto";
  import {VirementTransactionResponseDto} from "../../dtos/virements/VirementTransactionResponseDto";

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
    echeanceLeasingToRapprochemnt(leasingTransactionRequest: LeasingTransactionRequest): Observable<LeasingTransactionResponse> {
      return this.http.post<LeasingTransactionResponse>(`${this.baseUrl}/process/echeance-leasing`,
        leasingTransactionRequest
      );
    }

    /**
     * Envoie une transaction d'échéance CMT au backend pour rapprochement
     * @param cmtTransactionRequest Les données de la transaction à rapprocher
     * @returns Observable<CmtTransactionResponseDto>
     */
    echeanceCmtToRapprochement(cmtTransactionRequest: CmtTransactionRequest): Observable<CmtTransactionResponseDto> {
      return this.http.post<CmtTransactionResponseDto>(`${this.baseUrl}/process/echeance-cmt`,
        cmtTransactionRequest
      );
    }

    /**
     * Envoie un cheque au backend pour le rapprochement
     * @param cheque  Les données du cheque  à rapprocher
     * @returns Observable<ChequeTransactionResponseDto >
     */
    chequeToRapprochement( cheque: ChequeTransactionRequest): Observable<ChequeTransactionResponseDto> {
      return this.http.post<ChequeTransactionResponseDto>(`${this.baseUrl}/process/cheque`,
        cheque
      );
    }

    /**
     * Envoie un LCN au backend pour le rapprochement
     * @param lcn  Les données du lcn à rapprocher
     * @returns Observable<LcnTransactionResponse>
     */
    lcnToRapprochement( lcn: LcnTransactionRequest): Observable<LcnTransactionResponse> {
      return this.http.post<LcnTransactionResponse>(`${this.baseUrl}/process/lcn`,
        lcn
      );
    }


    /**
     * Envoie le virement au backend pour le rapprochement
     * @param virement Les données du virement à rapprocher
     * @returns Observable<VirementTransactionResponseDto>
     */
    virementToRapprochement( virement: VirementTransactionRequestDto): Observable<VirementTransactionResponseDto> {
      return this.http.post<VirementTransactionResponseDto>(`${this.baseUrl}/process/virement`,
        virement
      );
    }

    /**
     * Envoie la mise à disposition  au backend pour le rapprochement
     * @param miseAdispo  Les données de la mise à disposition  à rapprocher
     * @returns Observable<MiseAdispositionTransactionResponseDto>
     */
    miseAdispoToRapprochement( miseAdispo: MiseAdispositionTransactionRequestDto): Observable<MiseAdispositionTransactionResponseDto> {
      return this.http.post<MiseAdispositionTransactionResponseDto>(`${this.baseUrl}/process/mise-a-disposition`,
        miseAdispo
      );
    }





  }
