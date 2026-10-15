import { Component, EventEmitter, Input, Output } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { TooltipModule } from 'primeng/tooltip';
import { ColonnePlan, PlanBanque } from '../../dtos/PlanTresorerie';
import { dateCourte, montantCompact } from '../../format';

/** Situation d'une case : sain, facilité de caisse entamée (solde négatif couvert), ou besoin de financement. */
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

/**
 * Grille de position de trésorerie : une ligne par banque, une colonne par période, chaque case
 * donnant le solde de fin de période et sa couleur (vert, orange, rouge), puis une ligne de total par
 * devise (on n'additionne jamais des devises différentes). Une mini-courbe résume la trajectoire de chaque banque. Présentation uniquement : un
 * clic sur le nom, une case ou le total remonte la sélection à l'écran.
 */
@Component({
  selector: 'app-position-grid',
  standalone: true,
  imports: [DecimalPipe, TooltipModule],
  templateUrl: './position-grid.component.html',
  styleUrl: './position-grid.component.scss'
})
export class PositionGridComponent {

  @Input({ required: true }) colonnes: ColonnePlan[] = [];
  @Input({ required: true }) banques: PlanBanque[] = [];
  /** Un total par devise, le dirham en premier. */
  @Input({ required: true }) groupes: PlanBanque[] = [];
  /** Banque dont le panneau est ouvert (surlignée) ; `null` = un total ; `undefined` si aucun panneau. */
  @Input() selection: number | null | undefined;
  /** Devise du total ouvert (surligné quand `selection` est nul). */
  @Input() deviseSelection = '';

  @Output() choisir = new EventEmitter<CaseChoisie>();

  readonly compact = montantCompact;
  readonly date = dateCourte;

  /** Vrai quand la grille mélange plusieurs devises : le code de devise est alors affiché sur chaque banque. */
  get plusieursDevises(): boolean {
    return this.groupes.length > 1;
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
    const statut = etat === 'danger' ? 'besoin de financement' : etat === 'warn' ? 'solde négatif, couvert par la facilité de caisse' : 'sain';
    return `${plan.banqueNom}, ${c.libelle} : solde ${plan.soldesFin[colonne].toLocaleString('fr-FR')} ${plan.devise}, ${statut}`;
  }
}
