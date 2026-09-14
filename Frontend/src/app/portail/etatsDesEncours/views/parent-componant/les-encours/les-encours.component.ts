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

  /** * Mapping complet basé sur vos routes enfants
   * Les icônes sont choisies pour être sémantiquement proches du contexte bancaire
   **/
  /**
   * Nature de l'écran courant, affichée en pastille à côté du fil d'Ariane.
   * « Prévisionnel » = opérations attendues, pas encore passées en banque ;
   * « Réalisé » = transactions effectivement passées sur le compte.
   */
  natureEcran: 'previsionnel' | 'realise' = 'previsionnel';

  /**
   * Seuls segments adossés aux transactions déjà passées en banque. Tout le reste
   * du workspace est de l'encours : la règle est énoncée ici en négatif plutôt que
   * répétée sur les 34 entrées de breadcrumbMap, pour qu'un nouvel écran d'encours
   * soit correctement étiqueté sans rien avoir à déclarer.
   */
  private static readonly SEGMENTS_REALISES = new Set<string>(['rapprochement']);

  private breadcrumbMap: { [key: string]: { label: string, icon: string } } = {
    'rapprochement': { label: 'Rapprochement en cours', icon: 'pi pi-sync' },

    // Récapitulatifs
    'recapitulatif': { label: 'Récapitulatifs', icon: 'pi pi-chart-bar' },

    // Décaissements (Sorties)
    'echeances-cmt': { label: 'Décaissements / Échéances CMT', icon: 'pi pi-calendar-times' },
    'cheques-decaissements': { label: 'Décaissements / Chèques', icon: 'pi pi-money-bill' },
    'lcn-decaissements': { label: 'Décaissements / LCN', icon: 'pi pi-file-edit' },
    'virements-decaissements': { label: 'Décaissements / Virements', icon: 'pi pi-send' },
    'virements-internationaux-decaissements': { label: 'Décaissements / Virements Internationaux', icon: 'pi pi-globe' },
    'mise-a-disposition-decaissements': { label: 'Décaissements / Mise à disposition', icon: 'pi pi-wallet' },
    'prefinancement-remboursement': { label: 'Décaissements / Préfinancement / Remboursement', icon: 'pi pi-replay' },
    'dc-remboursement': { label: 'Décaissements / Droits Constatés / Remboursement', icon: 'pi pi-info-circle' },
    'factoring-remboursement': { label: 'Décaissements / Factoring / Remboursement', icon: 'pi pi-percentage' },
    'commissions': { label: 'Décaissements / Commissions', icon: 'pi pi-ticket' },
    'agios': { label: 'Décaissements / Agios', icon: 'pi pi-percentage' },
    'prelevements': { label: 'Décaissements / Prélèvements', icon: 'pi pi-sign-out' },
    'fraisBancaire': { label: 'Décaissements / Frais Bancaires', icon: 'pi pi-credit-card' },
    'impots-taxes': { label: 'Décaissements / Impôts et Taxes', icon: 'pi pi-file' },
    'operations-errones': { label: 'Décaissements / Opérations Erronées', icon: 'pi pi-exclamation-triangle' },
    'provisions-decaissements': { label: 'Décaissements / Provisions', icon: 'pi pi-wallet' },
    'impayees-decaissement': { label: 'Décaissements / Impayés', icon: 'pi pi-exclamation-circle' },



    // Encaissements (Entrées)
    'lcn-encaissements': { label: 'Encaissements / LCN', icon: 'pi pi-file-edit' },
    'cheques-encaissements': { label: 'Encaissements / Chèques', icon: 'pi pi-money-bill' },
    'virements-encaissements': { label: 'Encaissements / Virements', icon: 'pi pi-download' },
    'virements-internationaux-encaissements': { label: 'Encaissements / Virements Internationaux', icon: 'pi pi-globe' },
    'mise-a-disposition-encaissements': { label: 'Encaissements / Mise à disposition', icon: 'pi pi-wallet' },
    'factoring-deblocage': { label: 'Encaissements / Factoring / Déblocage', icon: 'pi pi-unlock' },
    'prefinancement-deblocage': { label: 'Encaissements / Préfinancement / Déblocage', icon: 'pi pi-unlock' },
    'dc-deblocage': { label: 'Encaissements / Droits Constatés / Déblocage', icon: 'pi pi-unlock' },
    'decomptes': { label: 'Encaissements / Décomptes', icon: 'pi pi-calculator' },
    'restitutions': { label: 'Encaissements / Restitutions', icon: 'pi pi-refresh' },
    'impayees-encaissement': { label: 'Encaissements / Impayés', icon: 'pi pi-exclamation-circle' },
    'provisions-encaissements': { label: 'Encaissements / Provisions', icon: 'pi pi-wallet' },

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
      { icon: 'pi pi-sync', route: '', label: "Rapprochement" }
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
        label: 'Rapprochement',
        icon: 'pi pi-sync',
        routerLink: ['/portail/etats-des-encours', this.banqueId, this.rapprochementId, 'rapprochement']
      },
      {
        label: 'Récapitulatifs',
        icon: 'pi pi-briefcase',
        routerLink: ['/portail/etats-des-encours', this.banqueId, this.rapprochementId, 'recapitulatif']
      },
      {
        label: 'Encaissements',
        icon: 'pi pi-arrow-circle-down',
        items: [
          { label: "Virements", icon: "pi pi-download", routerLink: ["/portail/etats-des-encours", this.banqueId, this.rapprochementId, "virements-encaissements"] },
          { label: "Virements Internationaux", icon: "pi pi-globe", routerLink: ["/portail/etats-des-encours", this.banqueId, this.rapprochementId, "virements-internationaux-encaissements"] },
          { label: 'Chèques', icon: 'pi pi-money-bill', routerLink: ['/portail/etats-des-encours', this.banqueId, this.rapprochementId, 'cheques-encaissements'] },
          { label: 'LCN', icon: 'pi pi-file-edit', routerLink: ['/portail/etats-des-encours', this.banqueId, this.rapprochementId, 'lcn-encaissements'] },
          { label: 'Préfinancement', icon: 'pi pi-unlock', routerLink: ['/portail/etats-des-encours', this.banqueId, this.rapprochementId, 'prefinancement-deblocage'] },
          { label: 'Droits Constatés', icon: 'pi pi-unlock', routerLink: ['/portail/etats-des-encours', this.banqueId, this.rapprochementId, 'dc-deblocage'] },
          { label: 'Factoring', icon: 'pi pi-unlock', routerLink: ['/portail/etats-des-encours', this.banqueId, this.rapprochementId, 'factoring-deblocage'] },
          { label: 'Décomptes', icon: 'pi pi-calculator', routerLink: ['/portail/etats-des-encours', this.banqueId, this.rapprochementId, 'decomptes'] },
          { label: 'Restitutions', icon: 'pi pi-refresh', routerLink: ['/portail/etats-des-encours', this.banqueId, this.rapprochementId, 'restitutions'] },
          { label: 'Impayées', icon: 'pi pi-ban', routerLink: ['/portail/etats-des-encours', this.banqueId, this.rapprochementId, 'impayees-encaissement'] },
          { label: 'Provisions', icon: 'pi pi-wallet', routerLink: ['/portail/etats-des-encours', this.banqueId, this.rapprochementId, 'provisions-encaissements'] },

        ]
      },
      {
        label: 'Décaissements',
        icon: 'pi pi-arrow-circle-up',
        items: [
          { label: 'CMT', icon: 'pi pi-calendar-times', routerLink: ['/portail/etats-des-encours', this.banqueId, this.rapprochementId, 'echeances-cmt'] },
          { label: 'Chèques', icon: 'pi pi-money-bill', routerLink: ['/portail/etats-des-encours', this.banqueId, this.rapprochementId, 'cheques-decaissements'] },
          { label: 'LCN', icon: 'pi pi-file-edit', routerLink: ['/portail/etats-des-encours', this.banqueId, this.rapprochementId, 'lcn-decaissements'] },
          { label: 'Virements', icon: 'pi pi-send', routerLink: ['/portail/etats-des-encours', this.banqueId, this.rapprochementId, 'virements-decaissements'] },
          { label: 'Virements Internationaux', icon: 'pi pi-globe', routerLink: ['/portail/etats-des-encours', this.banqueId, this.rapprochementId, 'virements-internationaux-decaissements'] },
          { label: 'Mise à disposition', icon: 'pi pi-wallet', routerLink: ['/portail/etats-des-encours', this.banqueId, this.rapprochementId, 'mise-a-disposition-decaissements'] },
          { label: 'Préfinancement', icon: 'pi pi-replay', routerLink: ['/portail/etats-des-encours', this.banqueId, this.rapprochementId, 'prefinancement-remboursement'] },
          { label: 'Droits Constatés', icon: 'pi pi-info-circle', routerLink: ['/portail/etats-des-encours', this.banqueId, this.rapprochementId, 'dc-remboursement'] },
          { label: 'Factoring', icon: 'pi pi-percentage', routerLink: ['/portail/etats-des-encours', this.banqueId, this.rapprochementId, 'factoring-remboursement'] },
          { label: 'Commissions', icon: 'pi pi-ticket', routerLink: ['/portail/etats-des-encours', this.banqueId, this.rapprochementId, 'commissions'] },
          { label: 'Agios', icon: 'pi pi-percentage', routerLink: ['/portail/etats-des-encours', this.banqueId, this.rapprochementId, 'agios'] },
          { label: 'Prélèvements', icon: 'pi pi-sign-out', routerLink: ['/portail/etats-des-encours', this.banqueId, this.rapprochementId, 'prelevements'] },
          { label: 'Frais Bancaires', icon: 'pi pi-credit-card', routerLink: ['/portail/etats-des-encours', this.banqueId, this.rapprochementId, 'fraisBancaire'] },
          { label: 'Impôts et Taxes', icon: 'pi pi-file', routerLink: ['/portail/etats-des-encours', this.banqueId, this.rapprochementId, 'impots-taxes'] },
          { label: 'Opérations Erronées', icon: 'pi pi-exclamation-triangle', routerLink: ['/portail/etats-des-encours', this.banqueId, this.rapprochementId, 'operations-errones'] },
          {label: 'Impayés', icon: 'pi pi-ban', routerLink: ['/portail/etats-des-encours', this.banqueId, this.rapprochementId, 'impayees-decaissement']},
          { label: 'Provisions', icon: 'pi pi-wallet', routerLink: ['/portail/etats-des-encours', this.banqueId, this.rapprochementId, 'provisions-decaissements'] },

        ]
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
