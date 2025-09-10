import {Component, OnInit} from '@angular/core';
import {Button} from "primeng/button";
import {RecapitulatifCompte} from "../../dtos/RecapitulatifCompte";
import {TooltipModule} from "primeng/tooltip";
import { CommonModule } from '@angular/common';
import {BanqueService} from "../../../comptes-bancaire/services/BanqueService";
import {MenuItem, MessageService} from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { provideAnimations } from '@angular/platform-browser/animations';
import {RouterLink, RouterLinkActive} from "@angular/router";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {ProgressBarModule} from "primeng/progressbar";
import {FunPaymentService} from "../../../FunPaimentService/FunPaymentService";

@Component({
  selector: 'app-accueil',
  standalone: true,
  imports: [
    Button,
    TooltipModule,
    CommonModule,
    ToastModule,
    RouterLink,
    BreadcrumbModule,
    RouterLinkActive,
    ProgressBarModule
  ],
  templateUrl: './acceuil.component.html',
  styleUrl: './acceuil.component.scss',
  providers: [MessageService, provideAnimations()]
})
export class AcceuilComponent  implements OnInit{

  recapitulatifComptes: RecapitulatifCompte[] = [];
  items: MenuItem[] | undefined;
  isLoading: boolean = false;

  constructor(
    private banqueService: BanqueService,
    private messageService: MessageService,
    private funPaymentService: FunPaymentService
  ) {}

  ngOnInit(): void {
    this.items = [{ icon: 'pi pi-home', route: '/portail/accueil', label:"Accueil" } ]
    this.fetchAccueilBankInfo();
  }

  fetchAccueilBankInfo(): void {
    this.banqueService.getAccueilBankInfo().subscribe({
      next: (data) => {
        this.recapitulatifComptes = data;
      },
      error: (err) => {
        console.error('Error fetching bank accueil info:', err);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec du chargement des informations des banques: ' + (err.error?.message || err.message),
          life: 5000
        });
      }
    });
  }

  /**
   * Triggers the payment synchronization process.
   * This method sets a loading state, calls the service, and handles the API response.
   */
  synchronizePayments(): void {
    this.isLoading = true; // Set loading state to true

    this.funPaymentService.synchronizePayments().subscribe({
      next: (response) => {
        // Handle success
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: response, // The success message from the backend
          life: 5000
        });
        // You might want to refresh the bank data after a successful sync
        this.fetchAccueilBankInfo();
      },
      error: (err) => {
        // Handle error
        console.error('Error during payment synchronization:', err);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec de la synchronisation des paiements: ' + (err.error || err.message),
          life: 5000
        });
      },
      complete: () => {
        this.isLoading = false; // Set loading state to false when the call is complete (success or error)
      }
    });
  }
}
