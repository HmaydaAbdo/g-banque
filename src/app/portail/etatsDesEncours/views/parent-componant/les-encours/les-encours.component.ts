import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterOutlet, NavigationEnd } from "@angular/router";
import { PanelMenuModule } from "primeng/panelmenu";
import { ConfirmationService, MenuItem, MessageService } from 'primeng/api';
import { CommonModule, DatePipe } from '@angular/common';
import { BankDetailsComponent } from "../../shared-componanat/bank-details/bank-details.component";
import { Ripple } from "primeng/ripple";
import { BreadcrumbModule } from "primeng/breadcrumb";
import { filter } from 'rxjs/operators';
import { Button } from "primeng/button";
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { RapprochementService } from "../../../../rapprochement/service/rapprochement-service.service";
import { CalendarModule } from "primeng/calendar";
import { ConfirmDialogModule } from "primeng/confirmdialog";
import { DialogModule } from "primeng/dialog";
import { ToastModule } from "primeng/toast";
import { TagModule } from "primeng/tag";
import { TooltipModule } from "primeng/tooltip";
import { RapprochementActifsDto } from "../../../../rapprochement/dtos/RapprochementActifsDto";

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
    CalendarModule,
    ConfirmDialogModule,
    DialogModule,
    FormsModule,
    ToastModule,
    ReactiveFormsModule,
    BankDetailsComponent,
    TagModule,
    TooltipModule
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
  clotureForm!: FormGroup;

  /** * Mapping complet basé sur vos routes enfants
   * Les icônes sont choisies pour être sémantiquement proches du contexte bancaire
   **/
  private breadcrumbMap: { [key: string]: { label: string, icon: string } } = {
    'rapprochement': { label: 'Rapprochement en cours', icon: 'pi pi-sync' },

    // Récapitulatifs
    'recap-decaissement': { label: 'Récapitulatif Décaissements', icon: 'pi pi-chart-bar' },
    'recap-encaissement': { label: 'Récapitulatif Encaissements', icon: 'pi pi-chart-line' },

    // Décaissements (Sorties)
    'echeances-leasing': { label: 'Décaissements / Échéances Leasing', icon: 'pi pi-building' },
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

  };

  constructor(
    private route: ActivatedRoute,
    private rapprochementService: RapprochementService,
    private messageService: MessageService,
    private fb: FormBuilder,
    private confirmationService: ConfirmationService,
    private router: Router
  ) {}

  ngOnInit() {
    this.breadcrumbItems = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: "Accueil" },
      { icon: 'pi pi-list', route: '/portail/etats-des-encours', label: "Suivi des encours" },
      { icon: 'pi pi-sync', route: '', label: "Rapprochement" }
    ];

    this.route.paramMap.subscribe(params => {
      this.banqueId = Number(params.get('banqueId'));
      this.updateMenuItems();
      this.updateBreadcrumb();
    });

    this.route.queryParamMap.subscribe(qp => {
      const rid = qp.get('rapprochementId');
      this.rapprochementId = rid ? +rid : null;
      if (this.banqueId) {
        this.loadActifs();
      }
    });

    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => this.updateBreadcrumb());

    this.initClotureForm();
  }

  updateBreadcrumb() {
    const child = this.route.firstChild?.snapshot;
    if (child?.routeConfig?.path && this.banqueId) {
      const segment = child.routeConfig.path;
      const bc = this.breadcrumbMap[segment];
      if (bc) {
        this.breadcrumbItems[2] = {
          icon: bc.icon,
          route: `/portail/etats-des-encours/${this.banqueId}/${segment}`,
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
        routerLink: ['/portail/etats-des-encours', this.banqueId, 'rapprochement']
      },
      {
        label: 'Récapitulatifs',
        icon: 'pi pi-briefcase',
        items: [
          { label: 'Décaissements', icon: 'pi pi-chart-bar', routerLink: ['/portail/etats-des-encours', this.banqueId, 'recap-decaissement'] },
          { label: 'Encaissements', icon: 'pi pi-chart-line', routerLink: ['/portail/etats-des-encours', this.banqueId, 'recap-encaissement'] }
        ]
      },
      {
        label: 'Encaissements',
        icon: 'pi pi-arrow-circle-down',
        items: [
          { label: "Virements", icon: "pi pi-download", routerLink: ["/portail/etats-des-encours", this.banqueId, "virements-encaissements"] },
          { label: "Virements Internationaux", icon: "pi pi-globe", routerLink: ["/portail/etats-des-encours", this.banqueId, "virements-internationaux-encaissements"] },
          { label: 'Chèques', icon: 'pi pi-money-bill', routerLink: ['/portail/etats-des-encours', this.banqueId, 'cheques-encaissements'] },
          { label: 'LCN', icon: 'pi pi-file-edit', routerLink: ['/portail/etats-des-encours', this.banqueId, 'lcn-encaissements'] },
          { label: 'Préfinancement', icon: 'pi pi-unlock', routerLink: ['/portail/etats-des-encours', this.banqueId, 'prefinancement-deblocage'] },
          { label: 'Droits Constatés', icon: 'pi pi-unlock', routerLink: ['/portail/etats-des-encours', this.banqueId, 'dc-deblocage'] },
          { label: 'Factoring', icon: 'pi pi-unlock', routerLink: ['/portail/etats-des-encours', this.banqueId, 'factoring-deblocage'] },
          { label: 'Décomptes', icon: 'pi pi-calculator', routerLink: ['/portail/etats-des-encours', this.banqueId, 'decomptes'] },
          { label: 'Restitutions', icon: 'pi pi-refresh', routerLink: ['/portail/etats-des-encours', this.banqueId, 'restitutions'] },
          { label: 'Impayées', icon: 'pi pi-ban', routerLink: ['/portail/etats-des-encours', this.banqueId, 'impayees-encaissement'] },

        ]
      },
      {
        label: 'Décaissements',
        icon: 'pi pi-arrow-circle-up',
        items: [
          { label: 'Leasing', icon: 'pi pi-building', routerLink: ['/portail/etats-des-encours', this.banqueId, 'echeances-leasing'] },
          { label: 'CMT', icon: 'pi pi-calendar-times', routerLink: ['/portail/etats-des-encours', this.banqueId, 'echeances-cmt'] },
          { label: 'Chèques', icon: 'pi pi-money-bill', routerLink: ['/portail/etats-des-encours', this.banqueId, 'cheques-decaissements'] },
          { label: 'LCN', icon: 'pi pi-file-edit', routerLink: ['/portail/etats-des-encours', this.banqueId, 'lcn-decaissements'] },
          { label: 'Virements', icon: 'pi pi-send', routerLink: ['/portail/etats-des-encours', this.banqueId, 'virements-decaissements'] },
          { label: 'Virements Internationaux', icon: 'pi pi-globe', routerLink: ['/portail/etats-des-encours', this.banqueId, 'virements-internationaux-decaissements'] },
          { label: 'Mise à disposition', icon: 'pi pi-wallet', routerLink: ['/portail/etats-des-encours', this.banqueId, 'mise-a-disposition-decaissements'] },
          { label: 'Préfinancement', icon: 'pi pi-replay', routerLink: ['/portail/etats-des-encours', this.banqueId, 'prefinancement-remboursement'] },
          { label: 'Droits Constatés', icon: 'pi pi-info-circle', routerLink: ['/portail/etats-des-encours', this.banqueId, 'dc-remboursement'] },
          { label: 'Factoring', icon: 'pi pi-percentage', routerLink: ['/portail/etats-des-encours', this.banqueId, 'factoring-remboursement'] },
          { label: 'Commissions', icon: 'pi pi-ticket', routerLink: ['/portail/etats-des-encours', this.banqueId, 'commissions'] },
          { label: 'Agios', icon: 'pi pi-percentage', routerLink: ['/portail/etats-des-encours', this.banqueId, 'agios'] },
          { label: 'Prélèvements', icon: 'pi pi-sign-out', routerLink: ['/portail/etats-des-encours', this.banqueId, 'prelevements'] },
          { label: 'Frais Bancaires', icon: 'pi pi-credit-card', routerLink: ['/portail/etats-des-encours', this.banqueId, 'fraisBancaire'] },
          { label: 'Impôts et Taxes', icon: 'pi pi-file', routerLink: ['/portail/etats-des-encours', this.banqueId, 'impots-taxes'] },
          { label: 'Opérations Erronées', icon: 'pi pi-exclamation-triangle', routerLink: ['/portail/etats-des-encours', this.banqueId, 'operations-errones'] },
          {label: 'Impayés', icon: 'pi pi-ban', routerLink: ['/portail/etats-des-encours', this.banqueId, 'impayees-decaissement']},

        ]
      }
    ];
  }

  // ... (Reste des méthodes toggleMenuItem, showClotureDialog, etc. sont identiques)

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
        this.clotureForm.reset();
        this.displayClotureDialog = true;
      }
    });
  }

  initClotureForm(): void {
    // Plus besoin de date — le backend identifie automatiquement le rapprochement EN_COURS
    this.clotureForm = this.fb.group({});
  }

  confirmCloture(): void {
    if (!this.banqueId) return;
    this.clotureLoading = true;

    if (this.isProvisoire && this.rapprochementId) {
      // Clôture définitive
      this.rapprochementService.cloturerDefinitivement(this.rapprochementId).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Clôture définitive effectuée.' });
          this.router.navigate(['/portail/etats-des-encours', this.banqueId, 'select']);
        },
        error: (err) => {
          this.messageService.add({ severity: 'error', summary: 'Erreur', detail: err.error?.message });
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
          this.messageService.add({ severity: 'error', summary: 'Erreur', detail: err.error?.message });
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

  formatPeriode(periode: string | null | undefined): string {
    if (!periode) return '';
    const [year, month] = periode.split('-');
    const months = ['Jan','Fév','Mar','Avr','Mai','Jun','Jul','Aoû','Sep','Oct','Nov','Déc'];
    return `${months[+month - 1]} ${year}`;
  }
}
