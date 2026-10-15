import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { MenuItem } from 'primeng/api';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ButtonModule } from 'primeng/button';
import { SelectButtonModule } from 'primeng/selectbutton';
import { SkeletonModule } from 'primeng/skeleton';
import {
  HORIZON_OPTIONS,
  HorizonPrevision,
  horizonDepuisParam,
} from '../../../etatsDesEncours/views/lesEncours/recap/dtos/PeriodeRecap';
import { PlanBanque, PlanTresorerie } from '../../dtos/PlanTresorerie';
import { TresorerieService } from '../../services/tresorerie.service';
import { Verdict, verdictDuPlan } from '../../verdict';
import { BanquePanelComponent } from '../banque-panel/banque-panel.component';
import { CourbeTresorerieComponent } from '../courbe-tresorerie/courbe-tresorerie.component';
import { MouvementsListComponent } from '../mouvements-list/mouvements-list.component';
import { CaseChoisie, PositionGridComponent } from '../position-grid/position-grid.component';

/**
 * Trésorerie prévisionnelle : une phrase de verdict, la grille de position (banques × périodes), puis
 * le détail d'une banque dans un panneau latéral (ses opérations, ses flux par famille).
 *
 * L'horizon, la banque ouverte et la période choisie sont portés par l'adresse
 * (`?horizon=FIN_DE_TRIMESTRE&banque=3&col=P2`) : l'accueil y mène directement, et le retour
 * arrière retrouve la même vue.
 */
@Component({
  selector: 'app-tresorerie-previsionnelle',
  standalone: true,
  imports: [
    DatePipe, NgClass, ReactiveFormsModule, RouterLink,
    BreadcrumbModule, ButtonModule, SelectButtonModule, SkeletonModule,
    PositionGridComponent, BanquePanelComponent, CourbeTresorerieComponent, MouvementsListComponent,
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

  plan: PlanTresorerie | null = null;
  verdict: Verdict | null = null;
  loading = false;
  exporting = false;

  /** Panneau de détail : `undefined` = fermé, `null` = total du groupe, sinon identifiant de la banque. */
  panneau: number | null | undefined;
  colonne = '';

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    this.horizonControl.setValue(horizonDepuisParam(params.get('horizon')), { emitEvent: false });
    const banque = Number(params.get('banque'));
    this.panneau = Number.isInteger(banque) && banque > 0 ? banque : undefined;
    this.colonne = this.panneau !== undefined ? params.get('col') ?? '' : '';

    this.subscriptions.push(
      this.horizonControl.valueChanges.subscribe(() => {
        this.syncUrl();
        this.charger();
      }),
    );
    this.charger();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  /** Plan affiché dans le panneau ouvert ; `null` si le panneau est fermé ou la banque inconnue. */
  get planDuPanneau(): PlanBanque | null {
    if (!this.plan || this.panneau === undefined) {
      return null;
    }
    return this.panneau === null
      ? this.plan.consolide
      : this.plan.banques.find(b => b.banqueId === this.panneau) ?? null;
  }

  ouvrir(choix: CaseChoisie): void {
    this.panneau = choix.banqueId;
    this.colonne = choix.colonne;
    this.syncUrl();
  }

  fermer(): void {
    this.panneau = undefined;
    this.colonne = '';
    this.syncUrl();
  }

  choisirColonne(code: string): void {
    this.colonne = code;
    this.syncUrl();
  }

  exporterPdf(banqueId: number | null): void {
    const periode = this.horizonControl.value;
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
          this.verdict = verdictDuPlan(plan);
          this.loading = false;
        },
        error: () => {
          this.loading = false;
        }
      })
    );
  }

  /** Écrit l'horizon, la banque et la période dans l'adresse sans ajouter d'entrée d'historique. Le total du groupe n'y figure pas. */
  private syncUrl(): void {
    const horizon = this.horizonControl.value;
    const banque = typeof this.panneau === 'number' ? this.panneau : null;
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        horizon: horizon === 'FIN_DE_TRIMESTRE' ? horizon : null,
        banque,
        col: banque !== null && this.colonne ? this.colonne : null,
      },
      replaceUrl: true,
    });
  }
}
