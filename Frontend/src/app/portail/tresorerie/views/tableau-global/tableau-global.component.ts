import { Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { NgClass } from '@angular/common';
import { RecaputilatifItem } from '../../../etatsDesEncours/views/lesEncours/recap/dtos/RecaputilatifItem';
import { situationFc } from '../../../accueil/fc-consommation';
import { FcBarreComponent } from '../../../accueil/views/fc-barre/fc-barre.component';
import { ColonnePlan, PlanBanque } from '../../dtos/PlanTresorerie';
import { montantCompact } from '../../format';
import { FiltresPlan, filtreActif, libellePeriode } from '../../plan-banque';
import { construireTableauGlobal, LigneTableauGlobal } from '../../plan-global';

/**
 * Vue « Tableau » de la trésorerie de toutes les banques : une ligne par banque, une colonne par période, le solde de
 * fin de période coloré selon son niveau avec la consommation de la facilité de caisse. Une banque se déplie en entrées
 * et sorties ; un clic sur une case ouvre le plan de la banque sur cette période. La dernière ligne est le total du
 * groupe (les devises ne sont jamais additionnées : la page ne montre qu'une devise à la fois).
 */
@Component({
  selector: 'app-tableau-global',
  standalone: true,
  imports: [NgClass, FcBarreComponent],
  templateUrl: './tableau-global.component.html',
  styleUrl: './tableau-global.component.scss'
})
export class TableauGlobalComponent implements OnChanges {

  @Input({ required: true }) banques: PlanBanque[] = [];
  @Input({ required: true }) colonnes: ColonnePlan[] = [];
  @Input({ required: true }) operations: RecaputilatifItem[] = [];
  @Input({ required: true }) filtres!: FiltresPlan;

  @Output() ouvrirBanque = new EventEmitter<{ banqueId: number; colonne: string }>();

  lignes: LigneTableauGlobal[] = [];
  depliees = new Set<number>();
  readonly compact = montantCompact;

  ngOnChanges(): void {
    this.lignes = construireTableauGlobal(this.banques, this.colonnes, this.operations, this.filtres);
  }

  get filtre(): boolean {
    return filtreActif(this.filtres);
  }

  /** Total du groupe par période : somme des soldes de fin de période des banques affichées. */
  get totaux(): number[] {
    return this.colonnes.map((_, k) => this.lignes.reduce((s, l) => s + l.soldes[k], 0));
  }

  get soldeActuel(): number {
    return this.banques.reduce((s, b) => s + b.soldeActuel, 0);
  }

  basculer(id: number): void {
    if (!this.depliees.delete(id)) {
      this.depliees.add(id);
    }
  }

  toutDeplier(deplier: boolean): void {
    this.depliees = new Set(deplier ? this.banques.map(b => b.banqueId!) : []);
  }

  get toutesDepliees(): boolean {
    return this.banques.length > 0 && this.banques.every(b => this.depliees.has(b.banqueId!));
  }

  fc(solde: number, plafond: number) {
    return situationFc(solde, plafond);
  }

  titre(c: ColonnePlan, i: number): string {
    return libellePeriode(c, i).titre;
  }

  sous(c: ColonnePlan, i: number): string {
    return libellePeriode(c, i).sous;
  }

  cellule(v: number): string {
    return v === 0 ? '—' : montantCompact(Math.abs(v));
  }
}
