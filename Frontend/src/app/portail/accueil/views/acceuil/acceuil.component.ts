import {Component, inject, OnDestroy, OnInit} from '@angular/core';
import {Button} from "primeng/button";
import {RecapitulatifCompte} from "../../dtos/RecapitulatifCompte";
import {TooltipModule} from "primeng/tooltip";
import {CommonModule, DatePipe} from '@angular/common';
import {BanqueService} from "../../../comptes-bancaire/services/BanqueService";
import {MenuItem, MessageService} from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { provideAnimations } from '@angular/platform-browser/animations';
import {ActivatedRoute, Router, RouterLink, RouterLinkActive} from "@angular/router";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {ProgressBarModule} from "primeng/progressbar";
import {FunPaymentService} from "../../../FunPaimentService/FunPaymentService";
import {RecapService} from "../../../etatsDesEncours/views/lesEncours/recap/services/recap.service";
import {
  HORIZON_OPTIONS,
  HorizonPrevision,
  horizonAuDemarrage,
  PeriodeRecap
} from "../../../etatsDesEncours/views/lesEncours/recap/dtos/PeriodeRecap";
import {catchError, finalize} from "rxjs/operators";
import {Subscription, throwError} from "rxjs";
import {FormControl, ReactiveFormsModule} from "@angular/forms";
import {DialogModule} from "primeng/dialog";
import {SelectButtonModule} from "primeng/selectbutton";
import {PeriodeChoix, PeriodeSelectorComponent} from "../periode-selector/periode-selector.component";
import {DropdownModule} from "primeng/dropdown";
import {SensOperation} from "../../../transactions/dtos/SensOperation";
import {EncoursOperationsDialogComponent} from "../encours-operations-dialog/encours-operations-dialog.component";

/**
 * Vrai tant que l'accueil n'a pas été affiché depuis le chargement de la page : seul ce premier affichage peut être un
 * rechargement (F5). Le type de navigation du navigateur reste « reload » pendant toute la session de la page, il ne
 * doit donc plus être consulté ensuite (retour arrière, navigation interne).
 */
let premierAffichageDepuisChargement = true;

function pageRechargee(): boolean {
  const entree = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming | undefined;
  return entree?.type === 'reload';
}

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
    ProgressBarModule,
    ReactiveFormsModule,
    DialogModule,
    SelectButtonModule,
    DropdownModule,
    PeriodeSelectorComponent,
    EncoursOperationsDialogComponent
  ],
  templateUrl: './acceuil.component.html',
  styleUrl: './acceuil.component.scss',
  providers: [MessageService, provideAnimations(), DatePipe]
})
export class AcceuilComponent implements OnInit, OnDestroy {

  recapitulatifComptes: RecapitulatifCompte[] = [];
  items: MenuItem[] | undefined;
  isLoading: boolean = false;

  globalRecapLoading: boolean = false;
  recapDetailleAllLoading: boolean = false;
  recapDetailleBanqueLoadingMap: Map<number, boolean> = new Map();

  // ── Horizon des cartes et des rapports détaillés : fin de mois (défaut) ou fin de trimestre ──
  // Porté par l'adresse (?horizon=FIN_DE_TRIMESTRE) pour survivre au retour arrière ; un rechargement (F5) revient à la fin de mois.
  readonly horizonOptions = HORIZON_OPTIONS;
  horizonControl = new FormControl<HorizonPrevision>('FIN_DE_MOIS', { nonNullable: true });

  // ── Popup des opérations en cours d'une banque (lignes « Encours entrées » / « Encours sorties » des cartes) ──
  showEncoursDialog = false;
  encoursCompte: RecapitulatifCompte | null = null;
  encoursSens: SensOperation = SensOperation.ENCAISSEMENT;

  // ── Choix de la période du Récap global (retour utilisateur du 31/08/2026) ──
  showRecapGlobalPeriodeModal: boolean = false;
  periodeControl = new FormControl<PeriodeRecap | null>('FIN_DE_MOIS');
  readonly periodeChoix: PeriodeChoix[] = [
    { value: 'A_CE_JOUR', label: 'À ce jour', icon: 'pi pi-calendar', hint: 'Déjà échu ou émis' },
    { value: 'FIN_DE_MOIS', label: 'Fin de mois', icon: 'pi pi-calendar-plus', hint: "Jusqu'au" },
    { value: 'FIN_DE_TRIMESTRE', label: 'Fin de trimestre', icon: 'pi pi-chart-line', hint: "Jusqu'au" },
  ];

  private banqueService      = inject(BanqueService);
  private messageService     = inject(MessageService);
  private recapService       = inject(RecapService);
  private funPaymentService  = inject(FunPaymentService);
  private datePipe           = inject(DatePipe);
  private route              = inject(ActivatedRoute);
  private router             = inject(Router);
  private subscriptions: Subscription[] = [];

  ngOnInit(): void {
    this.items = [{ icon: 'pi pi-home', route: '/portail/accueil', label:"Accueil" } ]
    const param = this.route.snapshot.queryParamMap.get('horizon');
    const rechargement = premierAffichageDepuisChargement && pageRechargee();
    premierAffichageDepuisChargement = false;
    this.horizonControl.setValue(horizonAuDemarrage(param, rechargement), { emitEvent: false });
    if (rechargement && param) {
      this.router.navigate([], { relativeTo: this.route, queryParams: { horizon: null }, replaceUrl: true });
    }
    this.subscriptions.push(
      this.horizonControl.valueChanges.subscribe(horizon => {
        this.router.navigate([], {
          relativeTo: this.route,
          queryParams: { horizon: horizon === 'FIN_DE_TRIMESTRE' ? horizon : null },
          replaceUrl: true,
        });
        this.fetchAccueilBankInfo();
      })
    );
    this.fetchAccueilBankInfo();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  /** Ouvre le popup des opérations en cours ; le clic ne doit pas suivre le lien de la carte. */
  ouvrirEncours(event: Event, compte: RecapitulatifCompte, sens: 'ENCAISSEMENT' | 'DECAISSEMENT'): void {
    event.stopPropagation();
    event.preventDefault();
    this.encoursCompte = compte;
    this.encoursSens = SensOperation[sens];
    this.showEncoursDialog = true;
  }

  get horizon(): HorizonPrevision {
    return this.horizonControl.value;
  }

  /** Titre de la tuile de prévision des cartes. */
  get titrePrevision(): string {
    return this.horizon === 'FIN_DE_TRIMESTRE' ? 'Fin de trimestre' : 'Fin de mois';
  }

  fetchAccueilBankInfo(): void {
    this.banqueService.getAccueilBankInfo(this.horizon).subscribe({
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


  /** Ouvre le choix de période avant de télécharger le Récap global (présélection : l'horizon des cartes). */
  openRecapGlobalPeriodeModal(): void {
    this.periodeControl.setValue(this.horizon);
    this.showRecapGlobalPeriodeModal = true;
  }

  confirmDownloadGlobalRecap(): void {
    if (!this.periodeControl.value) return;
    const periode = this.periodeControl.value;
    this.showRecapGlobalPeriodeModal = false;

    this.globalRecapLoading = true;

    this.recapService.downloadGlobalRecapForAllBanks(periode)
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
          const suffixePeriode = periode === 'A_CE_JOUR' ? 'A_ce_jour'
            : periode === 'FIN_DE_TRIMESTRE' ? 'Fin_de_trimestre' : 'Fin_de_mois';
          const filename = `Récapitulatif_Global_${suffixePeriode}_${date}.pdf`;

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

    this.recapService.downloadRecapGeneralDetailleAllBanquesPdf(this.horizon)
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

    this.recapService.downloadRecapGeneralDetaillePdf(banqueId, this.horizon)
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
