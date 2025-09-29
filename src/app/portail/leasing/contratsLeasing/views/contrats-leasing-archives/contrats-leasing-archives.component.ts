import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import { ContratLeasingResponseDTO} from "../../dtos/contrat-leasing";
import {FormBuilder, FormGroup, ReactiveFormsModule} from "@angular/forms";
import {TypeContratLeasing} from "../../dtos/type-contrat-leasing";
import {Table, TableLazyLoadEvent, TableModule} from "primeng/table";
import {debounceTime, distinctUntilChanged, Subscription, take} from "rxjs";
import {ConfirmationService, MenuItem, MessageService, PrimeTemplate} from "primeng/api";
import {ContratLeasingService} from "../../services/contrat-leasing.service";
import {OrganismeLeasingService} from "../../../organismesLeasing/services/organisme-leasing.service";
import {BanqueService} from "../../../../comptes-bancaire/services/BanqueService";
import {Router, RouterLinkActive} from "@angular/router";
import {LeasingContractWizardService} from "../../services/LeasingContractWizardService";
import {PageResponse} from "../../../../global/types/PageResponse";
import {OrganismeLeasingResponse} from "../../../organismesLeasing/dtos/OrganismeLeasingResponse";
import {BanqueResponse} from "../../../../comptes-bancaire/dtos/BanqueResponse";
import {ContratLeasingSearchDTO} from "../../dtos/contrat-leasing-search";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {Button, ButtonDirective} from "primeng/button";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {ContratLeasingDetailsComponent} from "../contrat-leasing-details/contrat-leasing-details.component";
import {DatePipe, NgClass, NgIf} from "@angular/common";
import {DialogModule} from "primeng/dialog";
import {DropdownModule} from "primeng/dropdown";
import {InputTextModule} from "primeng/inputtext";
import {Ripple} from "primeng/ripple";
import {TagModule} from "primeng/tag";
import {ToastModule} from "primeng/toast";
import {TooltipModule} from "primeng/tooltip";

@Component({
  selector: 'app-contrats-leasing-archives',
  standalone: true,
  imports: [
    BreadcrumbModule,
    Button,
    ButtonDirective,
    ConfirmDialogModule,
    ContratLeasingDetailsComponent,
    DatePipe,
    DialogModule,
    DropdownModule,
    InputTextModule,
    NgIf,
    PrimeTemplate,
    ReactiveFormsModule,
    Ripple,
    RouterLinkActive,
    TableModule,
    TagModule,
    ToastModule,
    TooltipModule,
    NgClass
  ],
  templateUrl: './contrats-leasing-archives.component.html',
  styleUrl: './contrats-leasing-archives.component.scss'
})
export class ContratsLeasingArchivesComponent implements OnInit, OnDestroy {

  contrats: ContratLeasingResponseDTO[] = [];
  totalRecords: number = 0;
  loading: boolean = false;

  searchForm!: FormGroup;

  typeContratOptions = Object.values(TypeContratLeasing).map(type => ({ label: type, value: type }));
  organismeLeasingOptions: { label: string, value: number }[] = [];
  banqueOptions: { label: string, value: number }[] = [];



  @ViewChild('dt') dt: Table | undefined;

  private subscriptions: Subscription[] = [];

  private lastLazyLoadEvent: TableLazyLoadEvent = {
    first: 0,
    rows: 10,
    sortField: 'id',
    sortOrder: 1,
    filters: {}
  };

  displayDetailsDialog: boolean = false;
  selectedContratDetails: ContratLeasingResponseDTO | null = null;
  items: MenuItem[] | undefined;


  constructor(
    private contratLeasingService: ContratLeasingService,
    private organismeLeasingService: OrganismeLeasingService,
    private banqueService: BanqueService,
    private router: Router,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private fb: FormBuilder,
    private wizardService: LeasingContractWizardService
  ) { }

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label:"Accueil" },
      { icon: 'pi pi-list', route: '/portail/leasing/contratsLeasing/archives', label:"Les Contracts Leasing Archivés" },
    ]

    this.initSearchForm();
    this.loadDropdownOptions();

    const numeroContratSub = this.searchForm.get('numeroContrat')?.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => this.onSearch());

    const immobilisationSub = this.searchForm.get('immobilisation')?.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => this.onSearch());

    const typeContratSub = this.searchForm.get('typeContrat')?.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => this.onSearch());

    const organismeLeasingSub = this.searchForm.get('organismeLeasingId')?.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => this.onSearch());

    const banqueDePrelevementSub = this.searchForm.get('banqueDePrelevementId')?.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => this.onSearch());




    this.subscriptions.push(numeroContratSub!, immobilisationSub!, typeContratSub!, organismeLeasingSub!, banqueDePrelevementSub!);

    this.loadContrats(this.lastLazyLoadEvent);
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      numeroContrat: [''],
      immobilisation: [''],
      typeContrat: [null],
      organismeLeasingId: [null],
      banqueDePrelevementId: [null],
      completed: [false]
    });
  }

  loadDropdownOptions(): void {
    this.organismeLeasingService.searchOrganismeLeasings({}, 0, 1000)
      .subscribe({
        next: (response: PageResponse<OrganismeLeasingResponse>) => {
          this.organismeLeasingOptions = response.content.map(org => ({
            label: org.nom,
            value: org.id!
          }));
        },
        error: (error) => {
          console.error('Error loading organisme leasing options:', error);
          this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load leasing organisms.' });
        }
      });

    this.banqueService.getAllBanques({}, 0, 1000)
      .subscribe({
        next: (response: PageResponse<BanqueResponse>) => {
          this.banqueOptions = response.content.map(banque => ({
            label: banque.nom,
            value: banque.id!
          }));
        },
        error: (error) => {
          console.error('Error loading bank options:', error);
          this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load banks.' });
        }
      });
  }

  loadContrats(event: TableLazyLoadEvent) {
    this.loading = true;
    this.lastLazyLoadEvent = event;

    const currentPage = event.first! / event.rows!;
    const pageSize = event.rows!;

    let sort: string = 'id,asc';
    if (event.sortField && event.sortOrder) {
      const actualSortField = Array.isArray(event.sortField) ? event.sortField[0] : event.sortField;
      sort = `${actualSortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`;
    }

    const searchCriteria: ContratLeasingSearchDTO = {
      numeroContrat: this.searchForm.get('numeroContrat')?.value,
      immobilisation: this.searchForm.get('immobilisation')?.value,
      typeContrat: this.searchForm.get('typeContrat')?.value,
      organismeLeasingId: this.searchForm.get('organismeLeasingId')?.value,
      banqueDePrelevementId: this.searchForm.get('banqueDePrelevementId')?.value
    };

    this.contratLeasingService.getInactiveContrats(
      searchCriteria,
      currentPage,
      pageSize,
      sort
    ).subscribe({
      next: (page: PageResponse<ContratLeasingResponseDTO>) => {
        this.contrats = page.content;
        this.totalRecords = page.page.totalElements;
        this.loading = false;
      },
      error: (error) => {
        this.loading = false;
        console.error('Erreur lors du chargement des contrats:', error);
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement des contrats.' });
      }
    });
  }

  onSearch(): void {
    if (this.dt) {
      this.dt.first = 0;
      this.loadContrats(this.dt.createLazyLoadMetadata());
    } else {
      this.loadContrats({
        ...this.lastLazyLoadEvent,
        first: 0,
        filters: {}
      });
    }
  }

  clearSearch(): void {
    this.searchForm.reset({
      numeroContrat: '',
      immobilisation: '',
      typeContrat: null,
      organismeLeasingId: null,
      banqueDePrelevementId: null,
      completed: null
    });

    if (this.dt) {
      this.dt.clear();
      this.dt.first = 0;
      this.loadContrats(this.dt.createLazyLoadMetadata());
    } else {
      this.loadContrats(this.lastLazyLoadEvent);
    }
  }



  onRowSelect(event: any): void {
    const contratId = event.data.id;
    this.getContratDetailsForDialog(contratId);
  }

  getContratDetailsForDialog(id: number): void {
    this.loading = true;
    this.contratLeasingService.getContratLeasingById(id).subscribe({
      next: (contratDetails: ContratLeasingResponseDTO) => {
        this.selectedContratDetails = contratDetails;
        this.displayDetailsDialog = true;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error fetching contract details:', error);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load contract details.' });
        this.loading = false;
      }
    });
  }

  hideDetailsDialog(): void {
    this.displayDetailsDialog = false;
    this.selectedContratDetails = null;
  }

  viewAmortizationTable(contratId: number): void {
    this.router.navigate(['portail/leasing/contratsLeasing', contratId, 'tableau-amortissement']);
  }



}
