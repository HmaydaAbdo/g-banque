import { AfterViewChecked, Component, ElementRef, EventEmitter, inject, Input, OnChanges, Output } from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import { RecaputilatifItem } from '../../../etatsDesEncours/views/lesEncours/recap/dtos/RecaputilatifItem';
import { BlocSens } from '../../../accueil/encours-timeline';
import { ColonnePlan, PlanBanque } from '../../dtos/PlanTresorerie';
import { montantCompact } from '../../format';
import { SituationFc, situationFc } from '../../../accueil/fc-consommation';
import { FcBarreComponent } from '../../../accueil/views/fc-barre/fc-barre.component';
import {
  construireEtapes, construireJalons, EtapeRecit, filtreActif, FiltresPlan, Jalon, libellePeriode,
} from '../../plan-banque';

/** Un élément de la frise : un jalon (losange) ou une étape (une colonne du plan). */
type ElementFrise = { type: 'jalon'; jalon: Jalon } | { type: 'etape'; etape: EtapeRecit };

/**
 * Vue « Récit » du plan d'une banque : une phrase qui résume le mois, puis une frise où alternent les étapes (une par
 * période du plan : solde d'ouverture → clôture, repère de niveau, pastilles par source) et les jalons datés
 * (aujourd'hui, passage sous zéro, dépassement de la facilité de caisse, point bas). Une étape s'ouvre pour détailler
 * ses opérations par sens puis par source ; un jalon ouvre son étape et surligne l'opération qui le provoque.
 */
@Component({
  selector: 'app-recit-banque',
  standalone: true,
  imports: [NgClass, DatePipe, FcBarreComponent],
  templateUrl: './recit-banque.component.html',
  styleUrl: './recit-banque.component.scss'
})
export class RecitBanqueComponent implements OnChanges, AfterViewChecked {

  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef);

  @Input({ required: true }) plan!: PlanBanque;
  @Input({ required: true }) colonnes: ColonnePlan[] = [];
  @Input({ required: true }) operations: RecaputilatifItem[] = [];
  @Input({ required: true }) filtres!: FiltresPlan;
  @Input({ required: true }) ouvertes = new Set<string>();

  @Output() basculer = new EventEmitter<string>();
  @Output() toutOuvrir = new EventEmitter<boolean>();
  @Output() ouvrir = new EventEmitter<string>();
  @Output() ouvrirEncours = new EventEmitter<{ operation: RecaputilatifItem | null; colonne: ColonnePlan }>();

  frise: ElementFrise[] = [];
  jalons: Jalon[] = [];
  /** Opérations surlignées après un clic sur un jalon. */
  surlignees = new Set<string>();
  private aDefiler: string | null = null;
  private echelle = { min: 0, max: 1 };

  readonly compact = montantCompact;

  ngOnChanges(): void {
    if (!this.plan) {
      return;
    }
    this.jalons = construireJalons(this.plan, this.colonnes, this.operations);
    const etapes = construireEtapes(this.plan, this.colonnes, this.operations, this.filtres);
    this.frise = [];
    etapes.forEach(etape => {
      // Les jalons d'aujourd'hui ouvrent la frise ; les autres suivent l'étape qui les contient
      if (etape.index === 0) {
        this.jalons.filter(j => j.code === 'AUJOURDHUI').forEach(jalon => this.frise.push({ type: 'jalon', jalon }));
      }
      this.frise.push({ type: 'etape', etape });
      this.jalons.filter(j => j.code !== 'AUJOURDHUI' && j.colonne === etape.index).forEach(jalon => this.frise.push({ type: 'jalon', jalon }));
    });
    const soldes = [this.plan.soldeActuel, ...this.plan.soldesFin, -Math.max(this.plan.plafondFC, 0), 0];
    const min = Math.min(...soldes);
    const max = Math.max(...soldes);
    const marge = (max - min) * 0.08 || 1;
    this.echelle = { min: min - marge, max: max + marge };
  }

  ngAfterViewChecked(): void {
    if (this.aDefiler) {
      const cible = this.element.nativeElement.querySelector(`#etape-${this.aDefiler}`);
      cible?.scrollIntoView?.({ behavior: 'smooth', block: 'center' });
      this.aDefiler = null;
    }
  }

  /** Dernier jour du plan (fin du mois ou du trimestre). */
  get dateHorizon(): string {
    return this.colonnes.length ? this.colonnes[this.colonnes.length - 1].au : '';
  }

  get filtre(): boolean {
    return filtreActif(this.filtres);
  }

  get toutesOuvertes(): boolean {
    return this.colonnes.length > 0 && this.colonnes.every(c => this.ouvertes.has(c.code));
  }

  get nombreSorties(): number {
    return this.operations.filter(o => o.sens === 'DECAISSEMENT').length;
  }

  get nombreEntrees(): number {
    return this.operations.filter(o => o.sens === 'ENCAISSEMENT').length;
  }

  get totalEntrees(): number {
    return this.plan.totalEncaissements.reduce((s, v) => s + v, 0);
  }

  get totalSorties(): number {
    return this.plan.totalDecaissements.reduce((s, v) => s + v, 0);
  }

  /** Consommation de la facilité de caisse pour un solde (mêmes paliers que les cartes de l'accueil : 70 % / 100 %). */
  fc(solde: number): SituationFc {
    return situationFc(solde, this.plan.plafondFC);
  }

  jalon(code: Jalon['code']): Jalon | undefined {
    return this.jalons.find(j => j.code === code);
  }

  /** Une étape est ouverte si on l'a ouverte, ou si un filtre actif y retient des opérations. */
  estOuverte(etape: EtapeRecit): boolean {
    return this.ouvertes.has(etape.colonne.code) || (this.filtre && etape.nombreFiltrees > 0);
  }

  /** Clic sur un jalon : ouvre son étape, la fait défiler à l'écran et surligne sa cause. */
  allerAuJalon(jalon: Jalon): void {
    const colonne = this.colonnes[jalon.colonne];
    if (!colonne) {
      return;
    }
    this.surlignees = new Set(jalon.causes);
    this.ouvrir.emit(colonne.code);
    this.aDefiler = colonne.code;
  }

  /** Position d'un solde sur la règle de niveau, en pourcentage. */
  position(solde: number): number {
    const { min, max } = this.echelle;
    return Math.max(0, Math.min(100, ((solde - min) / (max - min)) * 100));
  }

  /** Zones de la règle : au-delà du plafond, couvert par la facilité de caisse, positif. */
  get zones(): { depasse: number; couvert: number; zero: number } {
    const zero = this.position(0);
    const plafond = this.position(-Math.max(this.plan.plafondFC, 0));
    return { depasse: plafond, couvert: zero - plafond, zero };
  }

  blocs(etape: EtapeRecit): { sens: 'ENTREES' | 'SORTIES'; bloc: BlocSens }[] {
    const b: { sens: 'ENTREES' | 'SORTIES'; bloc: BlocSens }[] = [];
    if (etape.entreesFiltrees) {
      b.push({ sens: 'ENTREES', bloc: etape.entreesFiltrees });
    }
    if (etape.sortiesFiltrees) {
      b.push({ sens: 'SORTIES', bloc: etape.sortiesFiltrees });
    }
    return b;
  }

  /** Retrouve la ligne du récapitulatif d'une opération affichée, pour ouvrir les encours sur sa source. */
  ligneDe(cle: string): RecaputilatifItem | null {
    return this.operations.find(o => `${o.source}:${o.id}` === cle) ?? null;
  }

  montant(v: number): string {
    return Math.abs(v).toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  /** Montant compact signé (« +1,2 M », « −450 k ») ; zéro sans signe. */
  signe(v: number): string {
    const arrondi = Math.round(v * 100) / 100;
    return (arrondi > 0 ? '+' : '') + montantCompact(arrondi);
  }

  jourMois(iso: string): string {
    return iso ? `${iso.slice(8, 10)}/${iso.slice(5, 7)}` : '—';
  }

  libelleEtape(etape: EtapeRecit): string {
    return libellePeriode(etape.colonne, etape.index).titre;
  }

  sousTitreEtape(etape: EtapeRecit): string {
    return etape.index === 0 ? 'échues et du jour, comptées dès aujourd\'hui' : libellePeriode(etape.colonne, etape.index).sous;
  }

  /** Le jalon tombe-t-il aujourd'hui (échues comprises) ? */
  aujourdhui(date: string): boolean {
    return !!this.colonnes.length && date === this.colonnes[0].au;
  }
}
