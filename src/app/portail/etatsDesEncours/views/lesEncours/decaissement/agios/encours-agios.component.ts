import {Component, OnInit, ViewChild} from "@angular/core";
import {CommonModule, DatePipe, DecimalPipe, NgFor} from "@angular/common";
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators} from "@angular/forms";
import {Table, TableLazyLoadEvent, TableModule} from "primeng/table";
import {DropdownModule} from "primeng/dropdown";
import {ButtonModule} from "primeng/button";
import {RippleModule} from "primeng/ripple";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {ToastModule} from "primeng/toast";
import {TooltipModule} from "primeng/tooltip";
import {InputTextModule} from "primeng/inputtext";
import {CardModule} from "primeng/card";
import {DialogModule} from "primeng/dialog";
import {CalendarModule} from "primeng/calendar";
import {InputTextareaModule} from "primeng/inputtextarea";
import {MenuItem, MessageService, SelectItem} from "primeng/api";
import {Decimal} from "decimal.js";
import {ActivatedRoute} from "@angular/router";
import {RapprochemntService} from "../../../../rapprochement/services/rapprochement/rapprochemnt.service";
import {AgiosResponseDto} from "../../../../../agios/dtos/AgiosResponseDto";
import {AgiosService} from "../../../../../agios/service/agios.service";
import {AgiosSearchDto} from "../../../../../agios/dtos/AgiosSearchDto";
import {EncoursAgiosDto} from "../../../../../agios/dtos/EncoursAgiosDto";
import {AgiosValidationRequest} from "../../../../rapprochement/dtos/agios/agios-validation-request.dto";

@Component({
  selector: 'app-encours-agios',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    DropdownModule,
    ButtonModule,
    RippleModule,
    BreadcrumbModule,
    ToastModule,
    TooltipModule,
    DatePipe,
    DecimalPipe,
    NgFor,
    InputTextModule,
    CardModule,
    DialogModule,
    ReactiveFormsModule,
    CalendarModule,
    InputTextareaModule
  ],
  templateUrl: './encours-agios.component.html',
  providers: [MessageService]
})
export class EncoursAgiosComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  agios: AgiosResponseDto[] = [];
  totalRecords = 0;
  loading = true;
  totalEncours!: Decimal;
  banqueId?: number;

  displayModal: boolean = false;
  selectedAgio: any = null;
  rapprochementForm!: FormGroup;
  libelleFilter: string | null = null;

  items: MenuItem[] = [];
  tvaOptions: SelectItem[] = [
    { label: '0%', value: 0 },
    { label: '10%', value: 10 },
    { label: '14%', value: 14 },
    { label: '20%', value: 20 }
  ];

  isSubmitting: boolean = false;

  constructor(
    private agiosService: AgiosService,
    private messageService: MessageService,
    private route: ActivatedRoute,
    private fb: FormBuilder,
    private rapprochementService: RapprochemntService,
  ) {}

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/agios', label: 'Agios' },
      { label: 'Encours' }
    ];


    // Subscribe to the parent route to get the banqueId
    this.route.parent!.paramMap.subscribe(params => {
      const id = params.get('banqueId');
      if (id) {
        this.banqueId = +id;

        // Trigger the table's lazy load once the ID is ready
        if (this.table) {
          this.table.reset();
        } else {
          this.loadAgios({});
        }
      } else {
        this.messageService.add({
          severity: 'warn',
          summary: 'Attention',
          detail: 'No bank ID found in the URL.'
        });
      }
    });

    this.rapprochementForm = this.fb.group({
      dateEmission: [null, Validators.required],
      dateOperation: [null, Validators.required],
      dateValeur: [null, Validators.required],
      observation: ['', Validators.required],
      libelle: ['', Validators.required],
      tvaDeductible: [null, Validators.required],
    });
  }

  loadAgios(event: TableLazyLoadEvent) {
    this.loading = true;

    // Guard clause to prevent API call if banqueId is not set
    if (!this.banqueId) {
      this.agios = [];
      this.totalRecords = 0;
      this.totalEncours = new Decimal(0);
      this.loading = false;
      return;
    }

    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sortField = event.sortField ?? 'dateEmission';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    const searchCriteria: AgiosSearchDto = {
      libelle: this.libelleFilter || undefined
    };

    this.agiosService.getOutstandingAgios(this.banqueId, searchCriteria, page, size, sort)
      .subscribe({
        next: (res: EncoursAgiosDto) => {
          this.agios = res.page.content;
          this.totalRecords = res.page.page.totalElements;
          this.totalEncours = res.totalDesEncours;
          this.loading = false;
        },
        error: (err) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de charger les agios en cours.'
          });
          this.loading = false;
          console.error(err);
        }
      });
  }

  // Ouvrir le dialog de validation
  openRapprochementModal(agios: any): void {
    this.selectedAgio = agios;
    this.rapprochementForm.reset();
    this.rapprochementForm.patchValue({
      dateEmission: new Date(agios.dateEmission),
      observation: agios.observation,
      libelle: agios.libelle,
    })
    this.displayModal = true;
  }



  // Soumettre la validation
  submitRapprochement(): void {

    if (this.rapprochementForm.invalid || !this.selectedAgio) {
      this.rapprochementForm.markAllAsTouched();
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Veuillez remplir tous les champs.',
      });
      return;
    }

    const request: AgiosValidationRequest = {
      idAgio: this.selectedAgio.id,
      dateOperation: this.rapprochementForm.value.dateOperation,
      dateEmission:this.rapprochementForm.value.dateEmission,
      dateValeur: this.rapprochementForm.value.dateValeur,
      observation: this.rapprochementForm.value.observation,
      libelle:this.rapprochementForm.value.libelle,
      tva:this.rapprochementForm.value.tvaDeductible
    };

    this.isSubmitting = true;

    this.rapprochementService.agiosToRapprochement(request).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: 'Agios validée avec succès.',
        });
        this.displayModal = false;
        this.isSubmitting = false;
        this.table.reset();
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Une erreur est survenue.',
        });
        this.isSubmitting = false;
      },
    });
  }



  applyFilters() {
    if (this.table) {
      setTimeout(() => this.table.reset(), 0);
    }
  }

  isInvalid(controlName: string): boolean {
    const control = this.rapprochementForm.get(controlName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }
}
