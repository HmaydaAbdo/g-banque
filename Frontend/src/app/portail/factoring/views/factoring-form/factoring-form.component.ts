import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, ParamMap, RouterLink, RouterLinkActive } from '@angular/router';

// PrimeNG
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { ToastModule } from 'primeng/toast';
import { ConfirmationService, MenuItem, MessageService, ScrollerOptions } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { CalendarModule } from 'primeng/calendar';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { TooltipModule } from 'primeng/tooltip';

// Services & DTOs
import { FactoringService } from "../../service/FactoringService";
import { MarcheService } from '../../../Marches/service/marche-service.service';
import { Type } from "../../../prefinancements/enums/Type";
import { FactoringRequestDto } from '../../dtos/FactoringRequestDto';
import { FactoringResponseDto } from '../../dtos/FactoringResponseDto';
import { MarcheResponseDTO } from '../../../Marches/dtos/marche-response.dto';
import { toLocalDateString, parseLocalDate } from '../../../global/types/date.utils';

@Component({
  selector: 'app-factoring-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, InputTextModule, DropdownModule,
    InputNumberModule, ButtonModule, RippleModule, ToastModule, ConfirmDialogModule,
    CalendarModule, BreadcrumbModule, RouterLink, RouterLinkActive, TooltipModule,
  ],
  templateUrl: './factoring-form.component.html',
  providers: [MessageService, ConfirmationService],
})
export class FactoringFormComponent implements OnInit {
  factoringForm!: FormGroup;
  isEditMode: boolean = false;
  factoringId: number | null = null;
  loading: boolean = false;

  // Configuration Marchés (Lazy + Virtual Scroll)
  marchesOptions: any[] = [];
  loadingMarches: boolean = false;
  currentFilter: string = '';
  rowsPerPage: number = 10;

  marchesScrollerOptions: ScrollerOptions = {
    delay: 250,
    showLoader: true,
    lazy: true,
    onLazyLoad: this.onLazyLoadMarches.bind(this)
  };

  typeOptions: any[] = [];
  items: MenuItem[] | undefined;

  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly location = inject(Location);
  private readonly factoringService = inject(FactoringService);
  private readonly marcheService = inject(MarcheService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);

  ngOnInit(): void {
    this.setupBreadcrumbs();
    this.initForm();
    this.loadTypeOptions();
    this.initializeMarches();

    this.route.paramMap.subscribe((params: ParamMap) => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.factoringId = Number(id);
        this.loadFactoring(this.factoringId);
      }
    });
  }

  initForm(): void {
    this.factoringForm = this.fb.group({
      marcheId: [null, Validators.required],
      dateEmission: [null, Validators.required],
      montant: [null, [Validators.required, Validators.min(0.01)]],
      type: [null, Validators.required],
    });
  }

  //  Initialisation de la liste avec des objets vides pour éviter les erreurs de rendu
  initializeMarches(): void {
    this.marcheService.getAllMarchesForLazyLoad(0, this.rowsPerPage, 'numero,asc').subscribe(res => {
      const total = res.page.totalElements;
      const data = res.content.map(m => ({ label: m.numero, value: m.id }));
      this.marchesOptions = Array.from({ length: total }).map(() => ({}));
      this.marchesOptions.splice(0, data.length, ...data);
    });
  }

  //  Chargement à la demande lors du scroll
  onLazyLoadMarches(event: any): void {
    const { first } = event;
    const page = Math.floor(first / this.rowsPerPage);

    if (this.marchesOptions[first] && this.marchesOptions[first].value !== undefined) {
      return;
    }

    this.loadingMarches = true;
    this.marcheService.getAllMarches({ numero: this.currentFilter }, page, this.rowsPerPage, 'numero,asc').subscribe({
      next: (res) => {
        const newItems = res.content.map(m => ({ label: m.numero, value: m.id }));
        const updatedOptions = [...this.marchesOptions];
        updatedOptions.splice(first, newItems.length, ...newItems);
        this.marchesOptions = updatedOptions;
        this.loadingMarches = false;
      },
      error: () => this.loadingMarches = false
    });
  }

  // ✅ Filtrage serveur
  onFilterMarches(event: any): void {
    const query = event.filter?.trim() || '';
    this.currentFilter = query;

    this.marcheService.getAllMarches({ numero: query }, 0, this.rowsPerPage, 'numero,asc').subscribe(res => {
      const total = res.page.totalElements;
      const data = res.content.map(m => ({ label: m.numero, value: m.id }));
      this.marchesOptions = Array.from({ length: total }).map(() => ({}));
      this.marchesOptions.splice(0, data.length, ...data);
    });
  }

  loadTypeOptions(): void {
    this.typeOptions = Object.keys(Type).map((key) => ({
      label: key,
      value: Type[key as keyof typeof Type],
    }));
  }

  loadFactoring(id: number): void {
    this.loading = true;
    this.factoringService.getFactoringById(id).subscribe({
      next: (factoring: FactoringResponseDto) => {
        this.marcheService.getMarcheById(factoring.marcheId).subscribe(marche => {
          const selected = { label: marche.numero, value: marche.id };
          // Injection sécurisée du marché sélectionné
          if (!this.marchesOptions.find(m => m?.value === marche.id)) {
            this.marchesOptions[0] = selected;
          }
          this.factoringForm.patchValue({
            marcheId: factoring.marcheId,
            dateEmission: parseLocalDate(factoring.dateEmission),
            montant: factoring.montant,
            type: factoring.type,
          });
        });
        this.loading = false;
      },
      error: () => {
        this.location.back();
      }
    });
  }

  setupBreadcrumbs(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/factoring', label: 'Factoring' },
    ];
  }

  get f() { return this.factoringForm.controls; }

  onSubmit(): void {
    if (this.factoringForm.invalid) {
      this.factoringForm.markAllAsTouched();
      return;
    }
    this.confirmationService.confirm({
      message: this.isEditMode ? 'Modifier ce factoring ?' : 'Créer ce factoring ?',
      header: 'Confirmation',
      accept: () => this.executeSave()
    });
  }

  executeSave(): void {
    this.loading = true;
    const formValue = this.factoringForm.value;
    const request: FactoringRequestDto = {
      ...formValue,
      dateEmission: toLocalDateString(formValue.dateEmission)!
    };

    const op$ = (this.isEditMode && this.factoringId)
      ? this.factoringService.updateFactoring(this.factoringId, request)
      : this.factoringService.createFactoring(request);

    op$.subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Opération réussie.' });
        this.location.back();
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  onCancel(): void {
    this.location.back();
  }
}
