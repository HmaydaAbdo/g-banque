import { Component, inject, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, FormsModule } from '@angular/forms';
// PrimeNG Modules
import { CardModule } from 'primeng/card';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { DividerModule } from 'primeng/divider';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { TooltipModule } from 'primeng/tooltip';
import { ToastModule } from 'primeng/toast';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { MenuItem, MessageService } from 'primeng/api';

// Application-specific Imports
import { BanqueResponse } from '../../../dtos/BanqueResponse';
import { BanqueService } from '../../../services/BanqueService';
import { MarcheService } from '../../../../Marches/service/marche-service.service';
import { NatureLigneFonctionnelleService } from '../../../../lignes-fonctionnelles/services/nature-ligne-fonctionnelle.service';
import { PageResponse } from '../../../../global/types/PageResponse';
import {debounceTime, distinctUntilChanged} from "rxjs/operators";
import {
  LigneFonctionnelleSpecifiqueResponse
} from "../../../../specific-Functional-Lignes/dtos/LigneFonctionnelleSpecifiqueResponse";
import {
  SpecificFunctionalLigneService
} from "../../../../specific-Functional-Lignes/service/SpecificFunctionalLigneService";
import {
  LigneFonctionnelleSpecifiqueSearchCriteria
} from "../../../../specific-Functional-Lignes/dtos/LigneFonctionnelleSpecifiqueSearchCriteria";
import {SkeletonModule} from "primeng/skeleton";

@Component({
  selector: 'app-specific-lignes-by-bank',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    CardModule,
    TableModule,
    ButtonModule,
    RippleModule,
    DividerModule,
    DropdownModule,
    InputNumberModule,
    TooltipModule,
    ToastModule,
    BreadcrumbModule,
    DatePipe,
    DecimalPipe,
    SkeletonModule
  ],
  providers: [MessageService],
  templateUrl: './specific-lignes-by-bank.component.html',
})
export class SpecificLignesByBankComponent implements OnInit {
  @ViewChild('dtSpecificLines') dtSpecificLines!: Table;

  bankId!: number;
  bankDetails: BanqueResponse | undefined;
  specificLines: LigneFonctionnelleSpecifiqueResponse[] = [];
  loading: boolean = true;
  loadingBankDetails: boolean = true;

  totalRecords: number = 0;
  rows: number = 10;
  first: number = 0;
  sortField: string | string[] = 'id';
  sortOrder: number = 1;

  searchForm!: FormGroup;
  natureLigneOptions: { label: string; value: number | null }[] = [];
  marchesOptions: { label: string; value: number | null }[] = [];
  items: MenuItem[] | undefined;

  marchesLoading: boolean = false;
  marchesTotalRecords: number = 0;

  marchesScrollerOptions = {
    delay: 250,
    showLoader: true,
    lazy: true,
    onLazyLoad: (event: any) => this.loadMarchesForDropdown(event)
  };

  private route = inject(ActivatedRoute);
  private specificLigneService = inject(SpecificFunctionalLigneService);
  private banqueService = inject(BanqueService);
  private marcheService = inject(MarcheService);
  private natureLigneFonctionnelleService = inject(NatureLigneFonctionnelleService);
  private fb = inject(FormBuilder);
  private messageService = inject(MessageService);

  ngOnInit(): void {
    this.initSearchForm();
    this.setupFormListeners();
    this.loadNatureLigneOptions(); // Only load this one on init

    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-wallet', route: '/portail/comptes-bancaires', label: 'Comptes Bancaires' }
    ];

    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.bankId = +id;
        this.loadBankDetails();
      }
    });
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      natureLigneFonctionnelleId: [null],
      marcheId: [null],
      yearDepart: [null],
      actif: [true],
    });
  }

  setupFormListeners(): void {
    this.searchForm.valueChanges
      .pipe(
        debounceTime(800),
        distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
      )
      .subscribe(() => {
        this.onSearch();
      });
  }

  loadBankDetails(): void {
    this.loadingBankDetails = true;
    this.banqueService.getBanqueById(this.bankId).subscribe({
      next: (data: BanqueResponse) => {
        this.bankDetails = data;
        this.loadingBankDetails = false;
      },
      error: () => {
        this.loadingBankDetails = false;
      }
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
      error: () => {}

    });
  }

  loadMarchesForDropdown(event: { first: number, last: number, filter?: string }): void {
    this.marchesLoading = true;
    const page = Math.floor(event.first / 10);
    const size = 10;
    const sort = 'nom,asc';

    this.marcheService.getAllMarchesForLazyLoad(page, size, sort).subscribe({
      next: (pageResponse) => {
        const newItems = pageResponse.content.map(marche => ({ label: marche.nom, value: marche.id! }));

        if (event.first === 0) {
          this.marchesOptions = [{ label: 'Tous les marchés', value: null }, ...newItems];
        } else {
          this.marchesOptions = [...this.marchesOptions, ...newItems];
        }

        this.marchesTotalRecords = pageResponse.page.totalElements;
        this.marchesLoading = false;
      },
      error: () => {
        this.marchesLoading = false;
      }
    });
  }

  loadSpecificFunctionalLines(event: TableLazyLoadEvent): void {
    if (!this.bankId) {
      this.loading = false;
      return;
    }

    this.loading = true;

    const first = event.first ?? this.first;
    const rows = event.rows ?? this.rows;
    const sortField = event.sortField || this.sortField;
    const sortOrder = event.sortOrder === 1 ? 1 : -1;

    const page = first / rows;
    const actualSortField = Array.isArray(sortField) ? sortField[0] : sortField;
    const sort = `${actualSortField},${sortOrder === 1 ? 'asc' : 'desc'}`;

    const searchCriteria = this.searchForm.value as LigneFonctionnelleSpecifiqueSearchCriteria;

    this.specificLigneService.getLignesFonctionnellesByBanqueId(this.bankId, searchCriteria, page, this.rows, sort).subscribe({
      next: (pageResponse: PageResponse<LigneFonctionnelleSpecifiqueResponse>) => {
        this.specificLines = pageResponse.content;
        this.totalRecords = pageResponse.page.totalElements;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  onSearch(): void {
    if (this.dtSpecificLines) {
      this.dtSpecificLines.first = 0;
      this.dtSpecificLines.onLazyLoad.emit(this.dtSpecificLines.createLazyLoadMetadata());
    }
  }

  clearSearch(): void {
    this.searchForm.reset({
      natureLigneFonctionnelleId: null,
      marcheId: null,
      yearDepart: null,
      actif: true,
      isExpired: false
    });
  }
}
