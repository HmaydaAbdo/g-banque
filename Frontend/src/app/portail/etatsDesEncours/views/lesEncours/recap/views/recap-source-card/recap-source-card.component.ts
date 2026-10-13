import { Component, EventEmitter, Input, Output } from '@angular/core';
import { DecimalPipe, NgClass } from '@angular/common';
import { TooltipModule } from 'primeng/tooltip';
import { TransactionSource } from '../../../../../../transactions/dtos/TransactionSource';

/** Données d'affichage d'une carte du groupement par source. */
export interface RecapCardView {
  key: TransactionSource;
  label: string;
  icon: string;
  count: number;
  debit: number;
  credit: number;
  echues: number;
  /** Part du débit dans (débit + crédit), en % — alimente la barre de répartition de la carte. */
  debitPct: number;
}

/**
 * Carte d'une source dans la vue d'ensemble du Récapitulatif : nombre d'opérations, débit, crédit,
 * répartition et lien « échues ». Composant de présentation : l'ouverture de la source est remontée
 * au parent (`ouvrir`, `true` = ouvrir la source déjà filtrée sur ses opérations échues).
 */
@Component({
  selector: 'app-recap-source-card',
  standalone: true,
  imports: [NgClass, DecimalPipe, TooltipModule],
  templateUrl: './recap-source-card.component.html',
  styleUrl: './recap-source-card.component.scss',
})
export class RecapSourceCardComponent {
  @Input({ required: true }) card!: RecapCardView;
  @Output() ouvrir = new EventEmitter<boolean>();

  /** Ouverture au clavier (Entrée / Espace) uniquement quand le focus est sur la carte elle-même. */
  onKey(event: Event): void {
    if (event.target !== event.currentTarget) {
      return;
    }
    event.preventDefault();
    this.ouvrir.emit(false);
  }
}
