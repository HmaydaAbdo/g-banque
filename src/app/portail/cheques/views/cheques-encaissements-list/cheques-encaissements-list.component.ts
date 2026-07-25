import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { Button } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DatePipe, DecimalPipe, NgClass, NgIf } from '@angular/common';
import { DropdownModule } from 'primeng/dropdown';
import { InputSwitchModule } from 'primeng/inputswitch';
import { InputTextModule } from 'primeng/inputtext';
import { PaginatorModule } from 'primeng/paginator';
import { ConfirmationService, MenuItem, MessageService, PrimeTemplate, SelectItem } from 'primeng/api';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { Ripple } from 'primeng/ripple';
import { CalendarModule } from 'primeng/calendar';
import { FormsModule } from '@angular/forms';

import { ChequeResponseDto } from '../../dtos/ChequeResponseDto';
import { Subscription } from 'rxjs';
import { ChequeService } from '../../services/ChequeService';
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { AuthService } from '../../../../auth/services/auth.service';
import { ChequeSearchDto } from '../../dtos/ChequeSearchDto';
import { PageResponse } from '../../../global/types/PageResponse';
import { BanqueResponse } from '../../../comptes-bancaire/dtos/BanqueResponse';
import { StatutMoyenPaiement } from '../../../global/enums/StatutMoyenPaiement';

@Component({
  selector: 'app-cheques-encaissements-list',
  standalone: true,
  imports: [
    BreadcrumbModule,
    Button,
    ConfirmDialogModule,
    DatePipe,
    DecimalPipe,
    DropdownModule,
    InputSwitchModule,
    InputTextModule,
    NgIf,
    PaginatorModule,
    PrimeTemplate,
    ReactiveFormsModule,
    RouterLinkActive,
    RouterLink,
    TableModule,
    TagModule,
    ToastModule,
    NgClass,
    Ripple,
    CalendarModule,
    FormsModule
  ],
  templateUrl: './cheques-encaissements-list.component.html',
  styleUrl: './cheques-encaissements-list.component.scss'
})
export class ChequesEncaissementsListComponent implements OnInit, OnDestroy {

  protected readonly StatutMoyenPaiement = StatutMoyenPaiement;

  cheques: ChequeResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  exporting: boolean = false;
  isAdmin: boolean = false;
  searchForm!: FormGroup;
  items: MenuItem[] | undefined;

  banqueOptions: SelectItem[] = [];
  statusOptions: SelectItem[] = [
    { label: 'Tous les chèques', value: null },
    { label: 'En cours', value: StatutMoyenPaiement.ENCOURS },
    { label: 'Réglés', value: StatutMoyenPaiement.REGLE },
    { label: 'Annulés', value: StatutMoyenPaiement.ANNULE }
  ];

  /** Affiche/masque le panneau de recherche par période (fermé par défaut). */
  showDatePanel = false;

  @ViewChild('dt') dt: Table | undefined;

  private lastLazyLoadEvent: TableLazyLoadEvent = {
    first: 0,
    rows: 10,
    sortField: 'dateEmission',
    sortOrder: -1,
    filters: {}
  };

  private subscriptions: Subscription[] = [];

  constructor(
    private chequeService: ChequeService,
    private fb: FormBuilder,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private banqueService: BanqueService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/Cheques/encaissements', label: 'Les Chèques ( encaissements )' },
    ];
    this.isAdmin = this.authService.isSystemAdmin();
    this.initSearchForm();
    this.loadBanques();
    this.loadCheques(this.lastLazyLoadEvent);
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      dateEmissionFrom: [null],
      dateEmissionTo: [null],
      dateEcheanceFrom: [null],
      dateEcheanceTo: [null],
      dateOperationFrom: [null],
      dateOperationTo: [null],
      dateValeurFrom: [null],
      dateValeurTo: [null],
      montant: [null],
      numero: [''],
      emetteur: [''],
      nomBanque: [null],
      statut: [null]
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
    if (v.dateEcheanceFrom || v.dateEcheanceTo) count++;
    if (v.dateOperationFrom || v.dateOperationTo) count++;
    if (v.dateValeurFrom || v.dateValeurTo) count++;
    return count;
  }

  loadCheques(event: TableLazyLoadEvent): void {
    this.loading = true;
    this.lastLazyLoadEvent = event;

    const currentPage = event.first! / event.rows!;
    const pageSize = event.rows!;

    let sort = 'dateEmission,desc';
    if (event.sortField && event.sortOrder) {
      sort = `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`;
    }

    const searchCriteria: ChequeSearchDto = this.searchForm.getRawValue();

    this.chequeService.getEncaissementCheques(searchCriteria, currentPage, pageSize, sort).subscribe({
      next: (page: PageResponse<ChequeResponseDto>) => {
        this.cheques = page.content;
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
      this.loadCheques(this.dt.createLazyLoadMetadata());
    }
  }

  loadBanques(): void {
    this.banqueService.getAllBanques({}, 0, 1000).subscribe({
      next: (response: PageResponse<BanqueResponse>) => {
        this.banqueOptions = response.content.map(b => ({ label: b.nom, value: b.nom }));
        this.banqueOptions.unshift({ label: 'Toutes les banques', value: null });
      },
      error: () => {}
    });
  }

  clearSearch(): void {
    this.searchForm.reset({
      dateEmissionFrom: null, dateEmissionTo: null,
      dateEcheanceFrom: null, dateEcheanceTo: null,
      dateOperationFrom: null, dateOperationTo: null,
      dateValeurFrom: null, dateValeurTo: null,
      montant: null, numero: '', emetteur: '', nomBanque: null, statut: null
    });
    if (this.dt) {
      this.dt.clear();
      this.dt.first = 0;
      this.loadCheques(this.dt.createLazyLoadMetadata());
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

  confirmToggleStatus(cheque: ChequeResponseDto): void {
    const toRegle = cheque.statut === StatutMoyenPaiement.ENCOURS;
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir ${toRegle ? 'régler' : 'remettre en cours'} le chèque ID ${cheque.id} ?`,
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      acceptButtonStyleClass: toRegle ? 'p-button-warning' : 'p-button-success',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => this.toggleChequeStatus(cheque.id)
    });
  }

  toggleChequeStatus(id: number): void {
    this.chequeService.toggleChequeStatus(id).subscribe({
      next: (updated) => {
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: `Chèque ID ${updated.id} — statut : ${this.getStatutLabel(updated.statut)}`
        });
        if (this.dt) this.loadCheques(this.dt.createLazyLoadMetadata());
      },
      error: (err) => {
        console.error('Erreur lors de la mise à jour du chèque:', err);
      }
    });
  }

  exportPdf(): void {
    this.exporting = true;
    const searchCriteria: ChequeSearchDto = this.searchForm.getRawValue();
    this.chequeService.exportEncaissementsPdf(searchCriteria).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `cheques_encaissements_${new Date().toISOString().slice(0, 10)}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.exporting = false;
      },
      error: () => {
        this.exporting = false;
      }
    });
  }

  deleteCheque(cheque: ChequeResponseDto): void {
    this.confirmationService.confirm({
      message: `<div class="text-center">
        <i class="pi pi-exclamation-triangle" style="margin-right:5px;"></i>
        <p>Êtes-vous sûr de vouloir supprimer le chèque <strong>${cheque.id}</strong> ?</p>
        <p style="color:#ef4444;font-weight:bold;">Cette action est irréversible.</p>
      </div>`,
      header: 'Confirmation de Suppression',
      icon: 'none',
      acceptLabel: 'Confirmer',
      rejectLabel: 'Annuler',
      accept: () => {
        this.loading = true;
        this.chequeService.deleteCheque(cheque.id).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Chèque supprimé avec succès.', life: 3000 });
            this.loadCheques(this.lastLazyLoadEvent);
            this.loading = false;
          },
          error: (err) => {
            console.error('Erreur lors de la suppression du chèque:', err);
            this.loading = false;
          }
        });
      }
    });
  }
}
