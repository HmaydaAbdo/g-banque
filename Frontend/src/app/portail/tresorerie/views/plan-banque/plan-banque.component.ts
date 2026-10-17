import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { DatePipe, DecimalPipe, NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, Subject, Subscription } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { MenuItem } from 'primeng/api';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';
import { SkeletonModule } from 'primeng/skeleton';
import { TooltipModule } from 'primeng/tooltip';
import {
  HORIZON_OPTIONS,
  HorizonPrevision,
  horizonDepuisParam,
} from '../../../etatsDesEncours/views/lesEncours/recap/dtos/PeriodeRecap';
import { RecaputilatifItem } from '../../../etatsDesEncours/views/lesEncours/recap/dtos/RecaputilatifItem';
import { RecapService } from '../../../etatsDesEncours/views/lesEncours/recap/services/recap.service';
import { TransactionSource, TransactionSourceLabels } from '../../../transactions/dtos/TransactionSource';
import { ColonnePlan, PlanBanque } from '../../dtos/PlanTresorerie';
import {
  colonneDuPointBas, FILTRES_VIDES, filtreActif, FiltresPlan, garder, montantDe, Operateur, OPERATEURS, SensFiltre,
} from '../../plan-banque';
import { EncoursNavigationService } from '../../services/encours-navigation.service';
import { TresorerieService } from '../../services/tresorerie.service';
import { RecitBanqueComponent } from '../recit-banque/recit-banque.component';
import { TableauBanqueComponent } from '../tableau-banque/tableau-banque.component';

/** Nombre maximal d'opérations demandées d'un coup (un seul appel, sans pagination). */
const TAILLE_MAX = 2000;

export type VuePlan = 'recit' | 'tableau';

/**
 * Plan de trésorerie d'une banque — page dédiée, ouverte depuis la carte de la banque (accueil) ou depuis l'écran
 * global. Deux vues séparées : le **récit** (jalons et étapes chronologiques) et le **tableau** (catégorie › source ×
 * période). Une seule barre réunit les filtres, l'horizon et l'export. Tout l'état est porté par l'adresse
 * (`?horizon=…&vue=tableau&sens=…&sources=…&op=…&montant=…&q=…&ouvert=P1,P2`) : le retour arrière retrouve la page.
 *
 * Les soldes viennent du plan du backend, calculé sur toutes les opérations : les filtres ne changent que les
 * opérations montrées.
 */
@Component({
  selector: 'app-plan-banque',
  standalone: true,
  imports: [
    NgClass, DatePipe, DecimalPipe, FormsModule, RouterLink, BreadcrumbModule, ButtonModule, DropdownModule,
    InputNumberModule, InputTextModule, MultiSelectModule, SkeletonModule, TooltipModule,
    RecitBanqueComponent, TableauBanqueComponent,
  ],
  templateUrl: './plan-banque.component.html',
  styleUrl: './plan-banque.component.scss'
})
export class PlanBanqueComponent implements OnInit, OnDestroy {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly tresorerieService = inject(TresorerieService);
  private readonly recapService = inject(RecapService);
  private readonly navigation = inject(EncoursNavigationService);
  private readonly texte$ = new Subject<string>();
  private readonly subscriptions: Subscription[] = [];
  private chargement?: Subscription;

  readonly horizonOptions = HORIZON_OPTIONS;
  readonly operateurs = OPERATEURS;
  readonly sensOptions: { label: string; value: SensFiltre }[] = [
    { label: 'Tous les sens', value: 'TOUS' },
    { label: 'Entrées', value: 'ENCAISSEMENT' },
    { label: 'Sorties', value: 'DECAISSEMENT' },
  ];

  banqueId = 0;
  horizon: HorizonPrevision = 'FIN_DE_MOIS';
  vue: VuePlan = 'recit';
  filtres: FiltresPlan = { ...FILTRES_VIDES };
  /** Saisie de recherche, appliquée après une courte pause de frappe. */
  texte = '';
  /** Codes des étapes ouvertes dans le récit. */
  ouvertes = new Set<string>();
  /** Période demandée par l'écran global (`?col=P2`), ouverte au premier chargement. */
  private colonneDemandee = '';

  plan: PlanBanque | null = null;
  colonnes: ColonnePlan[] = [];
  dateCalcul = '';
  operations: RecaputilatifItem[] = [];
  tronque = false;
  loading = false;
  erreur = false;
  exporting = false;
  items: MenuItem[] = [];

  ngOnInit(): void {
    this.subscriptions.push(
      this.texte$.pipe(debounceTime(250)).subscribe(t => this.appliquer({ texte: t })),
      this.route.paramMap.subscribe(p => {
        this.banqueId = Number(p.get('banqueId'));
        this.lireAdresse();
        this.charger();
      }),
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
    this.chargement?.unsubscribe();
  }

  // ── Barre de filtres ────────────────────────────────────────────────────

  /** Sources présentes dans les opérations de la banque, avec leur nombre, pour la liste déroulante. */
  get sourcesDisponibles(): { label: string; value: TransactionSource }[] {
    const nombres = new Map<TransactionSource, number>();
    this.operations.forEach(o => nombres.set(o.source, (nombres.get(o.source) ?? 0) + 1));
    return [...nombres.entries()]
      .map(([value, n]) => ({ value, label: `${TransactionSourceLabels[value] ?? value} (${n})` }))
      .sort((a, b) => a.label.localeCompare(b.label));
  }

  get actif(): boolean {
    return filtreActif(this.filtres);
  }

  /** Pastilles des filtres actifs, chacune retirable. */
  get pastilles(): { cle: string; libelle: string; retirer: () => void }[] {
    const p: { cle: string; libelle: string; retirer: () => void }[] = [];
    const f = this.filtres;
    if (f.sens !== 'TOUS') {
      p.push({ cle: 'sens', libelle: f.sens === 'ENCAISSEMENT' ? 'Entrées' : 'Sorties', retirer: () => this.appliquer({ sens: 'TOUS' }) });
    }
    f.sources.forEach(s => p.push({
      cle: `src-${s}`, libelle: TransactionSourceLabels[s] ?? s,
      retirer: () => this.appliquer({ sources: f.sources.filter(x => x !== s) }),
    }));
    if (f.montant !== null) {
      const op = OPERATEURS.find(o => o.value === f.operateur)?.label ?? f.operateur;
      p.push({ cle: 'montant', libelle: `Montant ${op} ${this.montantTexte(f.montant)}`, retirer: () => this.appliquer({ montant: null }) });
    }
    if (f.texte.trim()) {
      p.push({ cle: 'texte', libelle: `« ${f.texte.trim()} »`, retirer: () => { this.texte = ''; this.appliquer({ texte: '' }); } });
    }
    return p;
  }

  /** Résumé des opérations retenues par les filtres. */
  get resumeFiltre(): { nombre: number; entrees: number; sorties: number } {
    const retenues = this.operations.filter(o => garder(o, this.filtres));
    const somme = (sens: string) => retenues.filter(o => o.sens === sens).reduce((s, o) => s + montantDe(o), 0);
    return { nombre: retenues.length, entrees: somme('ENCAISSEMENT'), sorties: somme('DECAISSEMENT') };
  }

  appliquer(changement: Partial<FiltresPlan>): void {
    this.filtres = { ...this.filtres, ...changement };
    this.ecrireAdresse();
  }

  saisirTexte(valeur: string): void {
    this.texte = valeur;
    this.texte$.next(valeur);
  }

  effacer(): void {
    this.texte = '';
    this.filtres = { ...FILTRES_VIDES };
    this.ecrireAdresse();
  }

  choisirHorizon(horizon: HorizonPrevision): void {
    this.horizon = horizon;
    this.ouvertes.clear();
    this.ecrireAdresse();
    this.charger();
  }

  choisirVue(vue: VuePlan): void {
    this.vue = vue;
    this.ecrireAdresse();
  }

  // ── Récit : étapes ouvertes ─────────────────────────────────────────────

  basculer(code: string): void {
    if (!this.ouvertes.delete(code)) {
      this.ouvertes.add(code);
    }
    this.ouvertes = new Set(this.ouvertes);
    this.ecrireAdresse();
  }

  toutOuvrir(ouvrir: boolean): void {
    this.ouvertes = new Set(ouvrir ? this.colonnes.map(c => c.code) : []);
    this.ecrireAdresse();
  }

  ouvrir(code: string): void {
    if (!this.ouvertes.has(code)) {
      this.ouvertes = new Set([...this.ouvertes, code]);
      this.ecrireAdresse();
    }
  }

  /** Ouvre le Récapitulatif de la banque filtré sur la source et la période de l'opération. */
  ouvrirEncours(evenement: { operation: RecaputilatifItem | null; colonne: ColonnePlan }): void {
    const params: Record<string, string | null> = { horizon: this.horizon === 'FIN_DE_TRIMESTRE' ? this.horizon : null };
    if (evenement.operation) {
      params['source'] = evenement.operation.source;
      params['du'] = evenement.colonne.du;
      params['au'] = evenement.colonne.au;
    }
    this.navigation.ouvrirRecapitulatif(this.banqueId, params);
  }

  // ── Export ──────────────────────────────────────────────────────────────

  /** Le PDF reprend le plan complet de la banque à l'horizon choisi, sans les filtres. */
  exporterPdf(): void {
    this.exporting = true;
    this.tresorerieService.exportPdf(this.horizon, this.banqueId).subscribe({
      next: blob => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Plan_tresorerie_${this.banqueId}_${this.horizon.toLowerCase()}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.exporting = false;
      },
      error: () => {
        this.exporting = false;
      },
    });
  }

  montantTexte(v: number): string {
    return Math.abs(v).toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  // ── Chargement et adresse ───────────────────────────────────────────────

  private charger(): void {
    this.chargement?.unsubscribe();
    this.loading = true;
    this.erreur = false;
    this.chargement = forkJoin({
      plan: this.tresorerieService.getPlan(this.horizon),
      recap: this.recapService.getRecapitulatif(this.banqueId, { libelleOuObservation: '', periode: this.horizon }, 0, TAILLE_MAX, 'dateReference,asc'),
    }).subscribe({
      next: ({ plan, recap }) => {
        this.plan = plan.banques.find(b => b.banqueId === this.banqueId) ?? null;
        this.colonnes = plan.colonnes;
        this.dateCalcul = plan.dateCalcul;
        this.operations = recap.page.content;
        this.tronque = recap.page.page.totalElements > recap.page.content.length;
        this.erreur = this.plan === null;
        if (this.plan && this.ouvertes.size === 0) {
          // Ouverture par défaut : la période du point bas (ou celle demandée par l'écran global)
          const col = this.colonneDemandee;
          this.ouvertes = new Set([col && this.colonnes.some(c => c.code === col) ? col : colonneDuPointBas(this.plan, this.colonnes)]);
        }
        this.items = [
          { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
          { route: '/portail/tresorerie-previsionnelle', label: 'Trésorerie prévisionnelle' },
          { label: this.plan?.banqueNom ?? 'Banque' },
        ];
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
    const sens = q.get('sens');
    const op = q.get('op') as Operateur | null;
    const montant = Number(q.get('montant'));
    this.texte = q.get('q') ?? '';
    this.filtres = {
      sens: sens === 'ENCAISSEMENT' || sens === 'DECAISSEMENT' ? sens : 'TOUS',
      sources: (q.get('sources') ?? '').split(',').filter(Boolean) as TransactionSource[],
      operateur: OPERATEURS.some(o => o.value === op) ? op! : '>=',
      montant: q.get('montant') !== null && Number.isFinite(montant) && montant >= 0 ? montant : null,
      texte: this.texte,
    };
    this.ouvertes = new Set((q.get('ouvert') ?? '').split(',').filter(Boolean));
    this.colonneDemandee = q.get('col') ?? '';
  }

  /** Écrit l'état dans l'adresse sans ajouter d'entrée d'historique ; les valeurs par défaut n'y figurent pas. */
  private ecrireAdresse(): void {
    const f = this.filtres;
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        horizon: this.horizon === 'FIN_DE_TRIMESTRE' ? this.horizon : null,
        vue: this.vue === 'tableau' ? 'tableau' : null,
        sens: f.sens !== 'TOUS' ? f.sens : null,
        sources: f.sources.length ? f.sources.join(',') : null,
        op: f.montant !== null && f.operateur !== '>=' ? f.operateur : null,
        montant: f.montant,
        q: f.texte.trim() || null,
        ouvert: this.ouvertes.size ? [...this.ouvertes].join(',') : null,
        col: null,
      },
      replaceUrl: true,
    });
  }
}
