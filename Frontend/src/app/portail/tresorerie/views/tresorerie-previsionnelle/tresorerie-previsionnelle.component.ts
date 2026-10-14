import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { DatePipe, DecimalPipe, NgClass } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { MenuItem } from 'primeng/api';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { SelectButtonModule } from 'primeng/selectbutton';
import { SkeletonModule } from 'primeng/skeleton';
import { TooltipModule } from 'primeng/tooltip';
import {
  HORIZON_OPTIONS,
  HorizonPrevision,
  horizonDepuisParam,
} from '../../../etatsDesEncours/views/lesEncours/recap/dtos/PeriodeRecap';
import { PlanBanque, PlanTresorerie } from '../../dtos/PlanTresorerie';
import { TresorerieService } from '../../services/tresorerie.service';
import { PlanTresorerieTableComponent } from '../plan-tresorerie-table/plan-tresorerie-table.component';
import { CourbeTresorerieComponent } from '../courbe-tresorerie/courbe-tresorerie.component';

/**
 * Trésorerie prévisionnelle : du solde actuel jusqu'à la fin du mois ou du trimestre, pour toutes
 * les banques ou une seule — indicateurs (solde prévu, point bas, besoin de financement), courbe
 * du solde, plan par catégorie et comparaison des banques.
 *
 * L'horizon et la banque sont portés par l'adresse (`?horizon=FIN_DE_TRIMESTRE&banque=3`) : les
 * liens de l'accueil y mènent directement, et le retour arrière retrouve la même vue.
 */
@Component({
  selector: 'app-tresorerie-previsionnelle',
  standalone: true,
  imports: [
    DatePipe, DecimalPipe, NgClass, ReactiveFormsModule, RouterLink,
    BreadcrumbModule, ButtonModule, DropdownModule, SelectButtonModule, SkeletonModule, TooltipModule,
    PlanTresorerieTableComponent, CourbeTresorerieComponent,
  ],
  templateUrl: './tresorerie-previsionnelle.component.html',
  styleUrl: './tresorerie-previsionnelle.component.scss'
})
export class TresoreriePrevisionnelleComponent implements OnInit, OnDestroy {

  private readonly tresorerieService = inject(TresorerieService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly subscriptions: Subscription[] = [];

  readonly items: MenuItem[] = [
    { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
    { label: 'Trésorerie prévisionnelle' },
  ];
  readonly horizonOptions = HORIZON_OPTIONS;

  readonly horizonControl = new FormControl<HorizonPrevision>('FIN_DE_MOIS', { nonNullable: true });
  readonly banqueControl = new FormControl<number | null>(null);

  plan: PlanTresorerie | null = null;
  banqueOptions: { label: string; value: number | null }[] = [];
  loading = false;
  exporting = false;

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    this.horizonControl.setValue(horizonDepuisParam(params.get('horizon')), { emitEvent: false });
    const banque = Number(params.get('banque'));
    this.banqueControl.setValue(Number.isInteger(banque) && banque > 0 ? banque : null, { emitEvent: false });

    this.subscriptions.push(
      this.horizonControl.valueChanges.subscribe(() => {
        this.syncUrl();
        this.charger();
      }),
      this.banqueControl.valueChanges.subscribe(() => this.syncUrl()),
    );
    this.charger();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  /** Périmètre affiché : la banque choisie, ou toutes les banques consolidées. */
  get perimetre(): PlanBanque | null {
    if (!this.plan) {
      return null;
    }
    const id = this.banqueControl.value;
    return (id !== null && this.plan.banques.find(b => b.banqueId === id)) || this.plan.consolide;
  }

  get vueConsolidee(): boolean {
    return this.perimetre?.banqueId === null;
  }

  /** Écart entre le solde prévu à l'horizon et le solde actuel. */
  get variation(): number {
    const p = this.perimetre;
    return p ? p.soldeFinal - p.soldeActuel : 0;
  }

  /** Niveau d'alerte du périmètre : besoin non couvert, découvert couvert par la facilité, ou sain. */
  get alerte(): 'besoin' | 'decouvert' | null {
    const p = this.perimetre;
    if (!p) {
      return null;
    }
    if (p.besoinFinancement > 0) {
      return 'besoin';
    }
    return p.joursSoldeNegatif > 0 ? 'decouvert' : null;
  }

  choisirBanque(banqueId: number | null): void {
    this.banqueControl.setValue(banqueId);
  }

  exporterPdf(): void {
    const periode = this.horizonControl.value;
    const banqueId = this.vueConsolidee ? null : this.perimetre?.banqueId ?? null;
    this.exporting = true;
    this.tresorerieService.exportPdf(periode, banqueId).subscribe({
      next: blob => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Plan_tresorerie_${banqueId ?? 'toutes_banques'}_${periode.toLowerCase()}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.exporting = false;
      },
      error: () => {
        this.exporting = false;
      }
    });
  }

  private charger(): void {
    this.loading = true;
    this.subscriptions.push(
      this.tresorerieService.getPlan(this.horizonControl.value).subscribe({
        next: plan => {
          this.plan = plan;
          this.banqueOptions = [
            { label: 'Toutes les banques', value: null },
            ...plan.banques.map(b => ({ label: b.banqueNom, value: b.banqueId })),
          ];
          this.loading = false;
        },
        error: () => {
          this.loading = false;
        }
      })
    );
  }

  private syncUrl(): void {
    const horizon = this.horizonControl.value;
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        horizon: horizon === 'FIN_DE_TRIMESTRE' ? horizon : null,
        banque: this.banqueControl.value,
      },
      replaceUrl: true,
    });
  }
}
