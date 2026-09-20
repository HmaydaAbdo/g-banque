import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ToastModule } from 'primeng/toast';
import { RippleModule } from 'primeng/ripple';
import { finalize } from 'rxjs/operators';

import { EcheanceLeasingService } from '../../services/EcheanceLeasingService';
import { EcheanceLeasingResponse } from '../../dtos/EcheanceLeasingResponse';
import { EcheanceLeasingUpdateRequest } from '../../dtos/EcheanceLeasingUpdateRequest';
import { StatutEcheanceLeasing } from '../../dtos/StatutEcheanceLeasing';

/**
 * Page d'édition d'une échéance de leasing — remplace le dialogue historique de
 * `TableauAmortissementComponent` (21/09/2026, demande HMAYDA) pour pouvoir être atteinte aussi
 * bien depuis le tableau d'amortissement que depuis le récapitulatif unifié. La route est protégée
 * par `systemAdminGuard` (édition réservée aux administrateurs système, comme le dialogue
 * d'origine) ; le blocage « échéance déjà payée » est réappliqué ici côté UI en plus du blocage
 * déjà fait par le backend (`PUT /echeances-leasing/{id}` renvoie 400 si le statut n'est pas
 * PREVU), pour ne pas présenter un formulaire soumissible qui échouera de toute façon.
 */
@Component({
  selector: 'app-echeance-leasing-edit',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ButtonModule,
    CalendarModule,
    InputNumberModule,
    InputTextModule,
    InputTextareaModule,
    ProgressSpinnerModule,
    ToastModule,
    RippleModule,
  ],
  providers: [MessageService],
  templateUrl: './echeance-leasing-edit.component.html',
  styleUrl: './echeance-leasing-edit.component.scss',
})
export class EcheanceLeasingEditComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly location = inject(Location);
  private readonly fb = inject(FormBuilder);
  private readonly echeanceLeasingService = inject(EcheanceLeasingService);
  private readonly messageService = inject(MessageService);

  readonly StatutEcheanceLeasing = StatutEcheanceLeasing;

  echeanceId!: number;
  echeance: EcheanceLeasingResponse | null = null;
  loading = true;
  submitting = false;
  loadError: string | null = null;
  editForm!: FormGroup;

  ngOnInit(): void {
    this.editForm = this.fb.group({
      dateEcheance: [null, Validators.required],
      montantTTC: [null, [Validators.required, Validators.min(0.01)]],
      dateOperation: [null],
      dateValeur: [null],
      observation: [null],
      libelle: [null],
    });

    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.loadError = "Identifiant d'échéance manquant.";
      this.loading = false;
      return;
    }
    this.echeanceId = +id;
    this.loadEcheance();
  }

  private loadEcheance(): void {
    this.loading = true;
    this.loadError = null;
    this.echeanceLeasingService.getEcheanceLeasingById(this.echeanceId).pipe(
      finalize(() => { this.loading = false; })
    ).subscribe({
      next: (echeance) => {
        this.echeance = echeance;
        if (echeance.statutPaiement === StatutEcheanceLeasing.PREVU) {
          this.editForm.reset({
            dateEcheance: echeance.dateEcheance ? new Date(echeance.dateEcheance) : null,
            montantTTC: echeance.montantTTC,
            dateOperation: echeance.dateOperation ? new Date(echeance.dateOperation) : null,
            dateValeur: echeance.dateValeur ? new Date(echeance.dateValeur) : null,
            observation: echeance.observation ?? null,
            libelle: echeance.libelle ?? null,
          });
        }
      },
      error: () => {
        this.loadError = 'Impossible de récupérer cette échéance.';
      },
    });
  }

  get isEditable(): boolean {
    return this.echeance?.statutPaiement === StatutEcheanceLeasing.PREVU;
  }

  save(): void {
    if (!this.isEditable || this.editForm.invalid) return;

    const toIso = (d: Date | null | undefined): string | null =>
      d ? `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}` : null;

    const raw = this.editForm.value;
    const dto: EcheanceLeasingUpdateRequest = {
      dateEcheance: toIso(raw.dateEcheance)!,
      montantTTC: raw.montantTTC,
      dateOperation: toIso(raw.dateOperation),
      dateValeur: toIso(raw.dateValeur),
      observation: raw.observation ?? null,
      libelle: raw.libelle ?? null,
    };

    this.submitting = true;
    this.echeanceLeasingService.updateEcheance(this.echeanceId, dto).pipe(
      finalize(() => { this.submitting = false; })
    ).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Échéance mise à jour avec succès.' });
        this.location.back();
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: "Échec de la mise à jour de l'échéance." });
      },
    });
  }

  cancel(): void {
    this.location.back();
  }
}
