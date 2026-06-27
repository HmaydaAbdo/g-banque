import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject, tap } from 'rxjs';
import {RecapitulatifCompte} from "../../accueil/dtos/RecapitulatifCompte";
import {BanqueService} from "./BanqueService";

@Injectable({
  providedIn: 'root'
})
export class BankStateService {
  // BehaviorSubject qui stocke l'état actuel des infos bancaires
  private bankInfoSubject = new BehaviorSubject<RecapitulatifCompte | null>(null);

  // Signal émis après chaque validation réussie — permet au header de se rafraîchir
  private readonly _validationDone$ = new Subject<void>();
  readonly validationDone$ = this._validationDone$.asObservable();

  // ID du rapprochement actuellement sélectionné
  private currentRapprochementId: number | null = null;

  /** Notifie tous les abonnés qu'une transaction vient d'être validée. */
  notifyValidation(): void {
    this._validationDone$.next();
  }

  setRapprochementId(id: number | null): void {
    this.currentRapprochementId = id;
  }

  getRapprochementId(): number | null {
    return this.currentRapprochementId;
  }

  // Observable public pour que les composants puissent s'abonner
  public bankInfo$: Observable<RecapitulatifCompte | null> = this.bankInfoSubject.asObservable();

  private banqueService = inject(BanqueService);

  /**
   * Met à jour les informations de la banque dans le state
   * @param bankInfo Les nouvelles informations bancaires
   */
  updateBankInfo(bankInfo: RecapitulatifCompte): void {
    this.bankInfoSubject.next(bankInfo);
  }

  /**
   * Récupère les informations actuelles de la banque (valeur synchrone)
   * @returns Les informations bancaires actuelles ou null
   */
  getCurrentBankInfo(): RecapitulatifCompte | null {
    return this.bankInfoSubject.value;
  }

  /**
   * Réinitialise les informations de la banque (utile lors de la déconnexion)
   */
  clearBankInfo(): void {
    this.bankInfoSubject.next(null);
  }

  /**
   * Recharge les informations de la banque depuis le backend
   * et met à jour automatiquement tous les composants abonnés
   * @param banqueId L'ID de la banque à recharger
   */
  refreshBankInfo(banqueId: number): void {
    this.banqueService.getAccueilBankInfoById(banqueId).subscribe({
      next: (data: RecapitulatifCompte) => {
        this.updateBankInfo(data);
      },
      error: (err: any) => {
        console.error('Erreur lors du rechargement des informations bancaires', err);
      }
    });
  }

  /**
   * Charge les informations initiales de la banque et met à jour le state.
   * @param banqueId L'ID de la banque
   * @returns Observable des informations bancaires
   */
  loadBankInfo(banqueId: number): Observable<RecapitulatifCompte> {
    return this.banqueService.getAccueilBankInfoById(banqueId).pipe(
      tap((data: RecapitulatifCompte) => this.updateBankInfo(data))
    );
  }
}
