import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { ChargeFixeConfigService, GenerationChargeFixeLine, GenerationChargeFixeResult } from '../../service/charge-fixe-config.service';

@Component({
  selector: 'app-charge-fixe-rattrapage',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ButtonModule],
  templateUrl: './charge-fixe-rattrapage.component.html',
  styleUrl: './charge-fixe-rattrapage.component.scss'
})
export class ChargeFixeRattrapageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly service = inject(ChargeFixeConfigService);

  readonly form = this.fb.group({ mois: [new Date().toISOString().slice(0, 7), Validators.required] });
  readonly selection = new FormArray<FormControl<boolean>>([]);
  step = 0;
  busy = false;
  result: GenerationChargeFixeResult | null = null;

  preview(): void {
    if (this.form.invalid || this.busy) return;
    const [annee, mois] = (this.form.controls.mois.value ?? '').split('-').map(Number);
    this.busy = true;
    this.service.previewGeneration({ annee, mois }).subscribe({
      next: result => {
        this.result = result;
        this.selection.clear();
        result.lignes.forEach(line => this.selection.push(this.fb.control({
          value: line.statut === 'A_GENERER', disabled: line.statut !== 'A_GENERER'
        }, { nonNullable: true })));
        this.step = 1;
        this.busy = false;
      },
      error: () => { this.busy = false; }
    });
  }

  get selectedIds(): number[] {
    return this.result?.lignes.filter((_, index) => this.selection.at(index).value)
      .map(line => line.configurationId) ?? [];
  }

  get cycleEchu(): boolean {
    const [annee, mois] = (this.form.controls.mois.value ?? '').split('-').map(Number);
    const maintenant = new Date();
    return annee < maintenant.getFullYear()
      || (annee === maintenant.getFullYear() && mois < maintenant.getMonth() + 1);
  }

  run(): void {
    const configurationIds = this.selectedIds;
    if (!this.result || !configurationIds.length || this.busy) return;
    const [annee, mois] = (this.form.controls.mois.value ?? '').split('-').map(Number);
    this.busy = true;
    this.service.generateSelected({ annee, mois, configurationIds }).subscribe({
      next: result => { this.result = result; this.step = 2; this.busy = false; },
      error: () => { this.busy = false; }
    });
  }

  statusLabel(line: GenerationChargeFixeLine): string {
    return ({
      A_GENERER: 'À générer',
      DEJA_PRESENTE: 'Déjà présente',
      DOUBLON_CONFIGURATION: 'Configuration en doublon',
      GENEREE: 'Générée'
    } as const)[line.statut];
  }

  backToConfig(): void { void this.router.navigate(['/portail/admin/charges-fixes-config']); }
}
