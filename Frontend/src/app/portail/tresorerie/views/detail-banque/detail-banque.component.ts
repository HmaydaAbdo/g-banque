import { AfterViewInit, Component, ElementRef, EventEmitter, inject, Input, Output } from '@angular/core';
import { DatePipe, DecimalPipe, NgClass } from '@angular/common';
import { TooltipModule } from 'primeng/tooltip';
import { ColonnePlan, PlanBanque } from '../../dtos/PlanTresorerie';
import { PlanTresorerieTableComponent } from '../plan-tresorerie-table/plan-tresorerie-table.component';
import { MouvementsListComponent } from '../mouvements-list/mouvements-list.component';

/**
 * Détail du total d'une devise (toutes les banques), déplié sous les cartes de l'écran global : indicateurs, plus
 * gros mouvements et flux par famille. Une banque, elle, a sa page dédiée (`PlanBanqueComponent`).
 */
@Component({
  selector: 'app-detail-banque',
  standalone: true,
  imports: [DatePipe, DecimalPipe, NgClass, TooltipModule, PlanTresorerieTableComponent, MouvementsListComponent],
  templateUrl: './detail-banque.component.html',
  styleUrl: './detail-banque.component.scss'
})
export class DetailBanqueComponent implements AfterViewInit {

  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef);

  @Input({ required: true }) plan!: PlanBanque;
  @Input({ required: true }) colonnes: ColonnePlan[] = [];

  @Output() fermer = new EventEmitter<void>();

  /** Dernier jour du plan (fin du mois ou du trimestre). */
  get dateHorizon(): string {
    return this.colonnes[this.colonnes.length - 1]?.au ?? '';
  }

  get statut(): 'ok' | 'warn' | 'danger' {
    if (this.plan.besoinFinancement > 0) {
      return 'danger';
    }
    return this.plan.joursSoldeNegatif > 0 ? 'warn' : 'ok';
  }

  /** Le détail s'ouvre sous les cartes : on l'amène à l'écran. */
  ngAfterViewInit(): void {
    this.element.nativeElement.scrollIntoView?.({ block: 'nearest', behavior: 'smooth' });
  }
}
