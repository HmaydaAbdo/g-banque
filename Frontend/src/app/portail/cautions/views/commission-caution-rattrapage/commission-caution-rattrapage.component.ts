import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { MultiSelectModule } from 'primeng/multiselect';
import { BanqueResponse } from '../../../comptes-bancaire/dtos/BanqueResponse';
import { BanqueService } from '../../../comptes-bancaire/services/BanqueService';
import { CautionService, RattrapageCommissionCautionLine, RattrapageCommissionCautionResult } from '../../service/caution.service';

@Component({
  selector: 'app-commission-caution-rattrapage',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ButtonModule, MultiSelectModule],
  templateUrl: './commission-caution-rattrapage.component.html',
  styleUrl: './commission-caution-rattrapage.component.scss'
})
export class CommissionCautionRattrapageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly cautionService = inject(CautionService);
  private readonly banqueService = inject(BanqueService);

  readonly form = this.fb.group({
    mois: [this.currentMonth(), Validators.required],
    debutMois: [false, Validators.required],
    banqueIds: [[] as number[]]
  });
  readonly selection = new FormArray<FormControl<boolean>>([]);
  banques: BanqueResponse[] = [];
  step = 0;
  busy = false;
  result: RattrapageCommissionCautionResult | null = null;

  ngOnInit(): void {
    this.banqueService.getAllBanques({}, 0, 1000).subscribe({
      next: page => this.banques = page.content,
      error: () => { this.banques = []; }
    });
  }

  get banqueOptions() { return this.banques.map(b => ({ label: b.nom, value: b.id })); }
  get selectedIds(): string[] {
    return this.result?.lignes.filter((_, index) => this.selection.at(index).value)
      .map(line => `${line.cautionId}:${line.typeCommission}`) ?? [];
  }

  get cycleEchu(): boolean {
    const [annee, mois] = (this.form.controls.mois.value ?? '').split('-').map(Number);
    const debutMois = this.form.controls.debutMois.value ?? false;
    const maintenant = new Date();
    const anneeCourante = maintenant.getFullYear();
    const moisCourant = maintenant.getMonth() + 1;
    if (annee < anneeCourante || (annee === anneeCourante && mois < moisCourant)) return true;
    if (annee !== anneeCourante || mois !== moisCourant) return false;
    const dateEcheance = debutMois
      ? new Date(annee, mois - 1, 1, 3, 0)
      : new Date(annee, mois, 0, 20, 0);
    return maintenant >= dateEcheance;
  }

  preview(): void {
    if (this.form.invalid || this.busy) return;
    const [annee, mois] = (this.form.controls.mois.value ?? '').split('-').map(Number);
    const banqueIds = this.form.controls.banqueIds.value;
    this.busy = true;
    this.cautionService.previewCommissionCatchUp({
      annee, mois, debutMois: this.form.controls.debutMois.value ?? false,
      banqueIds: banqueIds?.length ? banqueIds : null
    }).subscribe({
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

  run(): void {
    const lignesSelectionnees = this.selectedIds;
    if (!this.result || !lignesSelectionnees.length || this.busy) return;
    const [annee, mois] = (this.form.controls.mois.value ?? '').split('-').map(Number);
    const banqueIds = this.form.controls.banqueIds.value;
    this.busy = true;
    this.cautionService.runCommissionCatchUp({
      annee, mois, debutMois: this.form.controls.debutMois.value ?? false,
      banqueIds: banqueIds?.length ? banqueIds : null, lignesSelectionnees
    }).subscribe({
      next: result => { this.result = result; this.step = 2; this.busy = false; },
      error: () => { this.busy = false; }
    });
  }

  statusLabel(line: RattrapageCommissionCautionLine): string {
    return ({
      A_GENERER: 'À générer',
      DEJA_PRESENTE: 'Déjà présente',
      ORIGINE_HISTORIQUE_A_CONTROLER: 'Origine à vérifier',
      GENEREE: 'Générée'
    } as const)[line.statut];
  }

  backToCommissions(): void { void this.router.navigate(['/portail/cautions/commissions']); }

  private currentMonth(): string {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  }
}
