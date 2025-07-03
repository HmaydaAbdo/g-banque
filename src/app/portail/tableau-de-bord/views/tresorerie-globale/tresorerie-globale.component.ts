import { Component, OnInit } from '@angular/core';
import { NgIf, CommonModule, CurrencyPipe } from '@angular/common';

import { catchError, of } from 'rxjs';
import {BanqueService} from "../../../comptes-bancaire/services/BanqueService";

@Component({
  selector: 'app-tresorerie-globale',
  standalone: true,
  imports: [
    CommonModule,
    CurrencyPipe
  ],
  templateUrl: './tresorerie-globale.component.html',
  styleUrl: './tresorerie-globale.component.scss'
})
export class TresorerieGlobaleComponent implements OnInit {

  currentGlobalTreasury: number | undefined;
  loading: boolean = true;
  errorMessage: string | undefined;

  constructor(private banqueService: BanqueService) { }

  ngOnInit() {
    this.loadGlobalTreasury();
  }

  /**
   * Loads the current global treasury by summing up all bank balances.
   */
  loadGlobalTreasury(): void {
    this.loading = true;
    this.errorMessage = undefined;
    this.currentGlobalTreasury = undefined; // Reset previous value

    // Using getAccueilBankInfo() as it provides bank names and balances.
    this.banqueService.getAccueilBankInfo().pipe(
      catchError(error => {
        console.error('Error fetching current bank info for global treasury:', error);
        this.errorMessage = 'Impossible de charger la trésorerie globale. Veuillez réessayer.';
        this.loading = false;
        return of([]); // Return an empty array on error
      })
    ).subscribe(recapData => {
      if (recapData && recapData.length > 0) {
        // Calculate the sum of all soldeActuel from the banks
        this.currentGlobalTreasury = recapData.reduce((sum, bank) => sum + bank.soldeActuelle, 0);
      } else {
        this.errorMessage = 'Aucune donnée de banque disponible pour calculer la trésorerie globale.';
      }
      this.loading = false;
    });

  }
}
