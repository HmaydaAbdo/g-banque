  import { inject, Injectable } from '@angular/core';
  import {LeasingTransactionRequest} from "../../dtos/leasing/LeasingTransactionRequest";
  import {HttpClient} from "@angular/common/http";
  import {environment} from "../../../../../environments/environment";
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
  import {FraisBancaireValidationRequest} from "../../dtos/frais-bancaire/frais-bancaire-validation-request.dto";
  import {FraisBancaireTransactionResponse} from "../../dtos/frais-bancaire/frais-bancaire-transaction-response.dto";
  import {FactoringTransactionRequest} from "../../dtos/factoring/FactoringTransactionRequest";
  import {FactoringTransactionResponse} from "../../dtos/factoring/FactoringTransactionResponse";
  import {PrelevementValidationRequestDto} from "../../dtos/prelevement/prelevement-validation-request.dto";
  import {PrelevementTransactionResponse} from "../../dtos/prelevement/PrelevementTransactionResponse";
  import {ImpotEtTaxeTransactionResponse} from "../../dtos/impotsEtTaxes/ImpotEtTaxeTransactionResponse";
  import {ImpotEtTaxeValidationRequest} from "../../dtos/impotsEtTaxes/ImpotEtTaxeValidationRequest";
  import {RestitutionTransactionResponse} from "../../dtos/restitution/RestitutionTransactionResponse";
  import {RestitutionValidationRequest} from "../../dtos/restitution/RestitutionValidationRequest";
  import {OperationErroneValidationRequest} from "../../dtos/operation-erronés/OperationErroneValidationRequest";
  import {OperationErroneTransactionResponse} from "../../dtos/operation-erronés/OperationErroneTransactionResponse";
  import {ProvisionValidationRequest} from "../../dtos/provision/ProvisionValidationRequest";
  import {ProvisionTransactionResponse} from "../../dtos/provision/ProvisionTransactionResponse";
  import {
    VirementInternationalTransactionRequestDto
  } from "../../dtos/virementsInternationaux/VirementInternationalTransactionRequestDto";
  import {
    VirementInternationalTransactionResponseDto
  } from "../../dtos/virementsInternationaux/VirementInternationalTransactionResponseDto";
  import {ImpayeeValidationRequest} from "../../dtos/impayee/Impayeevalidationrequest";
  import {ImpayeeTransactionResponse} from "../../dtos/impayee/Impayeetransactionresponse";
  import {BatchValidationRequestDto} from "../../dtos/lot/BatchValidationRequestDto";
  import {BatchValidationResponseDto} from "../../dtos/lot/BatchValidationResponseDto";

  @Injectable({
    providedIn: 'root'
  })
  export class ValidationService {


    private readonly baseUrl =  `${environment.apiUrl}rapprochement`

    private readonly http = inject(HttpClient);

    /**
     * Envoie une transaction d'échéance leasing au backend pour validation
     * @param leasingTransactionRequest Les données de la transaction à rapprocher
     * @returns Observable<LeasingTransactionResponse>
     */
    echeanceLeasingToRapprochemnt(leasingTransactionRequest: LeasingTransactionRequest): Observable<LeasingTransactionResponse> {
      return this.http.post<LeasingTransactionResponse>(`${this.baseUrl}/process/echeance-leasing`,
        leasingTransactionRequest
      );
    }

    /**
     * Envoie une transaction d'échéance CMT au backend pour validation
     * @param cmtTransactionRequest Les données de la transaction à rapprocher
     * @returns Observable<CmtTransactionResponseDto>
     */
    echeanceCmtToRapprochement(cmtTransactionRequest: CmtTransactionRequest): Observable<CmtTransactionResponseDto> {
      return this.http.post<CmtTransactionResponseDto>(`${this.baseUrl}/process/echeance-cmt`,
        cmtTransactionRequest
      );
    }



    /** Un seul endpoint depuis la fusion du 19/09/2026 — le sens est lu sur l'entité côté backend. */
    lcnToRapprochement( lcn: LcnTransactionRequest): Observable<LcnTransactionResponse> {
      return this.http.post<LcnTransactionResponse>(`${this.baseUrl}/process/lcn`,
        lcn
      );
    }

    /** Un seul endpoint depuis la fusion du 19/09/2026 — le sens est lu sur l'entité côté backend. */
    virementToRapprochement( virement: VirementTransactionRequestDto): Observable<VirementTransactionResponseDto> {
      return this.http.post<VirementTransactionResponseDto>(`${this.baseUrl}/process/virement`,
        virement
      );
    }

    /** Un seul endpoint depuis la fusion du 19/09/2026 — le sens est lu sur l'entité côté backend. */
    chequeToRapprochement( cheque: ChequeTransactionRequest): Observable<ChequeTransactionResponseDto> {
      return this.http.post<ChequeTransactionResponseDto>(`${this.baseUrl}/process/cheque`,
        cheque
      );
    }

    /** Un seul endpoint depuis la fusion du 19/09/2026 — le sens est lu sur l'entité côté backend. */
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

    prelevementToRapprochement(prelevement: PrelevementValidationRequestDto): Observable<PrelevementTransactionResponse> {
      return this.http.post<PrelevementTransactionResponse>(
        `${this.baseUrl}/process/prelevement`,
        prelevement
      );
    }





    fraisBancaireToRapprochement(fraisBancaire: FraisBancaireValidationRequest): Observable<FraisBancaireTransactionResponse> {
      return this.http.post<FraisBancaireTransactionResponse>(
        `${this.baseUrl}/process/frais-bancaire`,
        fraisBancaire
      );
    }


    impotEtTaxeToRapprochement(impotEtTaxe: ImpotEtTaxeValidationRequest): Observable<ImpotEtTaxeTransactionResponse> {
      return this.http.post<ImpotEtTaxeTransactionResponse>(
        `${this.baseUrl}/process/impot-taxe`, impotEtTaxe
      );
    }

    restitutionToRapprochement(restitution: RestitutionValidationRequest): Observable<RestitutionTransactionResponse> {
      return this.http.post<RestitutionTransactionResponse>(
        `${this.baseUrl}/process/restitution`,
        restitution
      );
    }

    /**
     * Traiter une opération erronée vers une transaction
     * POST /rapprochement/process/operation-errone
     */
    operationErroneToRapprochement(
      request: OperationErroneValidationRequest
    ): Observable<OperationErroneTransactionResponse> {
      return this.http.post<OperationErroneTransactionResponse>(
        `${this.baseUrl}/process/operation-errone`,
        request
      );
    }

    /**
     * Traiter une provision (encaissement ou décaissement selon son sens) vers une transaction
     * POST /rapprochement/process/provision
     */
    provisionToRapprochement(
      request: ProvisionValidationRequest
    ): Observable<ProvisionTransactionResponse> {
      return this.http.post<ProvisionTransactionResponse>(
        `${this.baseUrl}/process/provision`,
        request
      );
    }

    /**
     * Envoie une transaction de virement international au backend pour validation. Un seul
     * endpoint depuis la fusion du 19/09/2026 — le sens est lu sur l'entité côté backend.
     * @param virementInternational Les données de la transaction à rapprocher
     * @returns Observable<VirementInternationalTransactionResponseDto>
     */
    virementInternationalToRapprochement(
      virementInternational: VirementInternationalTransactionRequestDto
    ): Observable<VirementInternationalTransactionResponseDto> {
      return this.http.post<VirementInternationalTransactionResponseDto>(
        `${this.baseUrl}/process/virement-international`,
        virementInternational
      );
    }


    impayeeToRapprochement(impayee: ImpayeeValidationRequest): Observable<ImpayeeTransactionResponse> {
      return this.http.post<ImpayeeTransactionResponse>(
        `${this.baseUrl}/process/impayee`,
        impayee
      );
    }

    rollBackTransaction(id: number): Observable<void> {
      return this.http.delete<void>(`${this.baseUrl}/annuler/${id}`);
    }

    /**
     * Valide en une seule fois plusieurs opérations de même type (et même sens le cas échéant),
     * en appliquant les mêmes champs communs à toutes. Traitement tout-ou-rien côté backend.
     * POST /rapprochement/process/lot
     */
    validerEnLot(request: BatchValidationRequestDto): Observable<BatchValidationResponseDto> {
      return this.http.post<BatchValidationResponseDto>(
        `${this.baseUrl}/process/lot`,
        request
      );
    }

  }
