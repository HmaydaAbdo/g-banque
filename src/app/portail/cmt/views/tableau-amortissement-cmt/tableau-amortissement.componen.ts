import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ActivatedRoute, Router} from '@angular/router';
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule} from '@angular/forms';
// PrimeNG Modules
import {Table, TableLazyLoadEvent, TableModule} from 'primeng/table';
import {debounceTime, distinctUntilChanged, Subscription} from 'rxjs';
import {ButtonModule} from 'primeng/button';
import {ToastModule} from 'primeng/toast';
import {TagModule} from 'primeng/tag';
import {TooltipModule} from 'primeng/tooltip';
import {DropdownModule} from 'primeng/dropdown';
import {ConfirmDialogModule} from 'primeng/confirmdialog';
import {ConfirmationService, MenuItem, MessageService} from 'primeng/api';
import {ProgressSpinnerModule} from "primeng/progressspinner";
import {DividerModule} from "primeng/divider";
import {InputNumberModule} from "primeng/inputnumber";
import {InputSwitchModule} from "primeng/inputswitch";
import {RippleModule} from 'primeng/ripple';


// Services and DTOs
import {EcheanceCmtService} from '../../services/EcheanceCmtService';
import {TableauAmortissementCMTResponseDto} from '../../dtos/TableauAmortissementCMTResponseDto';
import {EcheanceCMTResponseDto} from '../../dtos/EcheanceCMTResponseDto';
import {StatutEcheanceCMT} from '../../enums/StatutEcheanceCMT';
import {EcheanceCMTSearchDto} from '../../dtos/EcheanceCMTSearchDto';
import {DialogModule} from "primeng/dialog";
import {InputTextModule} from "primeng/inputtext";
import {InputTextareaModule} from "primeng/inputtextarea";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {AuthService} from "../../../../auth/services/auth.service";
import {Role} from "../../../../auth/enum/Role";
import {finalize} from "rxjs/operators";


@Component({
  selector: 'app-tableau-amortissement',
  templateUrl: './tableau-amortissement.componen.html',
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    ButtonModule,
    ToastModule,
    TagModule,
    TooltipModule,
    DropdownModule,
    ReactiveFormsModule,
    ConfirmDialogModule,
    ProgressSpinnerModule,
    DividerModule,
    InputNumberModule,
    InputSwitchModule,
    RippleModule,
    FormsModule,
    DialogModule,
    InputTextModule,
    InputTextareaModule,
    BreadcrumbModule
  ],
  providers: [MessageService, ConfirmationService]
})
export class TableauAmortissementComponent implements OnInit, OnDestroy {

  creditId: number | null = null;
  tableauDetails: TableauAmortissementCMTResponseDto | null = null;
  echeances: EcheanceCMTResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = true;
  errorMessage: string | null = null;

  searchForm!: FormGroup;
  statutOptions: { label: string, value: StatutEcheanceCMT }[] = [];
  yearOptions: { label: string, value: number }[] = [];

  @ViewChild('dt') dt: Table | undefined;

  private subscriptions: Subscription[] = [];
  private lastLazyLoadEvent: TableLazyLoadEvent = {};
  items: MenuItem[] | undefined;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private echeanceCmtService: EcheanceCmtService,
    private messageService: MessageService,
    private fb: FormBuilder,
    private authService: AuthService,
  ) { }

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label:"Accueil" },
      { icon: 'pi pi-list', route: '/portail/cmt', label:"Les Crédits Moyens Termes" },
      { icon: 'pi pi-list', route: '/portail/cmt', label:"Tableau d'amortissement" },
    ]
    this.initSearchForm();
    this.initDropdownOptions();


    // Subscribe to form value changes to trigger search
    const formChangesSub = this.searchForm.valueChanges
      .pipe(debounceTime(500), distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)))
      .subscribe(() => this.onSearch());
    this.subscriptions.push(formChangesSub);

    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.creditId = Number(id);
        // Explicitly trigger the initial data load
        this.loadEcheances({});
      } else {
        this.errorMessage = 'ID de crédit manquant.';
        this.loading = false;
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: this.errorMessage, life: 3000 });
        this.router.navigate(['/portail/cmt/list']);
      }
    });
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      numeroEcheance: [null],
      month: [null],
      year: [null],
      statutPaiement: [null]
    });
  }

  initDropdownOptions(): void {
    this.statutOptions = [
      { label: 'Payé', value: StatutEcheanceCMT.PAYE },
      { label: 'Prévu', value: StatutEcheanceCMT.PREVU }
    ];

    const currentYear = new Date().getFullYear();
    for (let i = currentYear - 5; i <= currentYear + 10; i++) {
      this.yearOptions.push({ label: i.toString(), value: i });
    }
  }

  loadEcheances(event: TableLazyLoadEvent): void {
    if (!this.creditId) return;

    this.loading = true;
    this.errorMessage = null;
    this.lastLazyLoadEvent = event;

    const currentPage = event.first! / event.rows! || 0;
    const pageSize = event.rows! || 10;

    let sort: string = 'dateEcheance,asc';
    if (event.sortField && event.sortOrder) {
      sort = `${event.sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`;
    }

    const searchCriteria: EcheanceCMTSearchDto = {
      numeroEcheance: this.searchForm.get('numeroEcheance')?.value,
      month: this.searchForm.get('month')?.value,
      year: this.searchForm.get('year')?.value,
      statutPaiement: this.searchForm.get('statutPaiement')?.value,
    };

    this.echeanceCmtService.getTableauAmortissementForCredit(this.creditId, searchCriteria, { page: currentPage, size: pageSize, sort }).subscribe({
      next: (tableau) => {
        this.tableauDetails = tableau;
        this.echeances = tableau.echeances.content;
        this.totalRecords = tableau.echeances.page.totalElements;
        this.loading = false;
      },
      error: (error) => {
        this.errorMessage = error.message;
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: error.message, life: 3000 });
        this.loading = false;
      }
    });
  }

  onSearch(): void {
    if (this.dt) {
      this.dt.first = 0;
      this.loadEcheances(this.dt.createLazyLoadMetadata());
    } else {
      this.loadEcheances({
        ...this.lastLazyLoadEvent,
        first: 0
      });
    }
  }

  clearSearch(): void {
    this.searchForm.reset({
      numeroEcheance: null,
      month: null,
      year: null,
      statutPaiement: null
    });
    if (this.dt) {
      this.dt.first = 0;
      this.loadEcheances(this.dt.createLazyLoadMetadata());
    } else {
      this.loadEcheances(this.lastLazyLoadEvent);
    }
  }

  getStatusSeverity(statut: StatutEcheanceCMT): "success" | "warning" | "info" | "danger" | "secondary" | "contrast" | undefined {
    switch (statut) {
      case StatutEcheanceCMT.PAYE:
        return 'success';
      case StatutEcheanceCMT.PREVU:
        return 'warning';
      default:
        return 'info';
    }
  }


  isAdmin():boolean {
    return this.authService.hasRole(Role.ADMIN)
  }

  onStatutChange(echeance: EcheanceCMTResponseDto): void {
    this.loading = true; // Set general loading indicator or use a specific one
    this.echeanceCmtService.toggleStatus(echeance.id).pipe(
      finalize(() => {
        this.loading = false;
      })
    ).subscribe({
      next: () => {
        // Re-fetch the entire table to update the summary totals (Endettement, Montants payés, etc.)
        this.loadEcheances(this.lastLazyLoadEvent);
      },
      error: (error) => {
        console.error('Error toggling  echeance status  :', error);
        this.errorMessage = 'Échec de la mise à jour du statut. ' + (error.message || 'Veuillez réessayer.');
        this.messageService.add({severity: 'error', summary: 'Erreur', detail: this.errorMessage});

      }
    });


  }

}
