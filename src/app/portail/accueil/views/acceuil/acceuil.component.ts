import {Component, OnInit} from '@angular/core';
import {Button} from "primeng/button";
import {RecapitulatifCompte} from "../../dtos/RecapitulatifCompte";
import {TooltipModule} from "primeng/tooltip";
import {CommonModule, DatePipe} from '@angular/common';
import {BanqueService} from "../../../comptes-bancaire/services/BanqueService";
import {MenuItem, MessageService} from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { provideAnimations } from '@angular/platform-browser/animations';
import {RouterLink, RouterLinkActive} from "@angular/router";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {ProgressBarModule} from "primeng/progressbar";
import {FunPaymentService} from "../../../FunPaimentService/FunPaymentService";
import {RecapService} from "../../../etatsDesEncours/views/lesEncours/recap/services/recap.service";
import {catchError, finalize} from "rxjs/operators";
import {throwError} from "rxjs";

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
  providers: [MessageService, provideAnimations(),DatePipe]
})
export class AcceuilComponent  implements OnInit{

  recapitulatifComptes: RecapitulatifCompte[] = [];
  items: MenuItem[] | undefined;
  isLoading: boolean = false;

  globalRecapLoading: boolean = false;

  constructor(
    private banqueService: BanqueService,
    private messageService: MessageService,
    private recapService:RecapService,
    private funPaymentService: FunPaymentService,
    private datePipe: DatePipe
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

    this.funPaymentService.synchronizePayments().pipe(
      finalize(()=>this.isLoading=false))
      .subscribe({
      next: (response) => {
        // Handle success
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: "La synchronisation des paiements depuis l’application FunPayment s’est déroulée avec succès. ",
          life: 5000
        });

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
      }
    });
  }


  downloadGlobalRecap(): void {
    this.globalRecapLoading = true;

    this.recapService.downloadGlobalRecapForAllBanks()
      .pipe(
        // 1. Gérer l'erreur et afficher un message à l'utilisateur
        catchError(error => {
          console.error('Erreur lors du téléchargement du récapitulatif global:', error);
          const errorMessage = error.status === 204 ?
            "Aucune donnée trouvée pour générer le rapport global." :
            error.error?.message || "Une erreur est survenue lors du téléchargement du rapport global.";

          this.messageService.add({
            severity: 'error',
            summary: 'Erreur Téléchargement',
            detail: errorMessage
          });
          // Propage l'erreur pour que 'finalize' soit appelé
          return throwError(() => new Error('Global download failed'));
        }),
        // 2. Désactiver l'indicateur de chargement à la fin de l'opération (succès ou échec)
        finalize(() => {
          this.globalRecapLoading = false;
        })
      )
      .subscribe({
        next: (blob: Blob) => {

          // --- LOGIQUE DE TÉLÉCHARGEMENT NATIVE DU NAVIGATEUR ---

          // 1. Créer une URL temporaire pour le Blob
          const url = window.URL.createObjectURL(blob);

          // 2. Déterminer le nom de fichier
          const date = this.datePipe.transform(new Date(), 'dd/MM/yyyy');
          const filename = `Récapitulatif_Global_${date}.pdf`;

          // 3. Créer un élément <a> invisible
          const a = document.createElement('a');
          a.href = url;
          a.download = filename; // Définit le nom de fichier

          // 4. Simuler un clic pour déclencher le téléchargement
          document.body.appendChild(a);
          a.click();

          // 5. Nettoyer les ressources temporaires
          window.URL.revokeObjectURL(url);
          document.body.removeChild(a);

          // -----------------------------------------------------

          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Le rapport consolidé de trésorerie est en cours de téléchargement.'
          });
        },
        error: () => {
          // L'erreur est gérée dans catchError, donc on ne fait rien ici.
        }
      });
  }
}
