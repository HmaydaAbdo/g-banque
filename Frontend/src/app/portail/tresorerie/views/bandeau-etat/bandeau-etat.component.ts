import { Component, Input } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { TooltipModule } from 'primeng/tooltip';
import { PlanBanque } from '../../dtos/PlanTresorerie';
import { Verdict } from '../../verdict';

/**
 * Bandeau d'état : la réponse à « est-ce que ça va ? » en une phrase colorée, et juste dessous les
 * indicateurs du total affiché (solde actuel, solde prévu, solde le plus bas, besoin ou disponibilité,
 * jours à solde négatif). Présentation uniquement.
 */
@Component({
  selector: 'app-bandeau-etat',
  standalone: true,
  imports: [DatePipe, DecimalPipe, TooltipModule],
  templateUrl: './bandeau-etat.component.html',
  styleUrl: './bandeau-etat.component.scss'
})
export class BandeauEtatComponent {

  @Input({ required: true }) verdict!: Verdict;
  /** Total dont on affiche les indicateurs (celui de la devise choisie). */
  @Input({ required: true }) total!: PlanBanque;
  /** Dates du calcul et de l'horizon (yyyy-MM-dd). */
  @Input({ required: true }) dateCalcul = '';
  @Input({ required: true }) dateHorizon = '';
  /** Vrai quand plusieurs devises existent : la devise est alors rappelée dans les libellés. */
  @Input() plusieursDevises = false;

  /** Écart entre le solde prévu à l'horizon et le solde actuel. */
  get variation(): number {
    return this.total.soldeFinal - this.total.soldeActuel;
  }

  get enBesoin(): boolean {
    return this.total.besoinFinancement > 0;
  }
}
