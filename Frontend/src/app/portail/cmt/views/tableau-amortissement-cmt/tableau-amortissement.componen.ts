import {Component, OnDestroy, OnInit, ViewChild, inject} from '@angular/core';
import { AVERTISSEMENT_RETOUR_EN_COURS } from '../../../global/utils/bascule-statut.utils';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
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
import {BreadcrumbModule} from "primeng/breadcrumb";
import {AuthService} from "../../../../auth/services/auth.service";
import {finalize} from "rxjs/operators";
import {CalendarModule} from "primeng/calendar";


@Component({
  selector: 'app-tableau-amortissement',
  templateUrl: './tableau-amortissement.componen.html',
  styleUrls: ['./tableau-amortissement.componen.scss'],
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
    ProgressSpinnerModule,
    DividerModule,
    InputNumberModule,
    InputSwitchModule,
    RippleModule,
    ConfirmDialogModule,
    FormsModule,
    BreadcrumbModule,
    CalendarModule
  ],
  providers: [MessageService, ConfirmationService]
})
export class TableauAmortissementComponent implements OnInit, OnDestroy {

  creditId: number | null = null;
  tableauDetails: TableauAmortissementCMTResponseDto | null = null;
  echeances: EcheanceCMTResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = true;
  exporting: boolean = false;
  errorMessage: string | null = null;

  searchForm!: FormGroup;
  statutOptions: { label: string, value: StatutEcheanceCMT }[] = [];

  @ViewChild('dt') dt: Table | undefined;

  private subscriptions: Subscription[] = [];
  private lastLazyLoadEvent: TableLazyLoadEvent = {};
  items: MenuItem[] | undefined;

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly echeanceCmtService = inject(EcheanceCmtService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);

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
      dateEcheanceDebut: [null as Date | null],
      dateEcheanceFin: [null as Date | null],
      dateOperationDebut: [null as Date | null],
      dateOperationFin: [null as Date | null],
      dateValeurDebut: [null as Date | null],
      dateValeurFin: [null as Date | null],
      statutPaiement: [null]
    });
  }

  initDropdownOptions(): void {
    this.statutOptions = [
      { label: 'Payé', value: StatutEcheanceCMT.PAYE },
      { label: 'Prévu', value: StatutEcheanceCMT.PREVU }
    ];
  }

  /** Affiche/masque le panneau de recherche par période (fermé par défaut). */
  showDatePanel = false;

  toggleDatePanel(): void {
    this.showDatePanel = !this.showDatePanel;
  }

  get activeDateFilterCount(): number {
    const v = this.searchForm.value;
    let count = 0;
    if (v.dateEcheanceDebut || v.dateEcheanceFin) count++;
    if (v.dateOperationDebut || v.dateOperationFin) count++;
    if (v.dateValeurDebut || v.dateValeurFin) count++;
    return count;
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
      dateEcheanceDebut: this.searchForm.get('dateEcheanceDebut')?.value ?? undefined,
      dateEcheanceFin: this.searchForm.get('dateEcheanceFin')?.value ?? undefined,
      dateOperationDebut: this.searchForm.get('dateOperationDebut')?.value ?? undefined,
      dateOperationFin: this.searchForm.get('dateOperationFin')?.value ?? undefined,
      dateValeurDebut: this.searchForm.get('dateValeurDebut')?.value ?? undefined,
      dateValeurFin: this.searchForm.get('dateValeurFin')?.value ?? undefined,
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
      dateEcheanceDebut: null,
      dateEcheanceFin: null,
      dateOperationDebut: null,
      dateOperationFin: null,
      dateValeurDebut: null,
      dateValeurFin: null,
      statutPaiement: null
    });
    if (this.dt) {
      this.dt.first = 0;
      this.loadEcheances(this.dt.createLazyLoadMetadata());
    } else {
      this.loadEcheances(this.lastLazyLoadEvent);
    }
  }

  exportPdf(): void {
    if (!this.creditId) return;
    this.exporting = true;

    const searchCriteria: EcheanceCMTSearchDto = {
      numeroEcheance: this.searchForm.get('numeroEcheance')?.value,
      dateEcheanceDebut: this.searchForm.get('dateEcheanceDebut')?.value ?? undefined,
      dateEcheanceFin: this.searchForm.get('dateEcheanceFin')?.value ?? undefined,
      dateOperationDebut: this.searchForm.get('dateOperationDebut')?.value ?? undefined,
      dateOperationFin: this.searchForm.get('dateOperationFin')?.value ?? undefined,
      dateValeurDebut: this.searchForm.get('dateValeurDebut')?.value ?? undefined,
      dateValeurFin: this.searchForm.get('dateValeurFin')?.value ?? undefined,
      statutPaiement: this.searchForm.get('statutPaiement')?.value,
    };

    this.echeanceCmtService.exportTableauAmortissementPdf(this.creditId, searchCriteria).subscribe({
      next: (blob: Blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `tableau_amortissement_cmt_${this.creditId}_${new Date().toISOString().slice(0, 10)}.pdf`;
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
    return this.authService.isSystemAdmin()
  }

  /** Le retour « prévu » d'une échéance payée peut supprimer sa transaction de rapprochement : confirmation explicite. */
  onStatutChange(echeance: EcheanceCMTResponseDto): void {
    if (echeance.statutPaiement !== StatutEcheanceCMT.PAYE) {
      this.basculerStatut(echeance);
      return;
    }
    this.confirmationService.confirm({
      message: 'Voulez-vous remettre cette échéance à « prévu » ? ' + AVERTISSEMENT_RETOUR_EN_COURS,
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      accept: () => this.basculerStatut(echeance),
      reject: () => this.loadEcheances(this.lastLazyLoadEvent)
    });
  }

  private basculerStatut(echeance: EcheanceCMTResponseDto): void {
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
        this.errorMessage = 'Échec de la mise à jour du statut. ' + (error.message || 'Veuillez réessayer.');
        this.loadEcheances(this.lastLazyLoadEvent);
      }
    });


  }

  /** Navigue vers la page d'édition dédiée de cette échéance (remplace l'ancien dialogue). */
  editEcheance(echeance: EcheanceCMTResponseDto): void {
    this.router.navigate(['/portail/cmt/echeances/edit', echeance.id]);
  }

}
