import { Component, OnInit, inject } from '@angular/core';
import { CommonModule , Location } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

// PrimeNG
import { MessageService, ConfirmationService, MenuItem, ScrollerOptions } from 'primeng/api';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { RippleModule } from 'primeng/ripple';

// Services & DTOs
import { DecompteService } from '../../service/DecompteService';
import { MarcheService } from '../../../Marches/service/marche-service.service';
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { DecompteRequestDto } from '../../dtos/DecompteRequestDto';
import { toLocalDateString, parseLocalDate } from '../../../global/types/date.utils';

@Component({
  selector: 'app-decompte-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, RouterLink,
    InputTextModule, DropdownModule, InputNumberModule,
    ButtonModule, CalendarModule, BreadcrumbModule,
    ToastModule, ConfirmDialogModule, RippleModule
  ],
  templateUrl: './decomptes-form.component.html',
  providers: [MessageService, ConfirmationService]
})
export class DecompteFormComponent implements OnInit {
  decompteForm!: FormGroup;
  isEditMode = false;
  decompteId: number | null = null;
  loading = false;

  // Logique Marché & Banque
  marchesOptions: any[] = [];
  banquesOptions: any[] = [];

  loadingMarches = false;
  currentMarcheFilter = '';
  rowsPerPageMarches = 10;
  items: MenuItem[] = [];

  marchesScrollerOptions: ScrollerOptions = {
    delay: 250, showLoader: true, lazy: true,
    onLazyLoad: this.onLazyLoadMarches.bind(this)
  };

  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly location = inject(Location);
  private readonly decompteService = inject(DecompteService);
  private readonly marcheService = inject(MarcheService);
  private readonly banqueService = inject(BanqueService);
  private readonly confirmationService = inject(ConfirmationService);

  ngOnInit(): void {
    this.initForm();
    this.loadBanques();
    this.initializeMarches();

    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.decompteId = Number(id);
        this.loadDecompteData(this.decompteId);
      }
    });

    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
      { icon: 'pi pi-list', route: '/portail/decomptes', label: 'Décomptes' }
    ];
  }

  initForm(): void {
    this.decompteForm = this.fb.group({
      marcheId: [null, Validators.required],
      banqueEncaissementId: [null,Validators.required],
      dateEmission: [null, Validators.required],
      libelle: ['', Validators.required],
      montant: [null, [Validators.required, Validators.min(0.01)]],
    });

  }


  loadBanques(): void {
    this.banqueService.getAllBanques({}, 0, 1000, 'nom,asc').subscribe(res => {
      this.banquesOptions = res.content.map(b => ({ label: b.nom, value: b.id }));
    });
  }

  // --- Logique Marchés (Lazy Load) ---
  initializeMarches(filter: string = ''): void {
    this.loadingMarches = true;
    this.marcheService.getAllMarches({ numero: filter }, 0, this.rowsPerPageMarches, 'numero,asc').subscribe(res => {
      const total = res.page.totalElements;
      const data = res.content.map(m => ({ label: m.numero, value: m.id }));
      this.marchesOptions = Array.from({ length: total }).map(() => ({}));
      this.marchesOptions.splice(0, data.length, ...data);
      this.marchesOptions = [...this.marchesOptions];
      this.loadingMarches = false;
    });
  }

  onLazyLoadMarches(event: any): void {
    const { first } = event;
    if (this.marchesOptions[first] && this.marchesOptions[first].value !== undefined) return;
    const page = Math.floor(first / this.rowsPerPageMarches);
    this.marcheService.getAllMarches({ numero: this.currentMarcheFilter }, page, this.rowsPerPageMarches, 'numero,asc').subscribe(res => {
      const data = res.content.map(m => ({ label: m.numero, value: m.id }));
      this.marchesOptions.splice(first, data.length, ...data);
      this.marchesOptions = [...this.marchesOptions];
    });
  }

  onFilterMarches(event: any): void {
    this.currentMarcheFilter = event.filter || '';
    this.initializeMarches(this.currentMarcheFilter);
  }

  loadDecompteData(id: number): void {
    this.decompteService.getDecompteById(id).subscribe(data => {
      this.decompteForm.patchValue({
        marcheId: data.marcheId,
        dateEmission: parseLocalDate(data.dateEmission),
        montant: data.montant,
        libelle: data.libelle,
        banqueEncaissementId: data.banqueEncaissementId
      });
    });
  }

  onSubmit(): void {
    if (this.decompteForm.invalid) return;

    this.confirmationService.confirm({
      header: 'Confirmation',
      message: 'Voulez-vous enregistrer ce décompte ?',
      accept: () => {
        this.loading = true;
        const formRawValue = this.decompteForm.getRawValue();

        const request: DecompteRequestDto = {
          ...formRawValue,
          banqueEncaissementId: formRawValue.banqueEncaissementId,
          dateEmission: toLocalDateString(formRawValue.dateEmission)!
        };

        const action$ = this.isEditMode && this.decompteId
          ? this.decompteService.updateDecompte(this.decompteId, request)
          : this.decompteService.createDecompte(request);

        action$.subscribe({

          next: () => this.location.back(),
          error: () => this.loading = false
        });
      }
    });
  }

  get f() { return this.decompteForm.controls; }

  onCancel(): void {
   this.location.back();
  }
}
