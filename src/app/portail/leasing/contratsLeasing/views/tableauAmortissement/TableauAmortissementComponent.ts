import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule} from '@angular/forms';
import {CommonModule} from '@angular/common';
import {Subscription} from 'rxjs';
import {debounceTime, distinctUntilChanged, finalize} from 'rxjs/operators';

// PrimeNG Modules
import {CardModule} from 'primeng/card';
import {ButtonModule} from 'primeng/button';
import {ToastModule} from 'primeng/toast';
import {InputTextModule} from 'primeng/inputtext';
import {DropdownModule} from 'primeng/dropdown';
import {TableLazyLoadEvent, TableModule} from 'primeng/table';
import {TagModule} from 'primeng/tag';
import {TooltipModule} from 'primeng/tooltip';
import {MenuItem, MessageService} from 'primeng/api';
import {ProgressSpinnerModule} from 'primeng/progressspinner';
import {DividerModule} from 'primeng/divider';
import {InputNumberModule} from 'primeng/inputnumber';

// Make sure these paths are correct based on your project structure
import {TableauAmortissement} from "../../dtos/table-amortissement";
import {EcheanceLeasingResponse} from "../../dtos/EcheanceLeasingResponse";
import {StatutEcheanceLeasing} from "../../dtos/StatutEcheanceLeasing";
import {EcheanceLeasingService} from "../../services/EcheanceLeasingService";
import {EcheanceLeasingSearchDTO} from "../../dtos/echeance-leasing-search";
import {InputSwitchModule} from "primeng/inputswitch";
import {DialogModule} from "primeng/dialog";
import {InputTextareaModule} from "primeng/inputtextarea";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {AuthService} from "../../../../../auth/services/auth.service";


@Component({
  selector: 'app-tableau-amortissement',
  templateUrl: './tableau-amortissement.component.html',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    CardModule,
    ButtonModule,
    ToastModule,
    InputTextModule,
    DropdownModule,
    TableModule,
    TagModule,
    TooltipModule,
    ProgressSpinnerModule,
    DividerModule,
    InputNumberModule,
    InputSwitchModule,
    FormsModule,
    DialogModule,
    InputTextareaModule,
    BreadcrumbModule
  ],
  providers: [MessageService]
})
export class TableauAmortissementComponent implements OnInit, OnDestroy {

  contratId!: number;
  tableauAmortissement: TableauAmortissement | null = null;
  echeances: EcheanceLeasingResponse[] = [];
  totalEcheancesRecords: number = 0;
  loading: boolean = true;
  errorMessage: string | null = null;

  searchForm!: FormGroup;
  statutLeasingOptions = Object.values(StatutEcheanceLeasing).map(status => ({ label: status, value: status }));

  private subscriptions: Subscription[] = [];



  protected lastLazyLoadEvent: TableLazyLoadEvent = {
    first: 0,
    rows: 3,
    sortField: 'dateEcheance',
    sortOrder: 1,
    filters: {}
  };
  items: MenuItem[] | undefined;

  constructor(
    private route: ActivatedRoute,
    private echeanceLeasingService: EcheanceLeasingService,
    private messageService: MessageService,
    private fb: FormBuilder,
    private authService:AuthService
  ) { }

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label:"Accueil" },
      { icon: 'pi pi-list', route: '/portail/leasing', label:"Les Contracts Leasing" },
      { icon: 'pi pi-table', route: '/portail/leasing', label:"Tableau d'amortissement" },
    ]
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.contratId = +id;
        this.initSearchForm();
        this.loadTableauAmortissement(this.lastLazyLoadEvent);
      } else {
        this.errorMessage = 'Contract ID not provided in the URL.';
        this.loading = false;
      }
    });

  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  /**
   * Initializes the reactive search form and sets up subscriptions for value changes.
   */
  initSearchForm(): void {
    this.searchForm = this.fb.group({
      numeroEcheance: [null],
      month: [null],
      year: [null],
      statutPaiement: [null]
    });

    const searchSub = this.searchForm.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
    ).subscribe(() => {
      this.lastLazyLoadEvent.first = 0;
      this.loadTableauAmortissement(this.lastLazyLoadEvent);
    });
    this.subscriptions.push(searchSub);
  }

  /**
   * Loads the amortization table and its installments based on lazy load event and search criteria.
   * This method is called initially and whenever the p-table triggers a lazyLoad event (pagination, sort, filter).
   * @param event PrimeNG TableLazyLoadEvent containing current table state.
   */
  loadTableauAmortissement(event: TableLazyLoadEvent): void {
    if (!this.contratId) {
      console.warn('Cannot load amortization table: contratId is missing.');
      this.loading = false;
      return;
    }

    this.loading = true;
    this.errorMessage = null;
    this.lastLazyLoadEvent = event;

    const currentPage = event.first! / event.rows!;
    const pageSize = event.rows!;

    let sortBy: string | undefined;
    let sortDirection: 'asc' | 'desc' | undefined;

    if (event.sortField) {
      sortBy = Array.isArray(event.sortField) ? event.sortField[0] : event.sortField;
      sortDirection = event.sortOrder === 1 ? 'asc' : 'desc';
    } else {
      sortBy = this.lastLazyLoadEvent.sortField as string;
      sortDirection = this.lastLazyLoadEvent.sortOrder === 1 ? 'asc' : 'desc';
    }

    const searchCriteria: EcheanceLeasingSearchDTO = {
      numeroEcheance: this.searchForm.get('numeroEcheance')?.value,
      month: this.searchForm.get('month')?.value,
      year: this.searchForm.get('year')?.value,
      statutPaiement: this.searchForm.get('statutPaiement')?.value
    };

    this.echeanceLeasingService.getTableauAmortissementForContrat(
      this.contratId,
      searchCriteria,
      currentPage,
      pageSize,
      sortBy,
      sortDirection
    ).pipe(
      finalize(() => {
        this.loading = false;
      })
    ).subscribe({
      next: (data: TableauAmortissement) => {
        this.tableauAmortissement = data;
        this.echeances = data.echeances.content;
        this.totalEcheancesRecords = data.echeances.page.totalElements;
      },
      error: (error) => {
        console.error('Error fetching tableau amortissement:', error);
        this.errorMessage = 'Failed to load amortization table. ' + (error.message || 'Please try again.');
      }
    });
  }


  /**
   * Resets the search form fields to their initial null/empty state.
   * The form's valueChanges subscription will automatically trigger a new data load.
   */
  clearSearch(): void {
    this.searchForm.reset({
      numeroEcheance: null,
      month: null,
      year: null,
      statutPaiement: null
    });
  }

  /**
   * Helper function to determine the severity of the status tag.
   * @param status The StatutEcheanceLeasing value.
   * @returns PrimeNG severity string ('success', 'info', 'warning', 'danger').
   */
  getStatutSeverity(status: StatutEcheanceLeasing): "success" | "info" | "warning" | "danger" | "secondary" | "contrast" | undefined {
    switch (status) {
      case StatutEcheanceLeasing.PAYE:
        return 'success';
      case StatutEcheanceLeasing.PREVU:
        return 'info';

      default:
        return 'warning'; // Default for any unhandled status
    }
  }

  /**
   * Handles the toggle change to mark an installment as paid or revert it (optional: depends on API).
   * Note: The current API only supports mark-as-paid, so we'll only allow switching to 'PAYE'.
   * @param echeance The installment object being changed.
   */
  onStatutChange(echeance: EcheanceLeasingResponse): void {
    this.loading = true; // Set general loading indicator or use a specific one
    this.echeanceLeasingService.toggleStatus(echeance.id).pipe(
        finalize(() => {
          this.loading = false;
        })
      ).subscribe({
        next: () => {
          // Re-fetch the entire table to update the summary totals (Endettement, Montants payés, etc.)
          this.loadTableauAmortissement(this.lastLazyLoadEvent);
        },
        error: (error) => {
          console.error('Error toggling  echeance status  :', error);
          this.errorMessage = 'Échec de la mise à jour du statut. ' + (error.message || 'Veuillez réessayer.');
        }
      });


  }

  isAdmin():boolean{
    return this.authService.isSystemAdmin()
  }
}
