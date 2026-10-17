import { Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { NgClass } from '@angular/common';
import { RecaputilatifItem } from '../../../etatsDesEncours/views/lesEncours/recap/dtos/RecaputilatifItem';
import { TransactionSource } from '../../../transactions/dtos/TransactionSource';
import { SituationFc, situationFc } from '../../../accueil/fc-consommation';
import { FcBarreComponent } from '../../../accueil/views/fc-barre/fc-barre.component';
import { ColonnePlan, PlanBanque } from '../../dtos/PlanTresorerie';
import { montantCompact } from '../../format';
import {
  construireTableau, filtreActif, FiltresPlan, libellePeriode, LigneTableau, montantDe, operationsDeCase, TableauPlan,
} from '../../plan-banque';

/** Case du tableau dont on affiche les opérations sous le tableau. */
interface CaseChoisie {
  sens: 'ENCAISSEMENT' | 'DECAISSEMENT';
  source: TransactionSource;
  libelle: string;
  index: number;
}

/**
 * Vue « Tableau » du plan d'une banque : le plan de trésorerie classique, périodes en colonnes, puis solde
 * d'ouverture, entrées et sorties par catégorie (dépliables en sources), flux net, solde de clôture avec la
 * consommation de la facilité de caisse, et disponibilité ou besoin. Un clic sur une case de source détaille ses
 * opérations sous le tableau. Les soldes sont ceux du plan ; les lignes de flux suivent les filtres.
 */
@Component({
  selector: 'app-tableau-banque',
  standalone: true,
  imports: [NgClass, FcBarreComponent],
  templateUrl: './tableau-banque.component.html',
  styleUrl: './tableau-banque.component.scss'
})
export class TableauBanqueComponent implements OnChanges {

  @Input({ required: true }) plan!: PlanBanque;
  @Input({ required: true }) colonnes: ColonnePlan[] = [];
  @Input({ required: true }) operations: RecaputilatifItem[] = [];
  @Input({ required: true }) filtres!: FiltresPlan;

  @Output() ouvrirEncours = new EventEmitter<{ operation: RecaputilatifItem | null; colonne: ColonnePlan }>();

  tableau: TableauPlan = { entrees: [], sorties: [], totalEntrees: [], totalSorties: [] };
  /** Catégories repliées (clé `sens|catégorie`) et sens repliés. */
  repliees = new Set<string>();
  choix: CaseChoisie | null = null;

  readonly compact = montantCompact;

  ngOnChanges(): void {
    if (!this.plan) {
      return;
    }
    this.tableau = construireTableau(this.plan, this.colonnes, this.operations, this.filtres);
    const toutes = [...this.tableau.entrees, ...this.tableau.sorties];
    if (this.choix && !toutes.some(l => l.niveau === 2 && l.sens === this.choix!.sens && l.source === this.choix!.source)) {
      this.choix = null;
    }
  }

  /** Les deux blocs du tableau, entrées puis sorties. */
  get groupes(): { sens: 'ENCAISSEMENT' | 'DECAISSEMENT'; titre: string; lignes: LigneTableau[]; totaux: number[] }[] {
    return [
      { sens: 'ENCAISSEMENT', titre: 'Entrées', lignes: this.tableau.entrees, totaux: this.tableau.totalEntrees },
      { sens: 'DECAISSEMENT', titre: 'Sorties', lignes: this.tableau.sorties, totaux: this.tableau.totalSorties },
    ];
  }

  get filtre(): boolean {
    return filtreActif(this.filtres);
  }

  get ouvertures(): number[] {
    return this.colonnes.map((_, i) => (i === 0 ? this.plan.soldeActuel : this.plan.soldesFin[i - 1]));
  }

  get fluxNets(): number[] {
    return this.colonnes.map((_, i) => this.plan.totalEncaissements[i] - this.plan.totalDecaissements[i]);
  }

  fc(solde: number): SituationFc {
    return situationFc(solde, this.plan.plafondFC);
  }

  visible(ligne: LigneTableau): boolean {
    return ligne.niveau === 1 ? !this.repliees.has(ligne.sens) : !this.repliees.has(ligne.sens) && !this.repliees.has(`${ligne.sens}|${ligne.categorie}`);
  }

  basculer(cle: string): void {
    if (!this.repliees.delete(cle)) {
      this.repliees.add(cle);
    }
  }

  choisir(ligne: LigneTableau, index: number): void {
    if (!ligne.source || ligne.montants[index] === 0) {
      return;
    }
    this.choix = { sens: ligne.sens, source: ligne.source, libelle: ligne.libelle, index };
  }

  estChoisie(ligne: LigneTableau, index: number): boolean {
    return !!this.choix && this.choix.sens === ligne.sens && this.choix.source === ligne.source && this.choix.index === index;
  }

  get operationsChoisies(): RecaputilatifItem[] {
    return this.choix ? operationsDeCase(this.operations, this.colonnes, this.filtres, this.choix.sens, this.choix.source, this.choix.index) : [];
  }

  get totalChoisi(): number {
    return this.operationsChoisies.reduce((s, o) => s + montantDe(o), 0);
  }

  /** Part d'une source dans toutes les sorties de l'horizon (barre fine du tableau). */
  part(ligne: LigneTableau): number {
    const total = this.tableau.totalSorties.reduce((s, v) => s + v, 0);
    return total > 0 ? Math.round((ligne.total / total) * 100) : 0;
  }

  montantDe = montantDe;

  montant(v: number): string {
    return Math.abs(v).toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  /** Montant compact d'une case ; « — » pour zéro. */
  cellule(v: number): string {
    return v === 0 ? '—' : montantCompact(Math.abs(v));
  }

  signe(v: number): string {
    const a = Math.round(v * 100) / 100;
    return a === 0 ? '—' : (a > 0 ? '+' : '') + montantCompact(a);
  }

  somme(valeurs: number[]): number {
    return valeurs.reduce((s, v) => s + v, 0);
  }

  jourMois(iso: string): string {
    return iso ? `${iso.slice(8, 10)}/${iso.slice(5, 7)}` : '—';
  }

  libelleColonne(c: ColonnePlan, i: number): string {
    return libellePeriode(c, i).titre;
  }

  sousTitreColonne(c: ColonnePlan, i: number): string {
    return libellePeriode(c, i).sous;
  }

  /** Une catégorie se déplie seulement si elle regroupe plusieurs sources. */
  aDesSources(ligne: LigneTableau): boolean {
    return ligne.niveau === 1 && !ligne.source;
  }
}
