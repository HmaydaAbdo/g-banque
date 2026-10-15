import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { NgClass } from '@angular/common';
import { FormControl } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { MenuItem } from 'primeng/api';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { SkeletonModule } from 'primeng/skeleton';
import {
  finHorizon,
  HorizonPrevision,
  horizonDepuisParam,
} from '../../../etatsDesEncours/views/lesEncours/recap/dtos/PeriodeRecap';
import { PlanBanque, PlanTresorerie } from '../../dtos/PlanTresorerie';
import { FiltreActif } from '../../filtre-actif';
import { TresorerieService } from '../../services/tresorerie.service';
import { compterParSituation, NiveauSituation } from '../../situation';
import { CartesBanquesComponent } from '../cartes-banques/cartes-banques.component';
import { BanquePanelComponent } from '../banque-panel/banque-panel.component';
import { ConsoleFiltresComponent } from '../console-filtres/console-filtres.component';
import { CourbeTresorerieComponent } from '../courbe-tresorerie/courbe-tresorerie.component';
import { MouvementsListComponent } from '../mouvements-list/mouvements-list.component';
import { TotalGroupeComponent } from '../total-groupe/total-groupe.component';
import { CaseChoisie, PositionGridComponent } from '../position-grid/position-grid.component';

/** Ce que produit le bouton « Exporter PDF » : la banque ouverte, sinon le total d'une devise. */
export interface PerimetreExport {
  banqueId: number | null;
  devise: string;
  libelle: string;
}

/**
 * Trésorerie prévisionnelle : une console qui réunit **tous les filtres et l'export**, le total groupe en
 * tête, une carte par banque (classées par urgence), la grille Banque › Source, la courbe et les plus grosses
 * opérations, puis le détail d'une banque dans un panneau latéral.
 *
 * Tous les filtres sont portés par l'adresse (`?horizon=FIN_DE_TRIMESTRE&devise=EUR&banque=3&col=P2`) :
 * l'accueil y mène directement et le retour arrière retrouve la même vue.
 */
@Component({
  selector: 'app-tresorerie-previsionnelle',
  standalone: true,
  imports: [
    NgClass, RouterLink, BreadcrumbModule, SkeletonModule,
    ConsoleFiltresComponent, TotalGroupeComponent, CartesBanquesComponent, PositionGridComponent, BanquePanelComponent,
    CourbeTresorerieComponent, MouvementsListComponent,
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

  readonly horizonControl = new FormControl<HorizonPrevision>('FIN_DE_MOIS', { nonNullable: true });
  /** Devise du bandeau, de la courbe, des plus grosses opérations et de l'export ; vide = la première (le dirham). */
  readonly deviseControl = new FormControl<string>('', { nonNullable: true });

  plan: PlanTresorerie | null = null;
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
    this.deviseControl.setValue(params.get('devise') ?? '', { emitEvent: false });
    const banque = Number(params.get('banque'));
    this.panneau = Number.isInteger(banque) && banque > 0 ? banque : undefined;
    this.colonne = this.panneau !== undefined ? params.get('col') ?? '' : '';

    this.subscriptions.push(
      // Le plan est recalculé côté serveur quand l'horizon change ; la devise n'est que de l'affichage
      this.horizonControl.valueChanges.subscribe(() => { this.syncUrl(); this.charger(); }),
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

  /** Total de la devise choisie (bandeau, courbe, plus grosses opérations). */
  get groupeActif(): PlanBanque | null {
    if (!this.plan) {
      return null;
    }
    return this.plan.groupes.find(g => g.devise === this.deviseControl.value) ?? this.plan.groupes[0] ?? null;
  }

  /** Banques de la devise choisie : celles des cartes et du compte du total. */
  get banquesDevise(): PlanBanque[] {
    const devise = this.groupeActif?.devise;
    return this.plan?.banques.filter(b => b.devise === devise) ?? [];
  }

  get compte(): Record<NiveauSituation, number> {
    return compterParSituation(this.banquesDevise);
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

  /** Ce que l'export produira : la banque dont le panneau est ouvert, sinon le total de la devise choisie. */
  get perimetreExport(): PerimetreExport | null {
    const detail = this.planDuPanneau;
    if (detail) {
      return { banqueId: detail.banqueId, devise: detail.devise, libelle: detail.banqueNom };
    }
    const groupe = this.groupeActif;
    if (!groupe) {
      return null;
    }
    return { banqueId: null, devise: groupe.devise, libelle: this.devises.length > 1 ? `Total ${groupe.devise}` : 'Total groupe' };
  }

  /** Filtres qui s'écartent de la vue par défaut (fin de mois, première devise, aucun panneau ouvert). */
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
    this.deviseControl.setValue('', { emitEvent: false });
    const recharger = this.horizonControl.value !== 'FIN_DE_MOIS';
    this.horizonControl.setValue('FIN_DE_MOIS', { emitEvent: false });
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

  exporterPdf(): void {
    const perimetre = this.perimetreExport;
    if (!perimetre) {
      return;
    }
    const periode = this.horizonControl.value;
    this.exporting = true;
    this.tresorerieService.exportPdf(periode, perimetre.banqueId, perimetre.devise).subscribe({
      next: blob => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Plan_tresorerie_${perimetre.banqueId ?? perimetre.devise}_${periode.toLowerCase()}.pdf`;
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
          this.loading = false;
        },
        error: () => {
          this.loading = false;
        }
      })
    );
  }

  /** Écrit les filtres dans l'adresse sans ajouter d'entrée d'historique ; le total d'une devise n'y figure pas. */
  private syncUrl(): void {
    const horizon = this.horizonControl.value;
    const banque = typeof this.panneau === 'number' ? this.panneau : null;
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        horizon: horizon === 'FIN_DE_TRIMESTRE' ? horizon : null,
        devise: this.deviseControl.value || null,
        banque,
        col: banque !== null && this.colonne ? this.colonne : null,
      },
      replaceUrl: true,
    });
  }
}
