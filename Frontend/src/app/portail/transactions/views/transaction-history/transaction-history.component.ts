import { Component, OnInit, ViewChild, OnDestroy, inject } from '@angular/core';
import { CommonModule, DatePipe, CurrencyPipe, DecimalPipe } from '@angular/common';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { ToolbarModule } from 'primeng/toolbar';
import { InputTextModule } from 'primeng/inputtext';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { Subscription, debounceTime, distinctUntilChanged } from 'rxjs';
import { MenuItem, MessageService } from 'primeng/api';
import { DropdownModule } from 'primeng/dropdown';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { RouterLinkActive, RouterModule } from "@angular/router";
import { Ripple } from "primeng/ripple";
import { CalendarModule } from "primeng/calendar";

// DTOs & Services
import { TransactionResponseDto } from "../../dtos/TransactionResponseDto";
import { TransactionService } from "../../service/TransactionService";
import { BanqueService } from "../../../comptes-bancaire/services/BanqueService";
import { TransactionSource } from "../../dtos/TransactionSource";
import { TransactionSearchDto } from "../../dtos/TransactionSearchDto";
import {MultiSelectModule} from "primeng/multiselect";

@Component({
  selector: 'app-transaction-history',
  standalone: true,
  imports: [
    CommonModule, RouterModule, TableModule, ButtonModule, ToastModule,
    ToolbarModule, InputTextModule, ReactiveFormsModule,
    DropdownModule, TagModule, BreadcrumbModule, RouterLinkActive,
    TooltipModule, Ripple, MultiSelectModule, CalendarModule
  ],
  templateUrl: './transaction-history.component.html',
  styleUrl: './transaction-history.component.scss',
  providers: [DatePipe, CurrencyPipe, DecimalPipe, MessageService]
})
export class TransactionHistoryComponent implements OnInit, OnDestroy {

  private readonly transactionService = inject(TransactionService);
  private readonly banqueService = inject(BanqueService);
  private readonly fb = inject(FormBuilder);
  private readonly messageService = inject(MessageService);

  @ViewChild('dt') dt!: Table;

  transactions: TransactionResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  exporting: boolean = false;

  searchForm!: FormGroup;
  sourcesOptions: string[] = [];
  banquesOptions: { label: string, value: number | null }[] = [];
  statutOptions = [
    { label: 'Toutes les transactions', value: null },
    { label: 'Rapprochement en cours', value: 'EN_COURS' },
    { label: 'Clôture provisoire', value: 'CLOTURE_PROVISOIRE' },
    { label: 'Clôturé définitivement', value: 'CLOTURE_DEFINITIVE' }
  ];

  getStatutLabel(statut: string): string {
    switch (statut) {
      case 'EN_COURS':           return 'En cours';
      case 'CLOTURE_PROVISOIRE': return 'Clôture provisoire';
      case 'CLOTURE_DEFINITIVE': return 'Clôturé définitivement';
      default:                   return statut;
    }
  }

  getStatutSeverity(statut: string): 'success' | 'info' | 'warning' | 'danger' {
    switch (statut) {
      case 'EN_COURS':           return 'warning';
      case 'CLOTURE_PROVISOIRE': return 'info';
      case 'CLOTURE_DEFINITIVE': return 'success';
      default:                   return 'warning';
    }
  }

  items: MenuItem[] | undefined;
  private subscriptions: Subscription[] = [];

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: "Accueil" },
      { label: "Historique des transactions" },
    ];

    this.sourcesOptions = Object.values(TransactionSource);
    this.initSearchForm();
    this.loadBanques();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  /** Affiche/masque le panneau de recherche par période (fermé par défaut). */
  showDatePanel = false;

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      libelle: [''],
      banqueId: [null],
      transactionSources: [null],
      montant:[null],
      statutRapprochement: [null],
      dateEmissionDebut: [null],
      dateEmissionFin: [null],
      dateOperationDebut: [null],
      dateOperationFin: [null],
      dateValeurDebut: [null],
      dateValeurFin: [null]
    });

    const sub = this.searchForm.valueChanges
      .pipe(
        debounceTime(400),
        distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
      )
      .subscribe(() => this.onSearch());

    this.subscriptions.push(sub);
  }

  toggleDatePanel(): void {
    this.showDatePanel = !this.showDatePanel;
  }

  get activeDateFilterCount(): number {
    const v = this.searchForm.value;
    let count = 0;
    if (v.dateEmissionDebut || v.dateEmissionFin) count++;
    if (v.dateOperationDebut || v.dateOperationFin) count++;
    if (v.dateValeurDebut || v.dateValeurFin) count++;
    return count;
  }

  loadBanques(): void {
    this.banqueService.getAllBanques({}, 0, 1000, 'nom,asc').subscribe({
      next: (res) => {
        this.banquesOptions = [
          { label: 'Toutes les banques', value: null },
          ...res.content.map(b => ({ label: b.nom, value: b.id! }))
        ];
      }
    });
  }

  loadTransactions(event: TableLazyLoadEvent): void {
    this.loading = true;

    const searchDto: TransactionSearchDto = this.searchForm.value;
    const page = event.first! / event.rows!;
    const size = event.rows!;
    const sort = event.sortField ? `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}` : 'dateEmission,desc';

    this.transactionService.getTransactionHistory(searchDto, page, size, sort).subscribe({
      next: (res) => {
        this.transactions = res.content;
        this.totalRecords = res.page.totalElements;
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
      this.loadTransactions(this.dt.createLazyLoadMetadata());
    }
  }

  clearSearch(): void {
    this.searchForm.reset();
    this.onSearch();
  }

  exportPdf(): void {
    this.exporting = true;
    const searchDto: TransactionSearchDto = this.searchForm.value;

    this.transactionService.exportTransactionHistoryPdf(searchDto).subscribe({
      next: (blob: Blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `historique_transactions_${new Date().toISOString().slice(0, 10)}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
        this.exporting = false;
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: "Échec de l'export PDF." });
        this.exporting = false;
      }
    });
  }
}
