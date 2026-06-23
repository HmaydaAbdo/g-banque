import { Component, inject, OnInit, ViewChild } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import {ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { BanqueResponse } from '../../../dtos/BanqueResponse';
import { BanqueService } from '../../../services/BanqueService';
import { LigneFonctionnelleService } from '../../../../lignes-fonctionnelles/services/LigneFonctionnelleService';
import { LigneFonctionnelleResponse } from '../../../../lignes-fonctionnelles/dtos/LigneFonctionnelleResponse';
import { LigneFonctionnelleSearchCriteria } from '../../../../lignes-fonctionnelles/dtos/LigneFonctionnelleSearchCriteria';
import { NatureLigneFonctionnelleService } from '../../../../lignes-fonctionnelles/services/nature-ligne-fonctionnelle.service';

import {ConfirmationService, MenuItem, MessageService} from 'primeng/api';

// PrimeNG Modules
import { TagModule } from 'primeng/tag';
import { CardModule } from 'primeng/card';
import { PanelModule } from 'primeng/panel';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { DividerModule } from 'primeng/divider';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { TooltipModule } from 'primeng/tooltip';
import { ToastModule } from 'primeng/toast';
import { InputSwitchModule } from 'primeng/inputswitch';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { PageResponse } from "../../../../global/types/PageResponse";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {BreadcrumbModule} from "primeng/breadcrumb";


@Component({
  selector: 'app-general-lines-by-bank',
  standalone: true,
  imports: [
    CommonModule,
    TagModule,
    CardModule,
    PanelModule,
    TableModule,
    ButtonModule,
    RippleModule,
    DividerModule,
    DatePipe,
    DecimalPipe,
    ReactiveFormsModule,
    FormsModule,
    DropdownModule,
    InputNumberModule,
    TooltipModule,
    ToastModule,
    ConfirmDialogModule,
    InputSwitchModule,
    BreadcrumbModule,

  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './general-lignes-by-bank.component.html',
  styleUrls: ['./general-lignes-by-bank.component.scss']
})
export class GeneralLignesByBankComponent implements OnInit {
  @ViewChild('dtFunctionalLines') dtFunctionalLines: Table | undefined;

  bankId!: number;
  bankDetails: BanqueResponse | undefined;
  bankFunctionalLines: LigneFonctionnelleResponse[] = [];
  loading: boolean = true;
  loadingBankDetails: boolean = true;

  totalRecords: number = 0;
  rows: number = 10;
  first: number = 0;
  sortField: string | string[] = 'id';
  sortOrder: number = 1;

  functionalLineSearchForm!: FormGroup;
  natureLigneOptions: { label: string; value: number | null }[] = [];

  // NEW: Options for the 'actif' dropdown filter
  actifOptions: any[] = [
    { label: 'Toutes', value: null },
    { label: 'Encours', value: true },
    { label: 'Archivées', value: false }
  ];

  items: MenuItem[] | undefined;

  private route = inject(ActivatedRoute);
  private banqueService = inject(BanqueService);
  private ligneFonctionnelleService = inject(LigneFonctionnelleService);
  private natureLigneFonctionnelleService = inject(NatureLigneFonctionnelleService);
  private fb = inject(FormBuilder);
  private messageService = inject(MessageService);

  ngOnInit(): void {
    this.initFunctionalLineSearchForm();
    this.setupFunctionalLineFormListeners();
    this.loadNatureLigneOptions(); // This will now fetch from backend
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label:"Accueil" },
      { icon: 'pi pi-wallet', route: '/portail/comptes-bancaires', label:"Comptes bancaires" },
      { icon: 'pi pi-history', route: '/portail/select-for-history', label:"Détails & Historique" }
    ]

    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.bankId = +id;
        this.loadBankDetails();
        // loadBankFunctionalLines will be triggered by p-table's onLazyLoad
      }
    });
  }

  initFunctionalLineSearchForm(): void {
    this.functionalLineSearchForm = this.fb.group({
      natureLigneFonctionnelleId: [null],
      yearDepart: [null],
      actif: [true] // NEW: Default to showing only active functional lignePrefi
    });
  }

  setupFunctionalLineFormListeners(): void {
    this.functionalLineSearchForm.valueChanges
      .pipe(
        debounceTime(800),
        distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
      )
      .subscribe(() => {
        this.onFunctionalLineSearch();
      });
  }

  loadNatureLigneOptions(): void {
    this.natureLigneFonctionnelleService.getAllNatureLigneFonctionnellesList().subscribe({
      next: (natures) => {
        this.natureLigneOptions = [{ label: 'Toutes les natures', value: null }];
        natures.forEach(nature => {
          this.natureLigneOptions.push({ label: nature.label, value: nature.id! });
        });
      },
      error: (err) => {
        console.error('Error loading natures for filter:', err);
      }
    });
  }

  loadBankDetails(): void {
    this.loadingBankDetails = true;
    this.banqueService.getBanqueById(this.bankId).subscribe({
      next: (data: BanqueResponse) => {
        this.bankDetails = data;
        this.loadingBankDetails = false;
      },
      error: (error) => {
        console.error('Error loading bank details:', error);
        this.loadingBankDetails = false;
      }
    });
  }

  loadBankFunctionalLines(event: TableLazyLoadEvent): void {
    if (!this.bankId) {
      console.warn('Bank ID is not available to load functional lignePrefi.');
      this.loading = false;
      return;
    }

    this.loading = true;

    const first = event.first !== undefined ? event.first : this.first;
    const rows = event.rows !== undefined ? event.rows : this.rows;
    const sortField = event.sortField || this.sortField;
    const sortOrder = event.sortOrder === 1 ? 1 : -1;

    this.first = first;
    this.rows = rows as number;
    this.sortField = sortField;
    this.sortOrder = sortOrder;

    const page = (first as number) / (rows as number);

    const actualSortField = Array.isArray(sortField) ? sortField[0] : sortField;
    const sort = `${actualSortField},${sortOrder === 1 ? 'asc' : 'desc'}`;

    // Get search criteria from the form, including the 'actif' status
    const searchCriteria: LigneFonctionnelleSearchCriteria = {
      ...this.functionalLineSearchForm.value,
    };

    this.ligneFonctionnelleService.getLignesFonctionnellesByBanqueId(this.bankId, searchCriteria, page, this.rows, sort).subscribe({
      next: (pageResponse: PageResponse<LigneFonctionnelleResponse>) => {
        this.bankFunctionalLines = pageResponse.content;
        this.totalRecords = pageResponse.page.totalElements;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading bank functional lignePrefi:', error);
        this.loading = false;
      }
    });
  }

  onFunctionalLineSearch(): void {
    this.first = 0; // Reset to the first page on new search
    if (this.dtFunctionalLines) {
      this.dtFunctionalLines.first = 0;
      this.dtFunctionalLines.onLazyLoad.emit(this.dtFunctionalLines.createLazyLoadMetadata());
    } else {
      this.loadBankFunctionalLines({
        first: this.first,
        rows: this.rows,
        sortField: this.sortField,
        sortOrder: this.sortOrder,
        filters: {}
      } as TableLazyLoadEvent);
    }
  }
}
