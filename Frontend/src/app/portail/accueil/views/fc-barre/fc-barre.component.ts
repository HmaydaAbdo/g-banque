import { Component, Input } from '@angular/core';
import { NgClass } from '@angular/common';
import { montantCompact } from '../../../tresorerie/format';
import { SituationFc } from '../../fc-consommation';

/**
 * Barre de consommation de la facilité de caisse, commune aux cartes de l'accueil et au plan de trésorerie d'une
 * banque : bleue sous 70 % du plafond, orange de 70 à 100 %, rouge au-delà (barre pleine). Avec `entete`, une ligne
 * au-dessus donne le montant utilisé (ou « FC non utilisée ») et le pourcentage.
 */
@Component({
  selector: 'app-fc-barre',
  standalone: true,
  imports: [NgClass],
  template: `
    @if (entete) {
      <span class="fc__tete">
        <span>{{ situation.palier === 'aucun' ? 'FC non utilisée' : 'FC utilisée ' + texte(situation.utilise) }}</span>
        <strong [ngClass]="'fc__pct--' + situation.palier">{{ situation.pourcentage }} %</strong>
      </span>
    }
    <span class="fc__barre" role="img" [attr.aria-label]="'Facilité de caisse utilisée à ' + situation.pourcentage + ' pour cent'">
      <span class="fc__rempli" [ngClass]="'fc__rempli--' + situation.palier" [style.width.%]="situation.largeur"></span>
    </span>
  `,
  styles: [`
    :host { display: flex; flex-direction: column; gap: 4px; }
    .fc__tete { display: flex; justify-content: space-between; align-items: baseline; font-size: var(--text-xs);
      color: var(--color-text-secondary); font-variant-numeric: tabular-nums;
      strong { font-size: var(--text-sm); font-weight: var(--weight-semibold); } }
    .fc__barre { position: relative; display: block; height: var(--fc-hauteur, 6px); border-radius: var(--radius-full);
      background: var(--color-border); overflow: hidden; }
    .fc__rempli { position: absolute; inset: 0 auto 0 0; border-radius: var(--radius-full); transition: width 0.4s ease; }
    .fc__rempli--faible { background: var(--color-primary); }
    .fc__rempli--eleve { background: var(--color-warning); }
    .fc__rempli--depasse { background: var(--color-danger); }
    .fc__pct--aucun { color: var(--color-text-secondary); }
    .fc__pct--faible { color: var(--color-primary); }
    .fc__pct--eleve { color: var(--color-warning); }
    .fc__pct--depasse { color: var(--color-danger); }
  `]
})
export class FcBarreComponent {
  @Input({ required: true }) situation!: SituationFc;
  @Input() entete = true;
  /** Montant utilisé : complet (« 4 000 000,00 ») sur les cartes, compact (« 4,00 M ») dans le plan. */
  @Input() format: 'complet' | 'compact' = 'complet';

  texte(v: number): string {
    return this.format === 'compact'
      ? montantCompact(v)
      : v.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
}
