  import { Injectable } from '@angular/core';
  import {LeasingTransactionRequest} from "../../dtos/leasing/LeasingTransactionRequest";
  import {HttpClient} from "@angular/common/http";
  import {environment} from "../../../../../../environments/environment";
  import {Observable} from "rxjs";
  import {LeasingTransactionResponse} from "../../dtos/leasing/LeasingTransactionResponse";
  import {CmtTransactionRequest} from "../../dtos/cmt/CmtTransactionRequest";
  import {CmtTransactionResponseDto} from "../../dtos/cmt/CmtTransactionResponseDto";
  import {ChequeTransactionRequest} from "../../dtos/cheques/ChequeTransactionRequest";
  import {ChequeTransactionResponseDto} from "../../dtos/cheques/ChequeTransactionResponseDto";
  import {LcnTransactionRequest} from "../../dtos/lcn/LcnTransactionRequest";
  import {LcnTransactionResponse} from "../../dtos/lcn/LcnTransactionResponse";
  import {MiseAdispositionTransactionRequestDto} from "../../dtos/mise_a_disposition/MiseAdispositionTransactionRequestDto";
  import {MiseAdispositionTransactionResponseDto} from "../../dtos/mise_a_disposition/MiseAdispositionTransactionResponseDto";
  import {VirementTransactionRequestDto} from "../../dtos/virements/VirementTransactionRequestDto";
  import {VirementTransactionResponseDto} from "../../dtos/virements/VirementTransactionResponseDto";
  import {PrefinancementTransactionRequest} from "../../dtos/prefinancement/PrefinancementTransactionRequest";
  import {PrefinancementTransactionResponse} from "../../dtos/prefinancement/PrefinancementTransactionResponse";
  import {DroitConstateTransactionRequest} from "../../dtos/droits_constate/DroitConstateTransactionRequest";
  import {DroitConstateTransactionResponse} from "../../dtos/droits_constate/DroitConstateTransactionResponse";
  import {DecompteTransactionRequest} from "../../dtos/decomptes/DecompteTransactionRequest";
  import {DecompteTransactionResponseDto} from "../../dtos/decomptes/DecompteTransactionResponseDto";
  import {CommissionValidationDto} from "../../dtos/commission/CommissionValidationDto";
  import {CommissionTransactionResponse} from "../../dtos/commission/CommissionTransactionResponse";
  import {AgiosValidationRequest} from "../../dtos/agios/agios-validation-request.dto";
  import {AgiosTransactionResponse} from "../../dtos/agios/AgiosTransactionResponse";
  import {InteretValidationRequest} from "../../dtos/interets/interet-validation-request.dto";
  import {InteretTransactionResponse} from "../../dtos/interets/interet-transaction-response.dto";
  import {FraisBancaireValidationRequest} from "../../dtos/frais-bancaire/frais-bancaire-validation-request.dto";
  import {FraisBancaireTransactionResponse} from "../../dtos/frais-bancaire/frais-bancaire-transaction-response.dto";
  import {FactoringTransactionRequest} from "../../dtos/factoring/FactoringTransactionRequest";
  import {FactoringTransactionResponse} from "../../dtos/factoring/FactoringTransactionResponse";

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



    prefinancementToRapprochement(prefinancement: PrefinancementTransactionRequest): Observable<PrefinancementTransactionResponse> {
      return this.http.post<PrefinancementTransactionResponse>(`${this.baseUrl}/process/prefinancement`,
        prefinancement
      );
    }

    droitConstateToRapprochement(dc: DroitConstateTransactionRequest): Observable<DroitConstateTransactionResponse> {
      return this.http.post<DroitConstateTransactionResponse>(`${this.baseUrl}/process/droit-constate`,
        dc
      );
    }

    factoringToRapprochement(factoring: FactoringTransactionRequest): Observable<FactoringTransactionResponse> {
      return this.http.post<FactoringTransactionResponse>(`${this.baseUrl}/process/factoring`,
        factoring
      );
    }

    decompteToRapprochement(decompte: DecompteTransactionRequest): Observable<DecompteTransactionResponseDto> {
      return this.http.post<DecompteTransactionResponseDto>(`${this.baseUrl}/process/decompte`,
        decompte
      );
    }

    commissionToRapprochement(commission: CommissionValidationDto): Observable<CommissionTransactionResponse> {
      return this.http.post<CommissionTransactionResponse>(
        `${this.baseUrl}/process/commission`,
        commission
      );
    }

    agiosToRapprochement(agios: AgiosValidationRequest): Observable<AgiosTransactionResponse> {
      return this.http.post<AgiosTransactionResponse>(
        `${this.baseUrl}/process/agios`,
        agios
      );
    }

    interetToRapprochement(interet: InteretValidationRequest): Observable<InteretTransactionResponse> {
      return this.http.post<InteretTransactionResponse>(
        `${this.baseUrl}/process/interet`,
        interet
      );
    }


    fraisBancaireToRapprochement(fraisBancaire: FraisBancaireValidationRequest): Observable<FraisBancaireTransactionResponse> {
      return this.http.post<FraisBancaireTransactionResponse>(
        `${this.baseUrl}/process/frais-bancaire`,
        fraisBancaire
      );
    }

  }
