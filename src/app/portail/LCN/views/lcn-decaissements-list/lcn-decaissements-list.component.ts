import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Subscription} from 'rxjs';
import {CommonModule} from '@angular/common';
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule} from '@angular/forms';

// PrimeNG modules
import {ButtonModule} from 'primeng/button';
import {CardModule} from 'primeng/card';
import {InputTextModule} from 'primeng/inputtext';
import {Table, TableLazyLoadEvent, TableModule} from 'primeng/table';
import {TagModule} from 'primeng/tag';
import {TooltipModule} from 'primeng/tooltip';
import {RippleModule} from 'primeng/ripple';
import {ToastModule} from 'primeng/toast';
import {ToolbarModule} from 'primeng/toolbar';
import {ConfirmationService, MenuItem, MessageService, SelectItem} from 'primeng/api';
import {ConfirmDialogModule} from 'primeng/confirmdialog';
import {DropdownModule} from 'primeng/dropdown';
import {InputSwitchModule} from "primeng/inputswitch";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {RouterLinkActive} from "@angular/router";

// Project-specific imports
import {LcnResponseDTO} from "../../dtos/LcnResponseDTO";
import {LcnSearchDTO} from "../../dtos/LcnSearchDTO";
import {PageResponse} from "../../../global/types/PageResponse";
import {BanqueService} from "../../../comptes-bancaire/services/BanqueService";
import {BanqueResponse} from "../../../comptes-bancaire/dtos/BanqueResponse";
import {AuthService} from "../../../../auth/services/auth.service";
import {LcnService} from "../../service/LcnService";
import {StatutMoyenPaiement} from "../../../global/enums/StatutMoyenPaiement";


@Component({
  selector: 'app-lcn-decaissements-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    CardModule,
    ButtonModule,
    ToastModule,
    ToolbarModule,
    InputTextModule,
    TableModule,
    TagModule,
    TooltipModule,
    RippleModule,
    ConfirmDialogModule,
    DropdownModule,
    InputSwitchModule,
    FormsModule,
    BreadcrumbModule,
    RouterLinkActive
  ],
  templateUrl: './lcn-decaissements-list.component.html',
  styleUrl: './lcn-decaissements-list.component.scss',
  providers: [MessageService, ConfirmationService]
})
export class LcnDecaissementsListComponent implements OnInit, OnDestroy {

  protected readonly StatutMoyenPaiement = StatutMoyenPaiement;

  lcns: LcnResponseDTO[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  exporting: boolean = false;
  isAdmin: boolean = false;
  searchForm!: FormGroup;
  items: MenuItem[] | undefined;

  banqueOptions: SelectItem[] = [];
  statusOptions: SelectItem[] = [
    {label: 'Tous les LCN', value: null},
    {label: 'En cours', value: StatutMoyenPaiement.ENCOURS},
    {label: 'Réglés', value: StatutMoyenPaiement.REGLE},
    {label: 'Annulés', value: StatutMoyenPaiement.ANNULE}
  ];

  @ViewChild('dt') dt: Table | undefined;

  private lastLazyLoadEvent: TableLazyLoadEvent = {
    first: 0,
    rows: 10,
    sortField: 'dateEcheance',
    sortOrder: -1,
    filters: {}
  };

  private subscriptions: Subscription[] = [];

  constructor(
    private lcnService: LcnService,
    private fb: FormBuilder,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private banqueService: BanqueService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.items = [
      {icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil'},
      {icon: 'pi pi-list', route: '/portail/LCN/decaissements', label: 'LCN (décaissements)'},
    ];
    this.isAdmin = this.authService.isSystemAdmin();
    this.initSearchForm();
    this.loadBanques();
    this.loadLcns(this.lastLazyLoadEvent);
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      numero: [''],
      beneficiaire: [''],
      nomBanque: [null],
      statut: [null]
    });
    const sub = this.searchForm.valueChanges.subscribe(() => this.onSearch());
    this.subscriptions.push(sub);
  }

  loadLcns(event: TableLazyLoadEvent): void {
    this.loading = true;
    this.lastLazyLoadEvent = event;

    const currentPage = event.first! / event.rows!;
    const pageSize = event.rows!;

    let sort = 'dateEcheance,desc';
    if (event.sortField && event.sortOrder) {
      sort = `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`;
    }

    const searchCriteria: LcnSearchDTO = {
      numero: this.searchForm.get('numero')?.value,
      beneficiaire: this.searchForm.get('beneficiaire')?.value,
      nomBanque: this.searchForm.get('nomBanque')?.value,
      statut: this.searchForm.get('statut')?.value
    };

    this.lcnService.getDecaissementLcns(searchCriteria, currentPage, pageSize, sort).subscribe({
      next: (page: PageResponse<LcnResponseDTO>) => {
        this.lcns = page.content;
        this.totalRecords = page.page.totalElements;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  onSearch(): void {
    if (this.dt) {
      this.dt.first = 0;
      this.loadLcns(this.dt.createLazyLoadMetadata());
    }
  }

  loadBanques(): void {
    this.banqueService.getAllBanques({}, 0, 1000).subscribe({
      next: (response: PageResponse<BanqueResponse>) => {
        this.banqueOptions = response.content.map(b => ({label: b.nom, value: b.nom}));
        this.banqueOptions.unshift({label: 'Toutes les banques', value: null});
      },
      error: () => {}
    });
  }

  clearSearch(): void {
    this.searchForm.reset({numero: '', beneficiaire: '', nomBanque: null, statut: null});
    if (this.dt) {
      this.dt.clear();
      this.dt.first = 0;
      this.loadLcns(this.dt.createLazyLoadMetadata());
    }
  }

  getStatutSeverity(statut: StatutMoyenPaiement): 'success' | 'warning' | 'danger' | 'info' {
    switch (statut) {
      case StatutMoyenPaiement.ENCOURS: return 'success';
      case StatutMoyenPaiement.REGLE:   return 'warning';
      case StatutMoyenPaiement.ANNULE:  return 'danger';
      default: return 'info';
    }
  }

  getStatutLabel(statut: StatutMoyenPaiement): string {
    switch (statut) {
      case StatutMoyenPaiement.ENCOURS: return 'En cours';
      case StatutMoyenPaiement.REGLE:   return 'Réglé';
      case StatutMoyenPaiement.ANNULE:  return 'Annulé';
      default: return statut;
    }
  }

  confirmToggleStatus(lcn: LcnResponseDTO): void {
    const toRegle = lcn.statut === StatutMoyenPaiement.ENCOURS;
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir ${toRegle ? 'régler' : 'remettre en cours'} le LCN ID ${lcn.id} ?`,
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      acceptButtonStyleClass: toRegle ? 'p-button-warning' : 'p-button-success',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => this.toggleLcnStatus(lcn.id)
    });
  }

  toggleLcnStatus(id: number): void {
    this.lcnService.toggleLcnStatus(id).subscribe({
      next: (updated) => {
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: `LCN ID ${updated.id} — statut : ${this.getStatutLabel(updated.statut)}`
        });
        if (this.dt) this.loadLcns(this.dt.createLazyLoadMetadata());
      },
      error: (err) => {
        console.error('Erreur lors de la mise à jour du LCN:', err);
      }
    });
  }

  exportPdf(): void {
    this.exporting = true;
    const searchCriteria: LcnSearchDTO = this.searchForm.getRawValue();
    this.lcnService.exportDecaissementsPdf(searchCriteria).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `lcn_decaissements_${new Date().toISOString().slice(0, 10)}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.exporting = false;
      },
      error: () => {
        this.exporting = false;
      }
    });
  }

  deleteLcn(lcn: LcnResponseDTO): void {
    this.confirmationService.confirm({
      message: `<div class="text-center">
        <i class="pi pi-exclamation-triangle" style="margin-right:5px;"></i>
        <p>Êtes-vous sûr de vouloir supprimer le LCN <strong>${lcn.id}</strong> ?</p>
        <p style="color:#ef4444;font-weight:bold;">Cette action est irréversible.</p>
      </div>`,
      header: 'Confirmation de Suppression',
      icon: 'none',
      acceptLabel: 'Confirmer',
      rejectLabel: 'Annuler',
      accept: () => {
        this.loading = true;
        this.lcnService.deleteLcn(lcn.id).subscribe({
          next: () => {
            this.messageService.add({severity: 'success', summary: 'Succès', detail: 'LCN supprimé avec succès.', life: 3000});
            this.loadLcns(this.lastLazyLoadEvent);
            this.loading = false;
          },
          error: (err) => {
            console.error('Erreur lors de la suppression du LCN:', err);
            this.loading = false;
          }
        });
      }
    });
  }
}
