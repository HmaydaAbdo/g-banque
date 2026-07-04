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

import { CmtConfigService } from '../../services/cmt-config.service';
import { CmtConfigResponse } from '../../dtos/CmtConfigResponse';

/**
 * Administration de la configuration CMT (seuils d'alerte des échéances à venir).
 * Remplace les anciennes valeurs codées en dur dans CmtMetrics (plusDays(7)/plusDays(30)).
 */
@Component({
  selector: 'app-cmt-config',
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
  templateUrl: './cmt-config.component.html'
})
export class CmtConfigComponent implements OnInit {

  private readonly cmtConfigService = inject(CmtConfigService);
  private readonly messageService = inject(MessageService);
  private readonly fb = inject(FormBuilder);

  items: MenuItem[] = [
    { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
    { label: 'Administration' },
    { label: 'Configuration CMT' }
  ];
  home: MenuItem = { icon: 'pi pi-home', route: '/portail/accueil' };

  form!: FormGroup;
  loading = false;
  submitting = false;
  dateModification: string | null = null;

  ngOnInit(): void {
    this.form = this.fb.group({
      alerteEcheanceJoursCourtTerme: [null, [Validators.required, Validators.min(1)]],
      alerteEcheanceJoursLongTerme: [null, [Validators.required, Validators.min(1)]]
    });

    this.loading = true;
    this.cmtConfigService.getConfig()
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: (config: CmtConfigResponse) => {
          this.dateModification = config.dateModification;
          this.form.setValue({
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
    this.cmtConfigService.update(this.form.value)
      .pipe(finalize(() => this.submitting = false))
      .subscribe({
        next: (config: CmtConfigResponse) => {
          this.dateModification = config.dateModification;
          this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Configuration CMT mise à jour.' });
        }
      });
  }

  get f() { return this.form.controls; }
}
