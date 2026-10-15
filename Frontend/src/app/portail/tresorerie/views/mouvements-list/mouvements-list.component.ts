import { Component, Input } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { Mouvement } from '../../dtos/PlanTresorerie';

/**
 * Les plus gros flux à venir : date, catégorie, famille et montant signé (entrée en vert, sortie en
 * rouge). Un flux tombant le jour du point bas est signalé comme sa cause probable.
 */
@Component({
  selector: 'app-mouvements-list',
  standalone: true,
  imports: [DatePipe, DecimalPipe],
  template: `
    @if (mouvements.length === 0) {
      <p class="vide">Aucun mouvement prévu sur la période.</p>
    } @else {
      <ol class="mouvements">
        @for (m of mouvements; track $index) {
          <li class="mouvement">
            <i class="pi" [class.pi-arrow-down-left]="m.sens === 'ENCAISSEMENT'" [class.pi-arrow-up-right]="m.sens === 'DECAISSEMENT'"
               [class.entree]="m.sens === 'ENCAISSEMENT'" [class.sortie]="m.sens === 'DECAISSEMENT'" aria-hidden="true"></i>
            <span class="mouvement__corps">
              <span class="mouvement__cat">{{ m.categorie }}
                @if (m.date === dateCause && m.sens === 'DECAISSEMENT') { <span class="cause">cause du point bas</span> }
                @if (!m.certain) { <span class="estime">estimé</span> }
              </span>
              <span class="mouvement__sub">{{ m.famille }} · {{ m.date | date:'EEE dd/MM' }}</span>
            </span>
            <span class="mouvement__montant" [class.entree]="m.sens === 'ENCAISSEMENT'" [class.sortie]="m.sens === 'DECAISSEMENT'">
              {{ m.sens === 'ENCAISSEMENT' ? '+' : '−' }}{{ m.montant | number:'1.2-2' }}
            </span>
          </li>
        }
      </ol>
    }`,
  styles: [`
    .mouvements { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: var(--space-2); }
    .mouvement { display: flex; align-items: center; gap: var(--space-3); padding: var(--space-2) var(--space-3);
      background: var(--color-surface-2); border-radius: var(--radius-md); font-size: var(--text-sm); }
    .mouvement__corps { display: flex; flex-direction: column; flex: 1; min-width: 0; }
    .mouvement__cat { font-weight: var(--weight-semibold); display: flex; align-items: center; gap: var(--space-2); flex-wrap: wrap; }
    .mouvement__sub { font-size: var(--text-xs); color: var(--color-text-muted); }
    .mouvement__montant { font-weight: var(--weight-bold); font-variant-numeric: tabular-nums; white-space: nowrap; }
    .entree { color: var(--color-success); }
    .sortie { color: var(--color-danger); }
    .cause { font-size: var(--text-2xs); font-weight: var(--weight-semibold); padding: 1px var(--space-2);
      color: var(--color-danger-text); background: var(--color-danger-light); border-radius: var(--radius-full); }
    .estime { font-size: var(--text-2xs); font-style: italic; padding: 1px var(--space-2);
      color: var(--color-warning-text); background: var(--color-warning-light); border-radius: var(--radius-full); }
    .vide { margin: 0; font-size: var(--text-sm); color: var(--color-text-muted); }
  `]
})
export class MouvementsListComponent {
  @Input({ required: true }) mouvements: Mouvement[] = [];
  /** Date du point bas (yyyy-MM-dd) : les sorties de ce jour sont signalées comme sa cause. */
  @Input() dateCause = '';
}
