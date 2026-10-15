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
  finHorizon,
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
import { PlanTresorerieTableComponent } from '../plan-tresorerie-table/plan-tresorerie-table.component';
import { CaseChoisie, PositionGridComponent } from '../position-grid/position-grid.component';

export type FluxChoisis = 'TOUS' | 'CERTAINS';
export type VueChoisie = 'BANQUE' | 'SOURCE';

/** Un réglage qui s'écarte de la vue par défaut, montré en pastille avec son bouton de retrait. */
export interface FiltreActif {
  cle: string;
  libelle: string;
  valeur: string;
  retirer: () => void;
}

/**
 * Trésorerie prévisionnelle : une phrase de verdict, la position par banque (ou par source), puis le
 * détail d'une banque dans un panneau latéral (ses opérations, ses flux par famille et par source).
 *
 * Tous les réglages sont portés par l'adresse
 * (`?horizon=FIN_DE_TRIMESTRE&flux=certains&vue=source&devise=EUR&banque=3&col=P2`) : l'accueil y mène
 * directement, le retour arrière retrouve la même vue, et les réglages qui s'écartent de la vue par
 * défaut sont rappelés en pastilles sous le verdict.
 */
@Component({
  selector: 'app-tresorerie-previsionnelle',
  standalone: true,
  imports: [
    DatePipe, NgClass, ReactiveFormsModule, RouterLink,
    BreadcrumbModule, ButtonModule, SelectButtonModule, SkeletonModule,
    PositionGridComponent, BanquePanelComponent, CourbeTresorerieComponent, MouvementsListComponent,
    PlanTresorerieTableComponent,
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
  readonly fluxOptions: { label: string; value: FluxChoisis }[] = [
    { label: 'Tous les flux', value: 'TOUS' },
    { label: 'Certains seulement', value: 'CERTAINS' },
  ];
  readonly vueOptions: { label: string; value: VueChoisie }[] = [
    { label: 'Par banque', value: 'BANQUE' },
    { label: 'Par source', value: 'SOURCE' },
  ];

  readonly horizonControl = new FormControl<HorizonPrevision>('FIN_DE_MOIS', { nonNullable: true });
  readonly fluxControl = new FormControl<FluxChoisis>('TOUS', { nonNullable: true });
  readonly vueControl = new FormControl<VueChoisie>('BANQUE', { nonNullable: true });
  /** Devise du total affiché sous la grille ; vide = la première (le dirham). */
  readonly deviseControl = new FormControl<string>('', { nonNullable: true });

  plan: PlanTresorerie | null = null;
  verdict: Verdict | null = null;
  loading = false;
  exporting = false;

  /** Panneau de détail : `undefined` = fermé, `null` = total d'une devise, sinon identifiant de la banque. */
  panneau: number | null | undefined;
  /** Devise du total ouvert dans le panneau quand `panneau` est nul. */
  panneauDevise = '';
  colonne = '';

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    this.horizonControl.setValue(horizonDepuisParam(params.get('horizon')), { emitEvent: false });
    this.fluxControl.setValue(params.get('flux') === 'certains' ? 'CERTAINS' : 'TOUS', { emitEvent: false });
    this.vueControl.setValue(params.get('vue') === 'source' ? 'SOURCE' : 'BANQUE', { emitEvent: false });
    this.deviseControl.setValue(params.get('devise') ?? '', { emitEvent: false });
    const banque = Number(params.get('banque'));
    this.panneau = Number.isInteger(banque) && banque > 0 ? banque : undefined;
    this.colonne = this.panneau !== undefined ? params.get('col') ?? '' : '';

    this.subscriptions.push(
      // Le plan est recalculé côté serveur quand l'horizon ou les flux changent ; la vue et la devise ne sont que de l'affichage
      this.horizonControl.valueChanges.subscribe(() => { this.syncUrl(); this.charger(); }),
      this.fluxControl.valueChanges.subscribe(() => { this.syncUrl(); this.charger(); }),
      this.vueControl.valueChanges.subscribe(() => this.syncUrl()),
      this.deviseControl.valueChanges.subscribe(() => this.syncUrl()),
    );
    this.charger();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  /** Devises présentes ; le sélecteur n'apparaît que s'il y en a plusieurs. */
  get devises(): string[] {
    return this.plan?.groupes.map(g => g.devise) ?? [];
  }

  /** Total affiché sous la grille (courbe, plus gros mouvements, vue par source). */
  get groupeActif(): PlanBanque | null {
    if (!this.plan) {
      return null;
    }
    return this.plan.groupes.find(g => g.devise === this.deviseControl.value) ?? this.plan.groupes[0] ?? null;
  }

  /** Plan affiché dans le panneau ouvert ; `null` si le panneau est fermé ou la banque inconnue. */
  get planDuPanneau(): PlanBanque | null {
    if (!this.plan || this.panneau === undefined) {
      return null;
    }
    return this.panneau === null
      ? this.plan.groupes.find(g => g.devise === this.panneauDevise) ?? this.plan.groupes[0] ?? null
      : this.plan.banques.find(b => b.banqueId === this.panneau) ?? null;
  }

  /** Réglages qui s'écartent de la vue par défaut (horizon : fin de mois, tous les flux, par banque, panneau fermé). */
  get filtres(): FiltreActif[] {
    const filtres: FiltreActif[] = [];
    const horizon = this.horizonControl.value;
    if (horizon !== 'FIN_DE_MOIS') {
      filtres.push({
        cle: 'horizon', libelle: 'Horizon',
        valeur: `Fin de trimestre (${finHorizon(horizon).toLocaleDateString('fr-FR')})`,
        retirer: () => this.horizonControl.setValue('FIN_DE_MOIS'),
      });
    }
    if (this.fluxControl.value === 'CERTAINS') {
      filtres.push({ cle: 'flux', libelle: 'Flux', valeur: 'Certains seulement', retirer: () => this.fluxControl.setValue('TOUS') });
    }
    if (this.vueControl.value === 'SOURCE') {
      filtres.push({ cle: 'vue', libelle: 'Vue', valeur: 'Par source', retirer: () => this.vueControl.setValue('BANQUE') });
    }
    if (this.devises.length > 1 && this.deviseControl.value && this.deviseControl.value !== this.devises[0]) {
      filtres.push({ cle: 'devise', libelle: 'Devise', valeur: this.deviseControl.value, retirer: () => this.deviseControl.setValue('') });
    }
    const detail = typeof this.panneau === 'number' ? this.planDuPanneau : null;
    if (detail) {
      filtres.push({ cle: 'banque', libelle: 'Banque', valeur: detail.banqueNom, retirer: () => this.fermer() });
      const periode = this.plan!.colonnes.find(c => c.code === this.colonne);
      if (periode) {
        filtres.push({ cle: 'periode', libelle: 'Période', valeur: periode.libelle, retirer: () => this.choisirColonne('') });
      }
    }
    return filtres;
  }

  reinitialiser(): void {
    this.panneau = undefined;
    this.colonne = '';
    this.vueControl.setValue('BANQUE', { emitEvent: false });
    this.deviseControl.setValue('', { emitEvent: false });
    const recharger = this.horizonControl.value !== 'FIN_DE_MOIS' || this.fluxControl.value !== 'TOUS';
    this.horizonControl.setValue('FIN_DE_MOIS', { emitEvent: false });
    this.fluxControl.setValue('TOUS', { emitEvent: false });
    this.syncUrl();
    if (recharger) {
      this.charger();
    }
  }

  ouvrir(choix: CaseChoisie): void {
    this.panneau = choix.banqueId;
    this.panneauDevise = choix.devise;
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

  exporterPdf(banqueId: number | null, devise?: string): void {
    const periode = this.horizonControl.value;
    this.exporting = true;
    this.tresorerieService.exportPdf(periode, this.fluxControl.value === 'CERTAINS', banqueId, devise).subscribe({
      next: blob => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Plan_tresorerie_${banqueId ?? devise ?? 'toutes_banques'}_${periode.toLowerCase()}.pdf`;
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
      this.tresorerieService.getPlan(this.horizonControl.value, this.fluxControl.value === 'CERTAINS').subscribe({
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

  /** Écrit les réglages dans l'adresse sans ajouter d'entrée d'historique ; le total d'une devise n'y figure pas. */
  private syncUrl(): void {
    const horizon = this.horizonControl.value;
    const banque = typeof this.panneau === 'number' ? this.panneau : null;
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        horizon: horizon === 'FIN_DE_TRIMESTRE' ? horizon : null,
        flux: this.fluxControl.value === 'CERTAINS' ? 'certains' : null,
        vue: this.vueControl.value === 'SOURCE' ? 'source' : null,
        devise: this.deviseControl.value || null,
        banque,
        col: banque !== null && this.colonne ? this.colonne : null,
      },
      replaceUrl: true,
    });
  }
}
