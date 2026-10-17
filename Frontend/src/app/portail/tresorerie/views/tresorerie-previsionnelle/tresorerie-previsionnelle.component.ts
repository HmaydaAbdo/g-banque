import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { NgClass } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, Subscription } from 'rxjs';
import { MenuItem } from 'primeng/api';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { SkeletonModule } from 'primeng/skeleton';
import { HorizonPrevision, horizonDepuisParam } from '../../../etatsDesEncours/views/lesEncours/recap/dtos/PeriodeRecap';
import { RecaputilatifItem } from '../../../etatsDesEncours/views/lesEncours/recap/dtos/RecaputilatifItem';
import { RecapService } from '../../../etatsDesEncours/views/lesEncours/recap/services/recap.service';
import { TransactionSource, TransactionSourceLabels } from '../../../transactions/dtos/TransactionSource';
import { ColonnePlan, PlanBanque, PlanTresorerie } from '../../dtos/PlanTresorerie';
import { garder, montantDe, Operateur, OPERATEURS } from '../../plan-banque';
import {
  banquesDuPerimetre, FILTRES_GLOBAL_VIDES, FiltresGlobal,
} from '../../plan-global';
import { TresorerieService } from '../../services/tresorerie.service';
import { BarreFiltresComponent, OptionFiltre, ResumeFiltre } from '../barre-filtres/barre-filtres.component';
import { RecitGlobalComponent } from '../recit-global/recit-global.component';
import { TableauGlobalComponent } from '../tableau-global/tableau-global.component';

export type VueGlobale = 'recit' | 'tableau';

/**
 * Trésorerie prévisionnelle — toutes les banques, sur le modèle de la page d'une banque : une seule barre (banques,
 * sens, sources, montant, recherche, horizon, devise, export) et deux vues séparées, **Récit** (résumé du groupe,
 * bandeau « À surveiller », frise des périodes, jalons par banque) et **Tableau** (banques × périodes). Un clic sur une
 * banque ouvre sa page (`banque/:banqueId`). Tout l'état est porté par l'adresse (`?horizon=…&vue=tableau&banques=1,2
 * &sens=…&sources=…&op=…&montant=…&q=…&ouvert=P1`). Les filtres ne changent jamais les soldes.
 */
@Component({
  selector: 'app-tresorerie-previsionnelle',
  standalone: true,
  imports: [
    NgClass, RouterLink, BreadcrumbModule, SkeletonModule,
    BarreFiltresComponent, RecitGlobalComponent, TableauGlobalComponent,
  ],
  templateUrl: './tresorerie-previsionnelle.component.html',
  styleUrl: './tresorerie-previsionnelle.component.scss'
})
export class TresoreriePrevisionnelleComponent implements OnInit, OnDestroy {

  private readonly tresorerieService = inject(TresorerieService);
  private readonly recapService = inject(RecapService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly subscriptions: Subscription[] = [];
  private chargement?: Subscription;

  readonly items: MenuItem[] = [
    { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
    { label: 'Trésorerie prévisionnelle' },
  ];

  horizon: HorizonPrevision = 'FIN_DE_MOIS';
  vue: VueGlobale = 'recit';
  /** Devise affichée ; vide = la première (le dirham). */
  devise = '';
  filtres: FiltresGlobal = { ...FILTRES_GLOBAL_VIDES };
  /** Codes des étapes ouvertes dans le récit. */
  ouvertes = new Set<string>();

  plan: PlanTresorerie | null = null;
  operations: RecaputilatifItem[] = [];
  loading = false;
  erreur = false;
  exporting = false;
  private ouverturePar = false;

  ngOnInit(): void {
    this.lireAdresse();
    const banque = Number(this.route.snapshot.queryParamMap.get('banque'));
    if (Number.isInteger(banque) && banque > 0) {
      // Ancienne adresse du détail d'une banque : la banque a désormais sa page
      this.ouvrirBanque({ banqueId: banque, colonne: this.route.snapshot.queryParamMap.get('col') ?? '' }, true);
      return;
    }
    this.charger();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
    this.chargement?.unsubscribe();
  }

  // ── Données dérivées ────────────────────────────────────────────────────

  get colonnes(): ColonnePlan[] {
    return this.plan?.colonnes ?? [];
  }

  get devises(): string[] {
    return this.plan?.groupes.map(g => g.devise) ?? [];
  }

  get deviseActive(): string {
    return this.devise && this.devises.includes(this.devise) ? this.devise : this.devises[0] ?? 'MAD';
  }

  /** Banques de la devise, restreintes au filtre « Banques » : le périmètre de la page. */
  get banques(): PlanBanque[] {
    return this.plan ? banquesDuPerimetre(this.plan.banques, this.deviseActive, this.filtres.banques) : [];
  }

  /** Banques proposées au filtre : celles de la devise affichée. */
  get banquesDisponibles(): OptionFiltre<number>[] {
    return (this.plan ? banquesDuPerimetre(this.plan.banques, this.deviseActive, []) : [])
      .map(b => ({ label: b.banqueNom, value: b.banqueId! }));
  }

  /** Sources présentes dans les opérations du périmètre, avec leur nombre. */
  get sourcesDisponibles(): OptionFiltre<TransactionSource>[] {
    const ids = new Set(this.banques.map(b => b.banqueId));
    const nombres = new Map<TransactionSource, number>();
    this.operations.filter(o => ids.has(o.banqueId ?? null))
      .forEach(o => nombres.set(o.source, (nombres.get(o.source) ?? 0) + 1));
    return [...nombres.entries()]
      .map(([value, n]) => ({ value, label: `${TransactionSourceLabels[value] ?? value} (${n})` }))
      .sort((a, b) => a.label.localeCompare(b.label));
  }

  get operationsPerimetre(): RecaputilatifItem[] {
    const ids = new Set(this.banques.map(b => b.banqueId));
    return this.operations.filter(o => ids.has(o.banqueId ?? null));
  }

  get resumeFiltre(): ResumeFiltre {
    const retenues = this.operationsPerimetre.filter(o => garder(o, this.filtres));
    const somme = (sens: string) => retenues.filter(o => o.sens === sens).reduce((s, o) => s + montantDe(o), 0);
    return { nombre: retenues.length, entrees: somme('ENCAISSEMENT'), sorties: somme('DECAISSEMENT') };
  }

  // ── Actions ─────────────────────────────────────────────────────────────

  appliquer(changement: Partial<FiltresGlobal>): void {
    this.filtres = { ...this.filtres, ...changement };
    this.ecrireAdresse();
  }

  effacer(): void {
    this.filtres = { ...FILTRES_GLOBAL_VIDES };
    this.ecrireAdresse();
  }

  choisirHorizon(horizon: HorizonPrevision): void {
    this.horizon = horizon;
    this.ouvertes.clear();
    this.ecrireAdresse();
    this.charger();
  }

  choisirDevise(devise: string): void {
    this.devise = devise;
    this.filtres = { ...this.filtres, banques: [], sources: [] };
    this.ecrireAdresse();
  }

  choisirVue(vue: VueGlobale): void {
    this.vue = vue;
    this.ecrireAdresse();
  }

  basculer(code: string): void {
    const ouvertes = new Set(this.ouvertes);
    if (!ouvertes.delete(code)) {
      ouvertes.add(code);
    }
    this.ouvertes = ouvertes;
    this.ecrireAdresse();
  }

  toutOuvrir(ouvrir: boolean): void {
    this.ouvertes = new Set(ouvrir ? this.colonnes.map(c => c.code) : []);
    this.ecrireAdresse();
  }

  /** Page du plan d'une banque, avec l'horizon de l'écran et, s'il y en a une, la période à ouvrir. */
  ouvrirBanque(choix: { banqueId: number; colonne: string }, remplacer = false): void {
    this.router.navigate(['banque', choix.banqueId], {
      relativeTo: this.route,
      queryParams: { horizon: this.horizon === 'FIN_DE_TRIMESTRE' ? this.horizon : null, col: choix.colonne || null },
      replaceUrl: remplacer,
    });
  }

  get exportInfo(): string {
    return `Plan du total ${this.deviseActive} à l'horizon choisi (sans les filtres) — une banque s'exporte depuis sa page`;
  }

  exporterPdf(): void {
    const periode = this.horizon;
    this.exporting = true;
    this.tresorerieService.exportPdf(periode, null, this.deviseActive).subscribe({
      next: blob => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Plan_tresorerie_${this.deviseActive}_${periode.toLowerCase()}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.exporting = false;
      },
      error: () => {
        this.exporting = false;
      },
    });
  }

  // ── Chargement et adresse ───────────────────────────────────────────────

  private charger(): void {
    this.chargement?.unsubscribe();
    this.loading = true;
    this.erreur = false;
    this.chargement = forkJoin({
      plan: this.tresorerieService.getPlan(this.horizon),
      operations: this.recapService.getEncoursToutesBanques(this.horizon),
    }).subscribe({
      next: ({ plan, operations }) => {
        this.plan = plan;
        this.operations = operations;
        if (!this.ouverturePar && this.ouvertes.size === 0 && plan.colonnes.length > 1) {
          // Ouverture par défaut : la première période qui contient une banque en dépassement, sinon la première
          const banques = banquesDuPerimetre(plan.banques, this.deviseActive, this.filtres.banques);
          const colonne = plan.colonnes.find((_, k) => banques.some(b => b.soldesFin[k] + Math.max(b.plafondFC, 0) < 0));
          this.ouvertes = new Set(colonne ? [colonne.code] : []);
        }
        this.ouverturePar = true;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.erreur = true;
      },
    });
  }

  private lireAdresse(): void {
    const q = this.route.snapshot.queryParamMap;
    this.horizon = horizonDepuisParam(q.get('horizon'));
    this.vue = q.get('vue') === 'tableau' ? 'tableau' : 'recit';
    this.devise = q.get('devise') ?? '';
    const sens = q.get('sens');
    const op = q.get('op') as Operateur | null;
    const montant = Number(q.get('montant'));
    this.filtres = {
      banques: (q.get('banques') ?? '').split(',').map(Number).filter(n => Number.isInteger(n) && n > 0),
      sens: sens === 'ENCAISSEMENT' || sens === 'DECAISSEMENT' ? sens : 'TOUS',
      sources: (q.get('sources') ?? '').split(',').filter(Boolean) as TransactionSource[],
      operateur: OPERATEURS.some(o => o.value === op) ? op! : '>=',
      montant: q.get('montant') !== null && Number.isFinite(montant) && montant >= 0 ? montant : null,
      texte: q.get('q') ?? '',
    };
    this.ouvertes = new Set((q.get('ouvert') ?? '').split(',').filter(Boolean));
    this.ouverturePar = this.ouvertes.size > 0;
  }

  /** Écrit l'état dans l'adresse sans ajouter d'entrée d'historique ; les valeurs par défaut n'y figurent pas. */
  private ecrireAdresse(): void {
    const f = this.filtres;
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        horizon: this.horizon === 'FIN_DE_TRIMESTRE' ? this.horizon : null,
        vue: this.vue === 'tableau' ? 'tableau' : null,
        devise: this.devise || null,
        banques: f.banques.length ? f.banques.join(',') : null,
        sens: f.sens !== 'TOUS' ? f.sens : null,
        sources: f.sources.length ? f.sources.join(',') : null,
        op: f.montant !== null && f.operateur !== '>=' ? f.operateur : null,
        montant: f.montant,
        q: f.texte.trim() || null,
        ouvert: this.ouvertes.size ? [...this.ouvertes].join(',') : null,
        banque: null,
        col: null,
      },
      replaceUrl: true,
    });
  }
}
