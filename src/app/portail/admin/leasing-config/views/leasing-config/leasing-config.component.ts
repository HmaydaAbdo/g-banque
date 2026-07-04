import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { MessageService, MenuItem } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { finalize } from 'rxjs/operators';

import { LeasingConfigService } from '../../services/leasing-config.service';
import { LeasingConfigResponse } from '../../dtos/LeasingConfigResponse';

/**
 * Administration de la configuration leasing (valeur résiduelle, taux de TVA).
 * Remplace les anciennes valeurs codées en dur dans application.yml
 * (leasing.contract.valeur-residuelle / taux-tva).
 * Les deux taux sont saisis en pourcentage et convertis en fraction pour l'API.
 */
@Component({
  selector: 'app-leasing-config',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    CardModule,
    ButtonModule,
    InputNumberModule,
    BreadcrumbModule,
    ToastModule
  ],
  providers: [MessageService],
  templateUrl: './leasing-config.component.html'
})
export class LeasingConfigComponent implements OnInit {

  private readonly leasingConfigService = inject(LeasingConfigService);
  private readonly messageService = inject(MessageService);
  private readonly fb = inject(FormBuilder);

  items: MenuItem[] = [
    { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
    { label: 'Administration' },
    { label: 'Configuration leasing' }
  ];
  home: MenuItem = { icon: 'pi pi-home', route: '/portail/accueil' };

  form!: FormGroup;
  loading = false;
  submitting = false;
  dateModification: string | null = null;

  ngOnInit(): void {
    this.form = this.fb.group({
      valeurResiduelle: [null, [Validators.required, Validators.min(0), Validators.max(100)]],
      tauxTva: [null, [Validators.required, Validators.min(0), Validators.max(100)]],
      alerteEcheanceJoursCourtTerme: [null, [Validators.required, Validators.min(1)]],
      alerteEcheanceJoursLongTerme: [null, [Validators.required, Validators.min(1)]]
    });

    this.loading = true;
    this.leasingConfigService.getConfig()
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: (config: LeasingConfigResponse) => {
          this.dateModification = config.dateModification;
          this.form.setValue({
            valeurResiduelle: config.valeurResiduelle * 100,
            tauxTva: config.tauxTva * 100,
            alerteEcheanceJoursCourtTerme: config.alerteEcheanceJoursCourtTerme,
            alerteEcheanceJoursLongTerme: config.alerteEcheanceJoursLongTerme
          });
        }
      });
  }

  onSubmit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    this.submitting = true;
    const payload = {
      valeurResiduelle: this.form.value.valeurResiduelle / 100,
      tauxTva: this.form.value.tauxTva / 100,
      alerteEcheanceJoursCourtTerme: this.form.value.alerteEcheanceJoursCourtTerme,
      alerteEcheanceJoursLongTerme: this.form.value.alerteEcheanceJoursLongTerme
    };

    this.leasingConfigService.update(payload)
      .pipe(finalize(() => this.submitting = false))
      .subscribe({
        next: (config: LeasingConfigResponse) => {
          this.dateModification = config.dateModification;
          this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Configuration leasing mise à jour.' });
        }
      });
  }

  get f() { return this.form.controls; }
}
