import {Component, inject, OnInit} from '@angular/core';
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
  providers: [MessageService, provideAnimations(), DatePipe]
})
export class AcceuilComponent implements OnInit {

  recapitulatifComptes: RecapitulatifCompte[] = [];
  items: MenuItem[] | undefined;
  isLoading: boolean = false;

  globalRecapLoading: boolean = false;
  recapDetailleAllLoading: boolean = false;
  recapDetailleBanqueLoadingMap: Map<number, boolean> = new Map();

  private banqueService      = inject(BanqueService);
  private messageService     = inject(MessageService);
  private recapService       = inject(RecapService);
  private funPaymentService  = inject(FunPaymentService);
  private datePipe           = inject(DatePipe);

  ngOnInit(): void {
    this.items = [{ icon: 'pi pi-home', route: '/portail/accueil', label:"Accueil" } ]
    this.fetchAccueilBankInfo();
  }

  fetchAccueilBankInfo(): void {
    this.banqueService.getAccueilBankInfo().subscribe({
      next: (data) => {
        this.recapitulatifComptes = data;
      },
      error: () => {}
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
      error: () => {}
    });
  }


  downloadGlobalRecap(): void {
    this.globalRecapLoading = true;

    this.recapService.downloadGlobalRecapForAllBanks()
      .pipe(
        // 1. Gérer l'erreur et afficher un message à l'utilisateur
        catchError(() => {
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
  downloadRecapGeneralDetailleAll(): void {
    this.recapDetailleAllLoading = true;

    this.recapService.downloadRecapGeneralDetailleAllBanquesPdf()
      .pipe(
        catchError(() => {
          return throwError(() => new Error('Download failed'));
        }),
        finalize(() => { this.recapDetailleAllLoading = false; })
      )
      .subscribe({
        next: (blob: Blob) => {
          const url = window.URL.createObjectURL(blob);
          const date = this.datePipe.transform(new Date(), 'dd/MM/yyyy');
          const a = document.createElement('a');
          a.href = url;
          a.download = `Récap_Général_Détaillé_${date}.pdf`;
          document.body.appendChild(a);
          a.click();
          window.URL.revokeObjectURL(url);
          document.body.removeChild(a);
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Le Récap Général Détaillé (toutes banques) est en cours de téléchargement.'
          });
        },
        error: () => {}
      });
  }

  downloadRecapGeneralDetaillePourBanque(banqueId: number): void {
    this.recapDetailleBanqueLoadingMap.set(banqueId, true);

    this.recapService.downloadRecapGeneralDetaillePdf(banqueId)
      .pipe(
        catchError(() => {
          return throwError(() => new Error('Download failed'));
        }),
        finalize(() => { this.recapDetailleBanqueLoadingMap.set(banqueId, false); })
      )
      .subscribe({
        next: (blob: Blob) => {
          const url = window.URL.createObjectURL(blob);
          const date = this.datePipe.transform(new Date(), 'dd/MM/yyyy');
          const a = document.createElement('a');
          a.href = url;
          a.download = `Récap_Général_Détaillé_Banque${banqueId}_${date}.pdf`;
          document.body.appendChild(a);
          a.click();
          window.URL.revokeObjectURL(url);
          document.body.removeChild(a);
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: 'Le Récap Général Détaillé (banque) est en cours de téléchargement.'
          });
        },
        error: () => {}
      });
  }
}
