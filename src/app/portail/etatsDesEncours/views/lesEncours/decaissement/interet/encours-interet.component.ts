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
import {ConfirmationService, MenuItem, MessageService, SelectItem} from "primeng/api";
import {Decimal} from "decimal.js";
import {ActivatedRoute} from "@angular/router";
import {RapprochemntService} from "../../../../rapprochement/services/rapprochement/rapprochemnt.service";
import {InteretResponseDto} from "../../../../../interest/dtos/InteretResponseDto";
import {InteretService} from "../../../../../interest/service/InteretService";
import {InteretSearchDto} from "../../../../../interest/dtos/InteretSearchDto";
import {EncoursInteretDto} from "../../../../../interest/dtos/EncoursInteretDto";
import {InteretValidationRequest} from "../../../../rapprochement/dtos/interets/interet-validation-request.dto";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {debounceTime, distinctUntilChanged, Subscription} from "rxjs";

@Component({
  selector: 'app-encours-interest',
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
    InputTextareaModule,
    ConfirmDialogModule
  ],
  templateUrl: './encours-interet.component.html',
  providers: [MessageService,ConfirmationService,DecimalPipe]
})
export class EncoursInteretComponent implements OnInit {
  @ViewChild('dt') table!: Table;

  interests: InteretResponseDto[] = [];
  totalRecords = 0;
  loading = true;
  totalEncours!: number;
  banqueId?: number;

  displayModal: boolean = false;
  selectedInterest: any = null;
  rapprochementForm!: FormGroup;

  searchForm!:FormGroup

  items: MenuItem[] = [];
  tvaOptions: SelectItem[] = [
    { label: '0%', value: 0 },
    { label: '10%', value: 10 },
    { label: '14%', value: 14 },
    { label: '20%', value: 20 }
  ];

  isSubmitting: boolean = false;

  searchFormSubscription!: Subscription ;

  constructor(
    private interestService: InteretService,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private datePipe: DatePipe,
    private decimalPipe:DecimalPipe,
    private route: ActivatedRoute,
    private fb: FormBuilder,
    private rapprochementService: RapprochemntService,
  ) {}

  ngOnInit(): void {

    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/interests', label: 'Intérêts' },
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
          this.loadInterests({});
        }
      } else {
        this.messageService.add({
          severity: 'warn',
          summary: 'Attention',
          detail: 'No bank ID found in the URL.'
        });
      }
    });

    this.searchForm = this.fb.group({
      libelle: [''],
      montant: [null],
    })

    this.searchFormSubscription = this.searchForm.valueChanges.pipe(
      // Attends 300ms après la dernière frappe/changement
      debounceTime(300),
      // N'émet que si la valeur actuelle est différente de la dernière valeur émise
      distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
    ).subscribe(() => {
      // Recharge le tableau via applyFilters (qui appelle table.reset())
      this.applyFilters();
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

  loadInterests(event: TableLazyLoadEvent) {
    this.loading = true;

    // Guard clause to prevent API call if banqueId is not set
    if (!this.banqueId) {
      this.interests = [];
      this.totalRecords = 0;
      this.totalEncours = 0;
      this.loading = false;
      return;
    }

    const page = event.first ? event.first / (event.rows ?? 10) : 0;
    const size = event.rows ?? 10;
    const sortField = event.sortField ?? 'dateEmission';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    const searchCriteria: InteretSearchDto =this.searchForm.getRawValue()

    this.interestService.getOutstandingInterest(this.banqueId, searchCriteria, page, size, sort)
      .subscribe({
        next: (res: EncoursInteretDto) => {
          this.interests = res.page.content;
          this.totalRecords = res.page.page.totalElements;
          this.totalEncours = res.totalDesEncours;
          this.loading = false;
        },
        error: (err) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: 'Impossible de charger les Intérêts en cours.'
          });
          this.loading = false;
          console.error(err);
        }
      });
  }

  // Ouvrir le dialog de validation
  openRapprochementModal(interest: any): void {
    this.selectedInterest = interest;
    this.rapprochementForm.reset();
    this.rapprochementForm.patchValue({
      dateEmission: new Date(interest.dateEmission),
      observation: interest.observation,
      libelle: interest.libelle,
    })
    this.displayModal = true;
  }


  confirmRapprochement(): void {
    if (this.rapprochementForm.invalid) {
      this.rapprochementForm.markAllAsTouched();
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Veuillez remplir tous les champs obligatoires et corriger les erreurs.'
      });
      return;
    }

    if (this.rapprochementForm.valid && this.selectedInterest) {
      const formValue = this.rapprochementForm.getRawValue();
      const selectedInterest = this.selectedInterest;

      // Formatage des dates pour l'affichage
      const dateEmissionFormatted = this.datePipe.transform(formValue.dateEmission, 'dd/MM/yyyy');
      const dateValeurFormatted = this.datePipe.transform(formValue.dateValeur, 'dd/MM/yyyy');
      const dateOperationFormatted = this.datePipe.transform(formValue.dateOperation, 'dd/MM/yyyy');



      // Création du message de récapitulatif en HTML
      const recapMessage = `
            Êtes-vous sûr de vouloir rapprocher ces Intérêts ?
            <br><br>
            <strong>ID Intérêt:</strong> ${selectedInterest.id}
            <br>
            <strong>Montant:</strong> ${this.decimalPipe.transform(selectedInterest.montant,'1.2-2',"fr-FR")} MAD
            <br><br>
            <strong>Détails du Rapprochement:</strong>
            <ul>
                <li><strong>Date d'émission:</strong> ${dateEmissionFormatted}</li>
                <li><strong>Date de valeur:</strong> ${dateValeurFormatted || '-'}</li>
                <li><strong>Date d'opération:</strong> ${dateOperationFormatted || '-'}</li>
                <li><strong>Libellé:</strong> ${formValue.libelle}</li>
                <li><strong>Observation:</strong> ${formValue.observation}</li>
                <li><strong>TVA Déductible:</strong> ${formValue.tvaDeductible}%</li>
            </ul>
      `;

      this.confirmationService.confirm({
        message: recapMessage,
        header: 'Confirmation de Rapprochement Intérêts',
        icon: 'pi pi-exclamation-triangle',
        acceptLabel: 'Confirmer',
        rejectLabel: 'Annuler',
        accept: () => {
          this.submitRapprochement();
        },
        reject: () => {
          // L'utilisateur a annulé, aucune action
        }
      });
    }
  }

  // Soumettre la validation
  submitRapprochement(): void {

    if (this.rapprochementForm.valid && this.selectedInterest) {



    const request: InteretValidationRequest = {
      idInteret: this.selectedInterest.id,
      dateOperation: this.rapprochementForm.value.dateOperation,
      dateEmission:this.rapprochementForm.value.dateEmission,
      dateValeur: this.rapprochementForm.value.dateValeur,
      observation: this.rapprochementForm.value.observation,
      libelle:this.rapprochementForm.value.libelle,
      tva:this.rapprochementForm.value.tvaDeductible
    };

    this.isSubmitting = true;

    this.rapprochementService.interetToRapprochement(request).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: 'Intérêts validée avec succès.',
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
  }}



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
