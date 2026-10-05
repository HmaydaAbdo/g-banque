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
import {InputSwitch, InputSwitchModule} from "primeng/inputswitch";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {RouterLinkActive} from "@angular/router";
import {CalendarModule} from "primeng/calendar";
import {InputNumberModule} from "primeng/inputnumber";

// Project-specific imports
import {VirementService} from "../../service/VirementService";
import {VirementResponseDto} from "../../dtos/VirementResponseDto";
import {VirementSearchDto} from "../../dtos/VirementSearchDto";
import {PageResponse} from "../../../global/types/PageResponse";
import {BanqueService} from "../../../comptes-bancaire/services/BanqueService";
import {BanqueResponse} from "../../../comptes-bancaire/dtos/BanqueResponse";
import {AuthService} from "../../../../auth/services/auth.service";
import {StatutMoyenPaiement} from "../../../global/enums/StatutMoyenPaiement";


@Component({
  selector: 'app-virement-decaissement-list',
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
    RouterLinkActive,
    CalendarModule,
    InputNumberModule
  ],
  templateUrl: './virement-decaissement-list.component.html',
  styleUrl: './virement-decaissement-list.component.scss',
  providers: [MessageService, ConfirmationService]
})
export class VirementDecaissementListComponent implements OnInit, OnDestroy {

  protected readonly StatutMoyenPaiement = StatutMoyenPaiement;

  virements: VirementResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  exporting: boolean = false;
  isAdmin: boolean = false;
  searchForm!: FormGroup;
  items: MenuItem[] | undefined;

  banqueOptions: SelectItem[] = [];
  statusOptions: SelectItem[] = [
    {label: 'Tous les virements', value: null},
    {label: 'En cours', value: StatutMoyenPaiement.ENCOURS},
    {label: 'Réglés', value: StatutMoyenPaiement.REGLE},
    {label: 'Annulés', value: StatutMoyenPaiement.ANNULE}
  ];

  /** Affiche/masque le panneau de recherche par période (fermé par défaut). */
  showDatePanel = false;

  @ViewChild('dt') dt: Table | undefined;

  private lastLazyLoadEvent: TableLazyLoadEvent = {
    first: 0,
    rows: 10,
    sortField: 'id',
    sortOrder: -1,
    filters: {}
  };

  private subscriptions: Subscription[] = [];

  constructor(
    private virementService: VirementService,
    private fb: FormBuilder,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private banqueService: BanqueService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.items = [
      {icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil'},
      {icon: 'pi pi-list', route: '/portail/Virements/decaissement', label: 'Les Virements (décaissement)'},
    ];
    this.isAdmin = this.authService.isSystemAdmin();
    this.initSearchForm();
    this.loadBanques();
    this.loadVirements(this.lastLazyLoadEvent);
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      beneficiaire: [''],
      nomBanqueEmettrice: [null],
      statut: [null],
      montant: [null],
      banqueBeneficiaire: [null],
      dateEmissionFrom: [null],
      dateEmissionTo: [null],
      dateOperationFrom: [null],
      dateOperationTo: [null],
      dateValeurFrom: [null],
      dateValeurTo: [null]
    });
    const sub = this.searchForm.valueChanges.subscribe(() => this.onSearch());
    this.subscriptions.push(sub);
  }

  toggleDatePanel(): void {
    this.showDatePanel = !this.showDatePanel;
  }

  get activeDateFilterCount(): number {
    const v = this.searchForm.value;
    let count = 0;
    if (v.dateEmissionFrom || v.dateEmissionTo) count++;
    if (v.dateOperationFrom || v.dateOperationTo) count++;
    if (v.dateValeurFrom || v.dateValeurTo) count++;
    return count;
  }

  loadVirements(event: TableLazyLoadEvent): void {
    this.loading = true;
    this.lastLazyLoadEvent = event;

    const currentPage = event.first! / event.rows!;
    const pageSize = event.rows!;

    let sort = 'dateEmission,desc';
    if (event.sortField && event.sortOrder) {
      sort = `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`;
    }

    const searchCriteria: VirementSearchDto = this.searchForm.getRawValue();

    this.virementService.getDecaissementVirements(searchCriteria, currentPage, pageSize, sort).subscribe({
      next: (page: PageResponse<VirementResponseDto>) => {
        this.virements = page.content;
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
      this.loadVirements(this.dt.createLazyLoadMetadata());
    }
  }

  loadBanques(): void {
    this.banqueService.getAllBanques({}, 0, 1000).subscribe({
      next: (response: PageResponse<BanqueResponse>) => {
        this.banqueOptions = response.content.map(b => ({label: b.nom, value: b.nom}));
        this.banqueOptions.unshift({label: 'Toutes les banques émettrices', value: null});
      },
      error: () => {}
    });
  }

  clearSearch(): void {
    this.searchForm.reset({
      beneficiaire: '', nomBanqueEmettrice: null, statut: null, montant: null, banqueBeneficiaire: null,
      dateEmissionFrom: null, dateEmissionTo: null,
      dateOperationFrom: null, dateOperationTo: null,
      dateValeurFrom: null, dateValeurTo: null
    });
    if (this.dt) {
      this.dt.clear();
      this.dt.first = 0;
      this.loadVirements(this.dt.createLazyLoadMetadata());
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

  confirmToggleStatus(virement: VirementResponseDto, sw: InputSwitch): void {
    // Le switch bascule visuellement au clic : on le recale sur le statut réel si l'opération n'aboutit pas.
    const resync = () => sw.writeValue(virement.statut === StatutMoyenPaiement.ENCOURS);
    const toRegle = virement.statut === StatutMoyenPaiement.ENCOURS;
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir ${toRegle ? 'régler' : 'remettre en cours'} le virement ID ${virement.id} ?`,
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      acceptButtonStyleClass: toRegle ? 'p-button-warning' : 'p-button-success',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => this.toggleVirementStatus(virement.id, resync),
      reject: resync
    });
  }

  toggleVirementStatus(id: number, onError: () => void): void {
    this.virementService.toggleVirementStatus(id).subscribe({
      next: (updated) => {
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: `Virement ID ${updated.id} — statut : ${this.getStatutLabel(updated.statut)}`
        });
        if (this.dt) this.loadVirements(this.dt.createLazyLoadMetadata());
      },
      error: (err) => {
        console.error('Erreur lors de la mise à jour du virement:', err);
        onError();
      }
    });
  }

  exportPdf(): void {
    this.exporting = true;
    const searchCriteria: VirementSearchDto = this.searchForm.getRawValue();
    this.virementService.exportDecaissementsPdf(searchCriteria).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `virements_decaissements_${new Date().toISOString().slice(0, 10)}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.exporting = false;
      },
      error: () => {
        this.exporting = false;
      }
    });
  }

  deleteVirement(virement: VirementResponseDto): void {
    this.confirmationService.confirm({
      message: `<div class="text-center">
        <i class="pi pi-exclamation-triangle" style="margin-right:5px;"></i>
        <p>Êtes-vous sûr de vouloir supprimer le virement <strong>${virement.id}</strong> ?</p>
        <p style="color:#ef4444;font-weight:bold;">Cette action est irréversible.</p>
      </div>`,
      header: 'Confirmation de Suppression',
      icon: 'none',
      acceptLabel: 'Confirmer',
      rejectLabel: 'Annuler',
      accept: () => {
        this.loading = true;
        this.virementService.deleteVirement(virement.id).subscribe({
          next: () => {
            this.messageService.add({severity: 'success', summary: 'Succès', detail: 'Virement supprimé avec succès.', life: 3000});
            this.loadVirements(this.lastLazyLoadEvent);
            this.loading = false;
          },
          error: (err) => {
            console.error('Erreur lors de la suppression du virement:', err);
            this.loading = false;
          }
        });
      }
    });
  }
}
