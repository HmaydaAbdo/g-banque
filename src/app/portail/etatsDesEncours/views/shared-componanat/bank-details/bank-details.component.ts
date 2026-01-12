import {Component, Input, numberAttribute, OnInit, OnDestroy} from '@angular/core';
import {RecapitulatifCompte} from "../../../../accueil/dtos/RecapitulatifCompte";
import {DecimalPipe, NgIf} from "@angular/common";
import {Subscription} from "rxjs";
import {BankStateService} from "../../../../comptes-bancaire/services/bank-state.service";

@Component({
  selector: 'app-bank-details',
  standalone: true,
  imports: [
    NgIf,
    DecimalPipe
  ],
  templateUrl: './bank-details.component.html',
  styleUrl: './bank-details.component.scss'
})
export class BankDetailsComponent implements OnInit, OnDestroy {

  @Input({transform: numberAttribute}) banqueId!: number;

  bankInfo?: RecapitulatifCompte;
  loading = true;
  private bankInfoSubscription?: Subscription;

  constructor(
    private bankStateService: BankStateService
  ) {}

  ngOnInit() {
    if (!this.banqueId) {
      console.error('banqueId is required for BankDetailsComponent');
      this.loading = false;
      return;
    }

    // S'abonner aux changements des informations bancaires via le service
    this.bankInfoSubscription = this.bankStateService.bankInfo$.subscribe({
      next: (data) => {
        // Si des données sont reçues et correspondent à notre banqueId
        if (data && data.bankId === this.banqueId) {
          this.bankInfo = data;
          this.loading = false;
        }
      }
    });

    // Charger les informations initiales
    this.loadBankInfo();
  }

  /**
   * Charge les informations bancaires depuis le backend
   * et les enregistre dans le BankStateService
   */
  private loadBankInfo(): void {
    this.loading = true;
    this.bankStateService.loadBankInfo(this.banqueId).subscribe({
      next: (data) => {
        this.bankInfo = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur lors du chargement de la banque', err);
        this.loading = false;
      }
    });
  }

  /**
   * Nettoyage lors de la destruction du composant
   */
  ngOnDestroy(): void {
    if (this.bankInfoSubscription) {
      this.bankInfoSubscription.unsubscribe();
    }
  }
}
