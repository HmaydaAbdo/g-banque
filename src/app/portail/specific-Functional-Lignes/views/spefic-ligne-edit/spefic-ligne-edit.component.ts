import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
import { ActivatedRoute, ParamMap, Router, RouterLink, RouterLinkActive } from '@angular/router';

// PrimeNG Imports
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { ToastModule } from 'primeng/toast';
import { ConfirmationService, MenuItem, MessageService, ScrollerOptions } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { CalendarModule } from 'primeng/calendar';
import { BreadcrumbModule } from "primeng/breadcrumb";
import { TooltipModule } from "primeng/tooltip";

// Services & DTOs
import { SpecificFunctionalLigneService } from "../../service/SpecificFunctionalLigneService";
import { MarcheService } from "../../../Marches/service/marche-service.service";
import { NatureLigneFonctionnelleService } from "../../../lignes-fonctionnelles/services/nature-ligne-fonctionnelle.service";
import { PorteeNature } from "../../../lignes-fonctionnelles/dtos/nature-ligne-fonctionnelle/PorteeNature";
import { parseLocalDate, toLocalDateString } from "../../../global/types/date.utils";

@Component({
  selector: 'app-spefic-ligne-edit',
  templateUrl: './spefic-ligne-edit.component.html',
  styleUrls: ['./spefic-ligne-edit.component.scss'],
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, InputTextModule, DropdownModule,
    InputNumberModule, ButtonModule, RippleModule, ToastModule, ConfirmDialogModule,
    CalendarModule, BreadcrumbModule, RouterLink, RouterLinkActive, TooltipModule
  ],
  providers: [MessageService, ConfirmationService]
})
export class SpeficLigneEditComponent implements OnInit {

  ligneFonctionnelleSpecifiqueForm!: FormGroup;
  isEditMode: boolean = false;
  ligneFonctionnelleSpecifiqueId: number | null = null;
  natureLigneOptions: any[] = [];
  items: MenuItem[] | undefined;

  // ✅ Configuration Dropdown Lazy (Alignée sur la Doc PrimeNG)
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

  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly specificLigneService = inject(SpecificFunctionalLigneService);
  private readonly marcheService = inject(MarcheService);
  private readonly natureLigneFonctionnelleService = inject(NatureLigneFonctionnelleService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: "Accueil" },
      { icon: 'pi pi-list', route: '/portail/lignes-fonctionnelles-specifiques-encours', label: "Lignes Spécifiques" }
    ];
    this.initForm();
    this.loadNatureLigneOptions();
    this.initializeMarches();

    this.route.paramMap.subscribe((params: ParamMap) => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.ligneFonctionnelleSpecifiqueId = Number(id);
        this.loadEditData(this.ligneFonctionnelleSpecifiqueId);
      }
    });
  }

  initForm(): void {
    this.ligneFonctionnelleSpecifiqueForm = this.fb.group({
      natureLigneFonctionnelleId: [null, Validators.required],
      marcheId: [null, Validators.required],
      plafond: [null, [Validators.required, Validators.min(0.01)]],
      taux: [null, [Validators.required, Validators.min(0), Validators.max(100)]],
      dateDepart: [null, Validators.required],
      dateFin: [null, Validators.required],
    }, { validators: this.dateFinAfterDateDepartValidator() });
  }

  // ✅ INITIALISATION : Évite l'erreur 'optionGroup' en créant des objets {}
  initializeMarches(): void {
    this.marcheService.getAllMarchesForLazyLoad(0, this.rowsPerPage, 'numero,asc').subscribe(res => {
      const total = res.page.totalElements;
      const data = res.content.map(m => ({ label: m.numero, value: m.id }));

      // IMPORTANT : .map(() => ({})) empêche PrimeNG de lire des propriétés sur undefined
      this.marchesOptions = Array.from({ length: total }).map(() => ({}));

      // On remplace les premiers objets vides par les vraies données
      this.marchesOptions.splice(0, data.length, ...data);
    });
  }

  // ✅ LAZY LOAD : Déclenché au scroll
  onLazyLoadMarches(event: any): void {
    const { first, last } = event;
    const page = Math.floor(first / this.rowsPerPage);

    // Si l'objet à cet index a déjà une propriété 'value', on ne recharge pas
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

  // ✅ FILTER : Déclenché à la saisie dans le champ recherche
  onFilterMarches(event: any): void {
    const query = event.filter?.trim() || '';
    this.currentFilter = query;

    this.marcheService.getAllMarches({ numero: query }, 0, this.rowsPerPage, 'numero,asc').subscribe(res => {
      const total = res.page.totalElements;
      const data = res.content.map(m => ({ label: m.numero, value: m.id }));

      // Réinitialisation avec des objets vides pour la nouvelle taille filtrée
      this.marchesOptions = Array.from({ length: total }).map(() => ({}));
      this.marchesOptions.splice(0, data.length, ...data);
    });
  }

  // ✅ MODE ÉDITION : Assure que le marché sélectionné est dans la liste
  loadEditData(id: number): void {
    this.specificLigneService.getLigneFonctionnelleSpecifiqueById(id).subscribe(ligne => {
      this.marcheService.getMarcheById(ligne.marcheId).subscribe(marche => {
        const selected = { label: marche.numero, value: marche.id };

        // Si le marché n'est pas dans le premier bloc chargé, on l'injecte au début
        if (!this.marchesOptions.find(m => m?.value === marche.id)) {
          this.marchesOptions[0] = selected;
        }

        this.ligneFonctionnelleSpecifiqueForm.patchValue({
          natureLigneFonctionnelleId: ligne.natureLigneFonctionnelleId,
          marcheId: ligne.marcheId,
          plafond: ligne.plafond,
          taux: ligne.taux,
          dateDepart: parseLocalDate(ligne.dateDepart),
          dateFin: parseLocalDate(ligne.dateFin),
        });
      });
    });
  }

  loadNatureLigneOptions(): void {
    this.natureLigneFonctionnelleService.getAllNatureLigneFonctionnellesList(PorteeNature.SPECIFIQUE).subscribe(natures => {
      this.natureLigneOptions = natures.map(n => ({ label: n.label, value: n.id }));
    });
  }

  dateFinAfterDateDepartValidator(): ValidatorFn {
    return (group: AbstractControl) => {
      const start = group.get('dateDepart')?.value;
      const end = group.get('dateFin')?.value;
      if (start && end && new Date(end) < new Date(start)) {
        group.get('dateFin')?.setErrors({ minDate: true });
        return { dateMismatch: true };
      }
      return null;
    };
  }

  get f() { return this.ligneFonctionnelleSpecifiqueForm.controls; }

  onSubmit(): void {
    if (this.ligneFonctionnelleSpecifiqueForm.invalid) {
      this.ligneFonctionnelleSpecifiqueForm.markAllAsTouched();
      return;
    }
    this.confirmationService.confirm({
      header: 'Confirmation',
      message: this.isEditMode ? 'Modifier cette ligne ?' : 'Créer cette ligne ?',
      accept: () => this.executeSave()
    });
  }

  executeSave(): void {
    const val = this.ligneFonctionnelleSpecifiqueForm.value;
    const request = {
      ...val,
      dateDepart: toLocalDateString(val.dateDepart)!,
      dateFin: toLocalDateString(val.dateFin)!
    };
    const req$ = this.isEditMode && this.ligneFonctionnelleSpecifiqueId
      ? this.specificLigneService.updateLigneFonctionnelleSpecifique(this.ligneFonctionnelleSpecifiqueId, request)
      : this.specificLigneService.createLigneFonctionnelleSpecifique(request);

    req$.subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Enregistrement réussi' });
        this.router.navigate(['/portail/lignes-fonctionnelles-specifiques-encours']);
      },
      error: () => {}
    });
  }

  onCancel(): void {
    this.router.navigate(['/portail/lignes-fonctionnelles-specifiques-encours']);
  }
}
