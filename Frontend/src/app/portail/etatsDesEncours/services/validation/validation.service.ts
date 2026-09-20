import { inject, Injectable } from '@angular/core';
import { HttpClient } from "@angular/common/http";
import { environment } from "../../../../../environments/environment";
import { Observable } from "rxjs";
import { BatchValidationRequestDto } from "../../dtos/lot/BatchValidationRequestDto";
import { BatchValidationResponseDto } from "../../dtos/lot/BatchValidationResponseDto";

/**
 * Point d'entrée unique pour la validation (rapprochement), toutes sources confondues — voir
 * conventions/architecture.md, Règle 1 (révisée le 19/09/2026) : valider une opération n'est
 * qu'un cas particulier de valider un lot de taille 1, il n'existe donc plus de méthode ni
 * d'endpoint dédié par source.
 */
@Injectable({
  providedIn: 'root'
})
export class ValidationService {

  private readonly baseUrl = `${environment.apiUrl}rapprochement`;

  private readonly http = inject(HttpClient);

  /**
   * Valide une ou plusieurs opérations de même source (et même sens le cas échéant), en
   * appliquant les mêmes champs communs à toutes. Traitement tout-ou-rien côté backend.
   * POST /rapprochement/process
   */
  valider(request: BatchValidationRequestDto): Observable<BatchValidationResponseDto> {
    return this.http.post<BatchValidationResponseDto>(
      `${this.baseUrl}/process`,
      request
    );
  }

  rollBackTransaction(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/annuler/${id}`);
  }

}
