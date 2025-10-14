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
import {FraisBancaireResponseDto} from "../../../../../frais-bancaire/dtos/FraisBancaireResponseDto";
import {FraisBancaireService} from "../../../../../frais-bancaire/service/FraisBancaireService";
import {FraisBancaireSearchDto} from "../../../../../frais-bancaire/dtos/FraisBancaireSearchDto";
import {EncoursFraisBancaireDto} from "../../../../../frais-bancaire/dtos/EncoursFraisBancaireDto";
import {FraisBancaireValidationRequest} from "../../../../rapprochement/dtos/frais-bancaire/frais-bancaire-validation-request.dto";

@Component({
  selector: 'app-encours-fraisBancaire',
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
  templateUrl: './encours-fraisBancaire.component.html',
  providers: [MessageService]
})
export class EncoursFraisBancaireComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  fraisBancaires: FraisBancaireResponseDto[] = [];
  totalRecords = 0;
  loading = true;
  totalEncours!: Decimal;
  banqueId?: number;

  displayModal: boolean = false;
  selectedFraisBancaire: any = null;
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
    private fraisBancaireService: FraisBancaireService,
    private messageService: MessageService,
    private route: ActivatedRoute,
    private fb: FormBuilder,
    private rapprochementService: RapprochemntService,
  ) {}

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/fraisBancaire', label: 'Frais Bancaire' },
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
          this.loadFraisBancaire({});
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

  loadFraisBancaire(event: TableLazyLoadEvent) {
    this.loading = true;

    // Guard clause to prevent API call if banqueId is not set
    if (!this.banqueId) {
      this.fraisBancaires = [];
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

    const searchCriteria: FraisBancaireSearchDto = {
      libelle: this.libelleFilter || undefined
    };

    this.fraisBancaireService.getOutstandingFraisBancaire(this.banqueId, searchCriteria, page, size, sort)
      .subscribe({
        next: (res: EncoursFraisBancaireDto) => {
          this.fraisBancaires = res.page.content;
          this.totalRecords = res.page.page.totalElements;
          this.totalEncours = res.totalDesEncours;
          this.loading = false;
        },
        error: (err) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de charger les Frais Bancaire en cours.'
          });
          this.loading = false;
          console.error(err);
        }
      });
  }

  // Ouvrir le dialog de validation
  openRapprochementModal(fraisBancaire: any): void {
    this.selectedFraisBancaire = fraisBancaire;
    this.rapprochementForm.reset();
    this.rapprochementForm.patchValue({
      dateEmission: new Date(fraisBancaire.dateEmission),
      observation: fraisBancaire.observation,
      libelle: fraisBancaire.libelle,
    })
    this.displayModal = true;
  }



  // Soumettre la validation
  submitRapprochement(): void {
    if (this.rapprochementForm.invalid || !this.selectedFraisBancaire) {
      this.rapprochementForm.markAllAsTouched();
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Veuillez remplir tous les champs.',
      });
      return;
    }

    const request: FraisBancaireValidationRequest = {
      idFraisBancaire: this.selectedFraisBancaire.id,
      dateOperation: this.rapprochementForm.value.dateOperation,
      dateEmission:this.rapprochementForm.value.dateEmission,
      dateValeur: this.rapprochementForm.value.dateValeur,
      observation: this.rapprochementForm.value.observation,
      libelle:this.rapprochementForm.value.libelle,
      tva:this.rapprochementForm.value.tvaDeductible
    };

    this.isSubmitting = true;
    this.rapprochementService.fraisBancaireToRapprochement(request).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: 'Frai Bancaire validé avec succès.',
        });
        this.isSubmitting = false;
        this.displayModal = false;
        this.table.reset(); // recharge ton tableau existant
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
