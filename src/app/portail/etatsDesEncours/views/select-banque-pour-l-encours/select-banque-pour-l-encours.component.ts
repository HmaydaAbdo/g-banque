import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { BanqueResponse } from '../../../comptes-bancaire/dtos/BanqueResponse';
import { catchError, of } from 'rxjs';
import {PageResponse} from "../../../global/types/PageResponse";

@Component({
  selector: 'app-select-banque-pour-l-encours',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DropdownModule,
    ButtonModule,
    NgIf
  ],
  templateUrl: './select-banque-pour-l-encours.component.html',
  styleUrl: './select-banque-pour-l-encours.component.scss'
})
export class SelectBanquePourLEncoursComponent implements OnInit {

  // Properties to hold the list of banks and the selected one
  public banks: BanqueResponse[] = [];
  public selectedBank: BanqueResponse | null = null;
  public loading: boolean = true;
  public errorMessage: string | null = null;

  constructor(
    private banqueService: BanqueService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadBanks();
  }

  /**
   * Fetches the list of banks from the service.
   * We'll use a simple pageable request to get all banks.
   */
  loadBanks(): void {
    this.loading = true;
    this.errorMessage = null;

    // Fetch banks without specific criteria. Adjust pagination if you have a large number of banks.
    this.banqueService.getAllBanques({}, 0, 100, 'nom,asc')
      .pipe(
        catchError(error => {
          console.error('Failed to load banks:', error);
          this.errorMessage = 'Une erreur est survenue lors du chargement des banques.';
          this.loading = false;
          // The fix: Ensure all required properties of PageResponse are present in the returned object.
          // I've changed 'number: 0' to 'page: 0' based on the error message.
          return of( );
        })
      )
      .subscribe((response: PageResponse<BanqueResponse>) => {
        this.banks = response.content;
        this.loading = false;
        // Pre-select the first bank if available
        if (this.banks.length > 0) {
          this.selectedBank = this.banks[0];
        }
      });
  }

  /**
   * Navigates to the "etats-des-encours" page with the selected bank's ID.
   * This method is triggered when the user clicks the button.
   */
  public navigateToEncours(): void {
    if (this.selectedBank && this.selectedBank.id) {
      this.router.navigate(['/portail/etats-des-encours', this.selectedBank.id]);
    } else {
      // Handle case where no bank is selected
      this.errorMessage = 'Veuillez sélectionner une banque pour continuer.';
    }
  }
}
