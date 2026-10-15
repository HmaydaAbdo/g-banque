import { Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { TooltipModule } from 'primeng/tooltip';
import { construireCroisement, LigneCroisement } from '../../croisement';
import { ColonnePlan, PlanBanque } from '../../dtos/PlanTresorerie';
import { dateCourte, montantCompact } from '../../format';
import { niveauSituation, trajectoire } from '../../situation';

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
 * Grille de position de trésorerie, regroupée **Banque › Source** : une ligne par banque avec son solde
 * de fin de période (vert, orange, rouge) et sa tendance ; dépliée, la banque montre ses flux par source
 * (entrées en vert, sorties en rouge). Les totaux par devise (on n'additionne jamais des devises
 * différentes) restent en bas. Présentation uniquement : un clic sur une banque, une case ou un total
 * remonte la sélection à l'écran.
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
    this.sections = this.groupes.map(g => ({ devise: g.devise, lignes: construireCroisement(this.banques, g.devise) }));
    this.ouvertes.clear();
  }

  /** Vrai quand la grille mélange plusieurs devises : le code de devise est alors affiché. */
  get plusieursDevises(): boolean {
    return this.groupes.length > 1;
  }

  get banquesDepliables(): LigneCroisement[] {
    return this.sections.flatMap(s => s.lignes).filter(l => l.niveau === 1 && l.enfants > 0);
  }

  get toutOuvert(): boolean {
    const parents = this.banquesDepliables;
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
      this.banquesDepliables.forEach(l => this.ouvertes.add(l.cle));
    }
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

  readonly trajectoire = trajectoire;

  libelleCase(plan: PlanBanque, colonne: number): string {
    const c = this.colonnes[colonne];
    const etat = this.etat(plan, colonne);
    const statut = etat === 'danger' ? 'besoin (facilité de caisse dépassée)' : etat === 'warn' ? 'solde négatif, couvert par la facilité de caisse' : 'solde positif';
    return `${plan.banqueNom}, ${c.libelle} : solde ${plan.soldesFin[colonne].toLocaleString('fr-FR')} ${plan.devise}, ${statut}`;
  }

  readonly niveauTendance = niveauSituation;
}
