import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterOutlet, NavigationEnd } from "@angular/router";
import { PanelMenuModule } from "primeng/panelmenu";
import { ConfirmationService, MenuItem, MessageService } from 'primeng/api';
import { CommonModule, DatePipe } from '@angular/common';
import { Ripple } from "primeng/ripple";
import { BreadcrumbModule } from "primeng/breadcrumb";
import { filter } from 'rxjs/operators';
import { Button } from "primeng/button";
import { RapprochementService } from "../../../../rapprochement/service/rapprochement-service.service";
import { ConfirmDialogModule } from "primeng/confirmdialog";
import { DialogModule } from "primeng/dialog";
import { ToastModule } from "primeng/toast";
import { TagModule } from "primeng/tag";
import { TooltipModule } from "primeng/tooltip";
import { RapprochementActifsDto } from "../../../../rapprochement/dtos/RapprochementActifsDto";
import { RapprochementContextHeaderComponent } from "../../shared-componanat/rapprochement-context-header/rapprochement-context-header.component";
import { BankStateService } from "../../../../comptes-bancaire/services/bank-state.service";

@Component({
  selector: 'app-les-encours',
  standalone: true,
  imports: [
    RouterOutlet,
    PanelMenuModule,
    CommonModule,
    Ripple,
    BreadcrumbModule,
    Button,
    ConfirmDialogModule,
    DialogModule,
    ToastModule,
    TagModule,
    TooltipModule,
    RapprochementContextHeaderComponent
  ],
  providers: [ConfirmationService, MessageService, DatePipe],
  templateUrl: './les-encours.component.html',
  styleUrls: ['./les-encours.component.scss']
})
export class LesEncoursComponent implements OnInit {

  items: MenuItem[] = [];
  breadcrumbItems: MenuItem[] = [];
  banqueId!: number;

  rapprochementId: number | null = null;
  actifs: RapprochementActifsDto | null = null;
  isProvisoire = false;

  displayClotureDialog: boolean = false;
  clotureLoading: boolean = false;

  /**
   * Nature de l'écran courant, affichée en pastille à côté du fil d'Ariane.
   * « Prévisionnel » = opérations attendues, pas encore passées en banque ;
   * « Réalisé » = transactions effectivement passées sur le compte.
   */
  natureEcran: 'previsionnel' | 'realise' = 'previsionnel';

  /** Seul segment adossé aux transactions déjà passées en banque — tout le reste (récapitulatif) est de l'encours. */
  private static readonly SEGMENTS_REALISES = new Set<string>(['rapprochement']);

  private breadcrumbMap: { [key: string]: { label: string, icon: string } } = {
    'rapprochement': { label: 'Rapprochement en cours', icon: 'pi pi-sync' },

    // Récapitulatifs
    'recapitulatif': { label: 'Récapitulatifs', icon: 'pi pi-chart-bar' },
  };

  private readonly route = inject(ActivatedRoute);
  private readonly rapprochementService = inject(RapprochementService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly router = inject(Router);
  private readonly bankStateService = inject(BankStateService);

  ngOnInit() {
    this.breadcrumbItems = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: "Accueil" },
      { icon: 'pi pi-list', route: '/portail/etats-des-encours', label: "Suivi des encours" },
      { icon: 'pi pi-chart-bar', route: '', label: "Récapitulatifs" }
    ];

    this.route.paramMap.subscribe(params => {
      this.banqueId = Number(params.get('banqueId'));
      const rid = params.get('rapprochementId');
      if (rid) {
        const parsed = +rid;
        if (!isNaN(parsed)) {
          this.rapprochementId = parsed;
          this.bankStateService.setRapprochementId(parsed);
        }
      }
      this.updateMenuItems();
      this.updateBreadcrumb();
      if (this.banqueId && this.rapprochementId) {
        this.loadActifs();
      }
    });

    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => this.updateBreadcrumb());
  }

  updateBreadcrumb() {
    const child = this.route.firstChild?.snapshot;
    if (child?.routeConfig?.path && this.banqueId) {
      const segment = child.routeConfig.path;
      this.natureEcran = LesEncoursComponent.SEGMENTS_REALISES.has(segment)
        ? 'realise'
        : 'previsionnel';
      const bc = this.breadcrumbMap[segment];
      if (bc) {
        this.breadcrumbItems[2] = {
          icon: bc.icon,
          route: `/portail/etats-des-encours/${this.banqueId}/${this.rapprochementId}/${segment}`,
          label: bc.label
        };
      }
    }
  }

  updateMenuItems() {
    this.items = [
      {
        label: 'Récapitulatifs',
        icon: 'pi pi-chart-bar',
        routerLink: ['/portail/etats-des-encours', this.banqueId, this.rapprochementId, 'recapitulatif']
      },
      {
        label: 'Rapprochement',
        icon: 'pi pi-sync',
        routerLink: ['/portail/etats-des-encours', this.banqueId, this.rapprochementId, 'rapprochement']
      }
    ];
  }


  toggleMenuItem(item: MenuItem): void {
    if (item.items) {
      if (item.expanded) {
        item.expanded = false;
      } else {
        this.items.forEach(i => { if (i.items) i.expanded = false; });
        item.expanded = true;
      }
    }
  }

  showClotureDialog(): void {
    const msg = this.isProvisoire
      ? 'Vous êtes sur le point de clôturer <strong>définitivement</strong> ce mois. Cette action est irréversible.'
      : 'Vous êtes sur le point de passer ce mois en <strong>clôture provisoire</strong>. Vous pourrez encore ajuster les opérations.';

    this.confirmationService.confirm({
      message: msg,
      header: this.isProvisoire ? 'Clôture définitive' : 'Clôture provisoire',
      icon: this.isProvisoire ? 'pi pi-lock' : 'pi pi-lock-open',
      acceptLabel: 'Confirmer',
      rejectLabel: 'Annuler',
      accept: () => {
        this.displayClotureDialog = true;
      }
    });
  }

  confirmCloture(): void {
    if (!this.banqueId) return;
    this.clotureLoading = true;

    if (this.isProvisoire && this.rapprochementId) {
      // Clôture définitive
      this.rapprochementService.cloturerDefinitivement(this.rapprochementId).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Clôture définitive effectuée.' });
          this.clotureLoading = false;
          this.displayClotureDialog = false;
          this.router.navigate(['/portail/etats-des-encours', this.banqueId, 'select']);
        },
        error: (err) => {
          this.clotureLoading = false;
        }
      });
    } else {
      // Clôture provisoire
      this.rapprochementService.cloturerRapprochement({ banqueId: this.banqueId }).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Clôture provisoire effectuée.' });
          window.location.reload();
        },
        error: (err) => {
          this.clotureLoading = false;
        }
      });
    }
  }

  private loadActifs(): void {
    this.rapprochementService.getActifs(this.banqueId).subscribe({
      next: (data) => {
        this.actifs = data;
        this.isProvisoire = !!data.provisoireId && this.rapprochementId === data.provisoireId;
      },
      error: () => { /* silent */ }
    });
  }

}
