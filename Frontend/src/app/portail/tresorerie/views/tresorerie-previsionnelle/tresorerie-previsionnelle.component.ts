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
import { DetailBanqueComponent } from '../detail-banque/detail-banque.component';
import { ConsoleFiltresComponent } from '../console-filtres/console-filtres.component';
import { TotalGroupeComponent } from '../total-groupe/total-groupe.component';
import { CaseChoisie, PositionGridComponent } from '../position-grid/position-grid.component';

/** Ce que produit le bouton « Exporter PDF » : le total d'une devise. */
export interface PerimetreExport {
  devise: string;
  libelle: string;
}

/**
 * Trésorerie prévisionnelle : une console qui réunit **tous les filtres et l'export**, le total groupe en
 * tête, une carte par banque (classées par urgence), le détail du total (déplié sous les cartes), puis la
 * grille Banque › Source. Un clic sur une banque ouvre sa page dédiée (`banque/:banqueId`).
 *
 * Les filtres sont portés par l'adresse (`?horizon=FIN_DE_TRIMESTRE&devise=EUR`) : le retour arrière retrouve
 * la même vue. L'ancienne adresse `?banque=3&col=P2` est redirigée vers la page de la banque.
 */
@Component({
  selector: 'app-tresorerie-previsionnelle',
  standalone: true,
  imports: [
    NgClass, RouterLink, BreadcrumbModule, SkeletonModule,
    ConsoleFiltresComponent, TotalGroupeComponent, CartesBanquesComponent, PositionGridComponent, DetailBanqueComponent,
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

  /** Détail du total déplié : `undefined` = fermé, `null` = ouvert. */
  panneau: null | undefined;
  /** Devise du total ouvert dans le panneau. */
  panneauDevise = '';

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    this.horizonControl.setValue(horizonDepuisParam(params.get('horizon')), { emitEvent: false });
    this.deviseControl.setValue(params.get('devise') ?? '', { emitEvent: false });
    const banque = Number(params.get('banque'));
    if (Number.isInteger(banque) && banque > 0) {
      // Ancienne adresse du détail d'une banque : la banque a désormais sa page
      this.ouvrirBanque(banque, params.get('col') ?? '', true);
      return;
    }

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

  /** Total affiché dans le panneau ouvert ; `null` si le panneau est fermé. */
  get planDuPanneau(): PlanBanque | null {
    if (!this.plan || this.panneau === undefined) {
      return null;
    }
    return this.plan.groupes.find(g => g.devise === this.panneauDevise) ?? this.plan.groupes[0] ?? null;
  }

  /** Ce que l'export produira : le total de la devise ouverte, sinon celui de la devise choisie. */
  get perimetreExport(): PerimetreExport | null {
    const groupe = this.planDuPanneau ?? this.groupeActif;
    if (!groupe) {
      return null;
    }
    return { devise: groupe.devise, libelle: this.devises.length > 1 ? `Total ${groupe.devise}` : 'Total groupe' };
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
    return filtres;
  }

  reinitialiser(): void {
    this.panneau = undefined;
    this.deviseControl.setValue('', { emitEvent: false });
    const recharger = this.horizonControl.value !== 'FIN_DE_MOIS';
    this.horizonControl.setValue('FIN_DE_MOIS', { emitEvent: false });
    this.syncUrl();
    if (recharger) {
      this.charger();
    }
  }

  /** Une banque ouvre sa page dédiée ; le total d'une devise se déplie sous les cartes (un second clic le referme). */
  ouvrir(choix: CaseChoisie): void {
    if (choix.banqueId !== null) {
      this.ouvrirBanque(choix.banqueId, choix.colonne);
      return;
    }
    if (this.panneau === null && this.panneauDevise === choix.devise) {
      this.fermer();
      return;
    }
    this.panneau = null;
    this.panneauDevise = choix.devise;
  }

  /** Page du plan d'une banque, avec l'horizon de l'écran et, s'il y en a une, la période à ouvrir. */
  ouvrirBanque(banqueId: number, colonne: string, remplacer = false): void {
    const horizon = this.horizonControl.value;
    this.router.navigate(['banque', banqueId], {
      relativeTo: this.route,
      queryParams: { horizon: horizon === 'FIN_DE_TRIMESTRE' ? horizon : null, col: colonne || null },
      replaceUrl: remplacer,
    });
  }

  fermer(): void {
    this.panneau = undefined;
  }

  exporterPdf(): void {
    const perimetre = this.perimetreExport;
    if (!perimetre) {
      return;
    }
    const periode = this.horizonControl.value;
    this.exporting = true;
    this.tresorerieService.exportPdf(periode, null, perimetre.devise).subscribe({
      next: blob => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Plan_tresorerie_${perimetre.devise}_${periode.toLowerCase()}.pdf`;
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

  /** Écrit les filtres dans l'adresse sans ajouter d'entrée d'historique ; le total ouvert n'y figure pas. */
  private syncUrl(): void {
    const horizon = this.horizonControl.value;
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        horizon: horizon === 'FIN_DE_TRIMESTRE' ? horizon : null,
        devise: this.deviseControl.value || null,
      },
      replaceUrl: true,
    });
  }
}
