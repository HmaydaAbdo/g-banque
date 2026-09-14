import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { CardModule } from "primeng/card";
import { InputTextModule } from "primeng/inputtext";
import { CommonModule } from "@angular/common";
import { ButtonModule } from "primeng/button";
import { ToastModule } from "primeng/toast";
import { OrganismeLeasingRequest } from "../../dtos/OrganismeLeasingRequest";
import { OrganismeLeasingResponse } from "../../dtos/OrganismeLeasingResponse";
import { OrganismeLeasingService } from "../../services/organisme-leasing.service";
import { finalize } from 'rxjs/operators';
import { ActivatedRoute, Router } from '@angular/router';
import { Ripple } from "primeng/ripple";

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
    ButtonModule,
    ToastModule,
    Ripple
  ],
  providers: [MessageService]
})
export class OrganismeLeasingFormComponent implements OnInit {

  private readonly fb                     = inject(FormBuilder);
  private readonly organismeLeasingService = inject(OrganismeLeasingService);
  private readonly messageService          = inject(MessageService);
  private readonly route                   = inject(ActivatedRoute);
  private readonly router                  = inject(Router);

  form!: FormGroup;
  isEditMode    = false;
  submitting    = false;
  organismeId: number | null = null;

  ngOnInit(): void {
    this.form = this.fb.group({ nom: ['', Validators.required] });

    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.isEditMode  = true;
        this.organismeId = +id;
        this.organismeLeasingService.getOrganismeLeasingById(this.organismeId).subscribe({
          next: (data: OrganismeLeasingResponse) => this.form.patchValue({ nom: data.nom }),
          error: () => this.router.navigate(['/portail/leasing/organismesLeasing'])
        });
      }
    });
  }

  onSubmit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    this.submitting = true;
    const request: OrganismeLeasingRequest = { nom: this.form.value.nom };

    const obs = this.isEditMode && this.organismeId !== null
      ? this.organismeLeasingService.updateOrganismeLeasing(this.organismeId, request)
      : this.organismeLeasingService.createOrganismeLeasing(request);

    obs.pipe(finalize(() => this.submitting = false)).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success', summary: 'Succès',
          detail: this.isEditMode ? 'Organisme mis à jour.' : 'Organisme créé.'
        });
        this.router.navigate(['/portail/leasing/organismesLeasing']);
      }
    });
  }

  onCancel(): void {
    this.router.navigate(['/portail/leasing/organismesLeasing']);
  }

  get f() { return this.form.controls; }
}
