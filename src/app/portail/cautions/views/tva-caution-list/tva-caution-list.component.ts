import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Liste opérationnelle des TVA sur commissions cautions.
 * Route : /portail/cautions/tva
 *
 * @todo Implémenter la liste paginée avec filtres (banque, statut, mois).
 */
@Component({
  selector: 'app-tva-caution-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  template: `
    <div class="p-4">
      <h2 class="text-xl font-semibold mb-3">TVA sur cautions</h2>
      <p class="text-color-secondary">Page en cours de construction.</p>
    </div>
  `,
})
export class TvaCautionListComponent {}
