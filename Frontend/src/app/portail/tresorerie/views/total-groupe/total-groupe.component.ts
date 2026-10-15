import { Component, Input } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { TooltipModule } from 'primeng/tooltip';
import { PlanBanque } from '../../dtos/PlanTresorerie';
import { NiveauSituation, niveauSituation } from '../../situation';

/**
 * Total groupe : en tête de l'écran, les indicateurs du total de la devise choisie (solde actuel, solde prévu,
 * solde le plus bas, besoin ou disponibilité, jours à solde négatif) et le nombre de banques par situation.
 * Présentation uniquement.
 */
@Component({
  selector: 'app-total-groupe',
  standalone: true,
  imports: [DatePipe, DecimalPipe, TooltipModule],
  templateUrl: './total-groupe.component.html',
  styleUrl: './total-groupe.component.scss'
})
export class TotalGroupeComponent {

  /** Total dont on affiche les indicateurs (celui de la devise choisie). */
  @Input({ required: true }) total!: PlanBanque;
  /** Nombre de banques par situation, dans la devise du total. */
  @Input({ required: true }) compte!: Record<NiveauSituation, number>;
  /** Dates du calcul et de l'horizon (yyyy-MM-dd). */
  @Input({ required: true }) dateCalcul = '';
  @Input({ required: true }) dateHorizon = '';
  /** Vrai quand plusieurs devises existent : la devise est alors rappelée dans le titre. */
  @Input() plusieursDevises = false;

  /** Écart entre le solde prévu à l'horizon et le solde actuel. */
  get variation(): number {
    return this.total.soldeFinal - this.total.soldeActuel;
  }

  get enBesoin(): boolean {
    return this.total.besoinFinancement > 0;
  }

  get niveau(): NiveauSituation {
    return niveauSituation(this.total);
  }
}
