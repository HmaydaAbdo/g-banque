import { Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { TooltipModule } from 'primeng/tooltip';
import { construireCroisement, LigneCroisement, OrdreCroisement } from '../../croisement';
import { ColonnePlan, PlanBanque } from '../../dtos/PlanTresorerie';
import { dateCourte, montantCompact } from '../../format';

/** Situation d'une case : sain, facilité de caisse entamée (solde négatif couvert), ou besoin. */
export type EtatCase = 'ok' | 'warn' | 'danger';

/**
 * Case de la grille : choix d'une banque et d'une période. `banqueId` nul = le total d'une devise
 * (`devise` donne laquelle).
 */
export interface CaseChoisie {
  banqueId: number | null;
  colonne: string;
  devise: string;
}

/** Les lignes d'une devise ; plusieurs sections seulement si les banques n'ont pas toutes la même devise. */
interface Section {
  devise: string;
  lignes: LigneCroisement[];
}

/**
 * Grille de position de trésorerie — **une seule vue** pour lire par banque, par source, ou les deux.
 * L'ordre de regroupement choisit l'axe principal ; chaque ligne se déplie sur l'autre axe :
 * - **Banque › Source** : une ligne par banque avec son solde de fin de période (vert, orange, rouge) ;
 *   dépliée, ses flux par source (entrées en vert, sorties en rouge) ;
 * - **Source › Banque** : une ligne par source avec son flux net ; dépliée, ses banques.
 * Les totaux par devise (on n'additionne jamais des devises différentes) restent en bas dans les deux
 * cas. Présentation uniquement : un clic sur une banque, une case ou un total remonte la sélection.
 */
@Component({
  selector: 'app-position-grid',
  standalone: true,
  imports: [DecimalPipe, TooltipModule],
  templateUrl: './position-grid.component.html',
  styleUrl: './position-grid.component.scss'
})
export class PositionGridComponent implements OnChanges {

  @Input({ required: true }) colonnes: ColonnePlan[] = [];
  @Input({ required: true }) banques: PlanBanque[] = [];
  /** Un total par devise, le dirham en premier. */
  @Input({ required: true }) groupes: PlanBanque[] = [];
  @Input() ordre: OrdreCroisement = 'BANQUE_SOURCE';
  /** Banque dont le panneau est ouvert (surlignée) ; `null` = un total ; `undefined` si aucun panneau. */
  @Input() selection: number | null | undefined;
  /** Devise du total ouvert (surligné quand `selection` est nul). */
  @Input() deviseSelection = '';

  @Output() choisir = new EventEmitter<CaseChoisie>();

  sections: Section[] = [];
  readonly compact = montantCompact;
  readonly date = dateCourte;
  private readonly ouvertes = new Set<string>();

  ngOnChanges(): void {
    this.sections = this.groupes.map(g => ({ devise: g.devise, lignes: construireCroisement(this.banques, g.devise, this.ordre) }));
    this.ouvertes.clear();
  }

  /** Vrai quand la grille mélange plusieurs devises : le code de devise est alors affiché. */
  get plusieursDevises(): boolean {
    return this.groupes.length > 1;
  }

  get parentsDepliables(): LigneCroisement[] {
    return this.sections.flatMap(s => s.lignes).filter(l => l.niveau === 1 && l.enfants > 0);
  }

  get toutOuvert(): boolean {
    const parents = this.parentsDepliables;
    return parents.length > 0 && parents.every(l => this.ouvertes.has(l.cle));
  }

  visible(l: LigneCroisement): boolean {
    return l.niveau === 1 || this.ouvertes.has(l.parent!);
  }

  ouverte(cle: string): boolean {
    return this.ouvertes.has(cle);
  }

  basculer(cle: string): void {
    if (!this.ouvertes.delete(cle)) {
      this.ouvertes.add(cle);
    }
  }

  /** Tout déplier, ou tout replier si tout est déjà ouvert. */
  basculerTout(): void {
    const ouvrir = !this.toutOuvert;
    this.ouvertes.clear();
    if (ouvrir) {
      this.parentsDepliables.forEach(l => this.ouvertes.add(l.cle));
    }
  }

  /** Une ligne affiche le solde coloré (et non un flux) quand c'est une banque au premier niveau, ordre Banque › Source. */
  estSolde(l: LigneCroisement): boolean {
    return this.ordre === 'BANQUE_SOURCE' && l.niveau === 1;
  }

  /** Plan d'une ligne de banque (pour ses soldes, sa trajectoire, sa devise). */
  planDe(l: LigneCroisement): PlanBanque | undefined {
    return this.banques.find(b => b.banqueId === l.banqueId);
  }

  etat(plan: PlanBanque, colonne: number): EtatCase {
    if (plan.disponibilites[colonne] < 0) {
      return 'danger';
    }
    return plan.soldesFin[colonne] < 0 ? 'warn' : 'ok';
  }

  /** Mini-courbe du solde jour par jour, tracée dans une boîte de 80 × 24. */
  trajectoire(plan: PlanBanque): { points: string; zero: number | null } {
    const valeurs = plan.courbe.map(p => p.solde);
    if (valeurs.length < 2) {
      return { points: '', zero: null };
    }
    const min = Math.min(...valeurs);
    const max = Math.max(...valeurs);
    const echelle = max === min ? 1 : max - min;
    const y = (v: number) => 22 - ((v - min) / echelle) * 20;
    const points = valeurs.map((v, i) => `${((i / (valeurs.length - 1)) * 80).toFixed(1)},${y(v).toFixed(1)}`).join(' ');
    return { points, zero: min < 0 && max > 0 ? y(0) : null };
  }

  libelleCase(plan: PlanBanque, colonne: number): string {
    const c = this.colonnes[colonne];
    const etat = this.etat(plan, colonne);
    const statut = etat === 'danger' ? 'besoin (facilité de caisse dépassée)' : etat === 'warn' ? 'solde négatif, couvert par la facilité de caisse' : 'solde positif';
    return `${plan.banqueNom}, ${c.libelle} : solde ${plan.soldesFin[colonne].toLocaleString('fr-FR')} ${plan.devise}, ${statut}`;
  }

  niveauTendance(plan: PlanBanque): 'ok' | 'warn' | 'danger' {
    return plan.besoinFinancement > 0 ? 'danger' : plan.joursSoldeNegatif > 0 ? 'warn' : 'ok';
  }
}
