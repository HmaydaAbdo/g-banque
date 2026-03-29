import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidatorFn } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { CardModule } from "primeng/card";
import { InputTextModule } from "primeng/inputtext";
import { InputNumberModule } from "primeng/inputnumber";
import { CommonModule } from "@angular/common";
import { CalendarModule } from "primeng/calendar";
import { ButtonModule } from "primeng/button";
import { ToastModule } from "primeng/toast";
import { OrganismeLeasingRequest } from "../../dtos/OrganismeLeasingRequest";
import { OrganismeLeasingResponse } from "../../dtos/OrganismeLeasingResponse";
import { OrganismeLeasingService } from "../../services/organisme-leasing.service";
import { finalize } from 'rxjs/operators';
import { ActivatedRoute, Router } from '@angular/router';
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {Ripple} from "primeng/ripple";
import {StyleClassModule} from "primeng/styleclass"; // Import ActivatedRoute and Router
import { toLocalDateString, parseLocalDate } from '../../../../global/types/date.utils';

@Component({
  selector: 'app-organisme-leasing-form',
  templateUrl: './organisme-leasing-form.component.html',
  styleUrls: ['./organisme-leasing-form.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    CardModule,
    InputTextModule,
    InputNumberModule,
    CalendarModule,
    ButtonModule,
    ToastModule,
    ConfirmDialogModule,
    Ripple,
    StyleClassModule
  ],
  providers: [MessageService]
})
export class OrganismeLeasingFormComponent implements OnInit {

  organismeLeasingForm!: FormGroup;
  isEditMode: boolean = false;
  submittingForm: boolean = false;
  organismeId: number | null = null;

  constructor(
    private fb: FormBuilder,
    private organismeLeasingService: OrganismeLeasingService,
    private messageService: MessageService,
    private route: ActivatedRoute, // Inject ActivatedRoute
    private router: Router // Inject Router
  ) { }

  ngOnInit(): void {
    this.initForm();

    // Check for ID in route parameters for edit mode
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.organismeId = +id;
        this.loadOrganismeLeasingData(this.organismeId);
      }
    });
  }

  initForm(): void {
    this.organismeLeasingForm = this.fb.group({
      nom: ['', Validators.required],
      plafond: [null, [Validators.required, Validators.min(0)]],
      dateDebut: [null, Validators.required],
      dateFin: [null, Validators.required],
    }, {
      validators: this.dateFinAfterDateDebutValidator()
    });
  }

  dateFinAfterDateDebutValidator(): ValidatorFn {
    return (formGroup: AbstractControl): { [key: string]: any } | null => {
      const dateDebutControl = formGroup.get('dateDebut');
      const dateFinControl = formGroup.get('dateFin');

      if (!dateDebutControl || !dateFinControl || !dateDebutControl.value || !dateFinControl.value) {
        return null;
      }

      const dateDebut = dateDebutControl.value as Date;
      const dateFin = dateFinControl.value as Date;

      if (dateFin < dateDebut) {
        dateFinControl.setErrors({ ...dateFinControl.errors, 'dateFinBeforeDateDebut': true });
        return { 'dateMismatch': true };
      } else {
        if (dateFinControl.hasError('dateFinBeforeDateDebut')) {
          const errors = { ...dateFinControl.errors };
          delete errors['dateFinBeforeDateDebut'];
          dateFinControl.setErrors(Object.keys(errors).length > 0 ? errors : null);
        }
      }
      return null;
    };
  }

  loadOrganismeLeasingData(id: number): void {
    this.organismeLeasingService.getOrganismeLeasingById(id).subscribe({
      next: (data: OrganismeLeasingResponse) => {
        this.populateForm(data);
      },
      error: (err: Error) => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement des données de l\'organisme de leasing.' });
        console.error('Error loading organisme leasing:', err);
        this.router.navigate(['/organismes-leasing']);
      }
    });
  }

  populateForm(data: OrganismeLeasingResponse): void {
    this.organismeLeasingForm.patchValue({
      nom: data.nom,
      plafond: data.plafond,
      dateDebut: data.dateDebut ? parseLocalDate(data.dateDebut) : null,
      dateFin: data.dateFin ? parseLocalDate(data.dateFin) : null,
    });
    this.organismeLeasingForm.updateValueAndValidity();
  }

  onSubmit(): void {
    this.organismeLeasingForm.markAllAsTouched();
    this.organismeLeasingForm.updateValueAndValidity();

    if (this.organismeLeasingForm.invalid) {
      this.messageService.add({ severity: 'warn', summary: 'Attention', detail: 'Veuillez remplir tous les champs requis et corriger les erreurs.' });
      return;
    }

    this.submittingForm = true;

    const formValue = this.organismeLeasingForm.value;
    const request: OrganismeLeasingRequest = {
      nom: formValue.nom,
      plafond: formValue.plafond,
      dateDebut: formValue.dateDebut ? toLocalDateString(formValue.dateDebut)! : '',
      dateFin: formValue.dateFin ? toLocalDateString(formValue.dateFin)! : '',
    };

    if (this.isEditMode && this.organismeId !== null) {
      this.organismeLeasingService.updateOrganismeLeasing(this.organismeId, request)
        .pipe(finalize(() => this.submittingForm = false))
        .subscribe({
          next: (response) => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Organisme de leasing mis à jour avec succès.' });
            this.router.navigate(['/portail/leasing/organismesLeasing']); // Navigate back to the list
          },
          error: (err: Error) => {
            this.handleServiceError(err);
          }
        });
    } else {
      this.organismeLeasingService.createOrganismeLeasing(request)
        .pipe(finalize(() => this.submittingForm = false))
        .subscribe({
          next: (response) => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Organisme de leasing créé avec succès.' });
            this.organismeLeasingForm.reset();
            this.isEditMode = false;
            this.router.navigate(['/portail/leasing/organismesLeasing']);
          },
          error: (err: Error) => {
            this.handleServiceError(err);
          }
        });
    }
  }

  onCancel(): void {
    this.router.navigate(['/organismes-leasing']);
  }

  private handleServiceError(err: Error): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur',
      detail: err.message || 'Une erreur est survenue lors de l\'opération.'
    });
    console.error('Frontend caught service error:', err);
  }

  get f() { return this.organismeLeasingForm.controls; }
}
