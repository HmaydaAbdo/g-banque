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

import { LigneFonctionnelleConfigService } from '../../services/ligne-fonctionnelle-config.service';
import { LigneFonctionnelleConfigResponse } from '../../dtos/LigneFonctionnelleConfigResponse';

/**
 * Administration de la configuration LigneFonctionnelle (seuils d'alerte/avertissement
 * d'expiration, utilisés par le batch général ET le batch spécifique).
 * Remplace les anciennes propriétés @Value (non définies dans application.yml).
 */
@Component({
  selector: 'app-ligne-fonctionnelle-config',
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
  templateUrl: './ligne-fonctionnelle-config.component.html'
})
export class LigneFonctionnelleConfigComponent implements OnInit {

  private readonly configService = inject(LigneFonctionnelleConfigService);
  private readonly messageService = inject(MessageService);
  private readonly fb = inject(FormBuilder);

  items: MenuItem[] = [
    { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
    { label: 'Administration' },
    { label: 'Configuration Lignes Fonctionnelles' }
  ];
  home: MenuItem = { icon: 'pi pi-home', route: '/portail/accueil' };

  form!: FormGroup;
  loading = false;
  submitting = false;
  dateModification: string | null = null;

  ngOnInit(): void {
    this.form = this.fb.group({
      alertThresholdMonths: [null, [Validators.required, Validators.min(1)]],
      warningThresholdMonths: [null, [Validators.required, Validators.min(1)]]
    });

    this.loading = true;
    this.configService.getConfig()
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: (config: LigneFonctionnelleConfigResponse) => {
          this.dateModification = config.dateModification;
          this.form.setValue({
            alertThresholdMonths: config.alertThresholdMonths,
            warningThresholdMonths: config.warningThresholdMonths
          });
        }
      });
  }

  onSubmit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    this.submitting = true;
    this.configService.update(this.form.value)
      .pipe(finalize(() => this.submitting = false))
      .subscribe({
        next: (config: LigneFonctionnelleConfigResponse) => {
          this.dateModification = config.dateModification;
          this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Configuration Lignes Fonctionnelles mise à jour.' });
        }
      });
  }

  get f() { return this.form.controls; }
}
