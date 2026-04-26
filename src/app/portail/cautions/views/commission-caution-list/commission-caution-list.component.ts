import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Liste opérationnelle des commissions sur cautions.
 * Route : /portail/cautions/commissions
 *
 * @todo Implémenter la liste paginée avec filtres (banque, statut, mois).
 */
@Component({
  selector: 'app-commission-caution-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  template: `
    <div class="p-4">
      <h2 class="text-xl font-semibold mb-3">Commissions sur cautions</h2>
      <p class="text-color-secondary">Page en cours de construction.</p>
    </div>
  `,
})
export class CommissionCautionListComponent {}
