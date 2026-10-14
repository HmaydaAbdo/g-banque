import { Component, Input, OnChanges } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { TooltipModule } from 'primeng/tooltip';
import { ColonnePlan, PlanBanque } from '../../dtos/PlanTresorerie';

/** Ligne affichée du plan : solde, en-tête de section, catégorie ou total. */
export interface LigneAffichee {
  libelle: string;
  type: 'solde' | 'section' | 'categorie' | 'total';
  valeurs: number[];
  total: number;
}

/**
 * Tableau du plan de trésorerie : une colonne par période (« À ce jour », puis semaines ou mois),
 * une ligne par catégorie, encadrées par le solde de début, les totaux, le solde de fin et la
 * disponibilité (solde + facilité de caisse). Présentation uniquement.
 */
@Component({
  selector: 'app-plan-tresorerie-table',
  standalone: true,
  imports: [DecimalPipe, DatePipe, TooltipModule],
  templateUrl: './plan-tresorerie-table.component.html',
  styleUrl: './plan-tresorerie-table.component.scss'
})
export class PlanTresorerieTableComponent implements OnChanges {

  @Input({ required: true }) colonnes: ColonnePlan[] = [];
  @Input({ required: true }) plan!: PlanBanque;

  lignes: LigneAffichee[] = [];

  ngOnChanges(): void {
    this.lignes = this.plan ? construireLignes(this.plan) : [];
  }
}

/** Lignes du tableau, dans l'ordre de lecture d'un plan de trésorerie. */
export function construireLignes(plan: PlanBanque): LigneAffichee[] {
  const n = plan.soldesFin.length;
  const somme = (valeurs: number[]) => valeurs.reduce((a, b) => a + b, 0);
  const soldesDebut = [plan.soldeActuel, ...plan.soldesFin.slice(0, n - 1)];

  return [
    { libelle: 'Solde de début', type: 'solde', valeurs: soldesDebut, total: plan.soldeActuel },
    { libelle: 'Encaissements', type: 'section', valeurs: [], total: 0 },
    ...plan.encaissements.map(l => ({ libelle: l.categorie, type: 'categorie' as const, valeurs: l.montants, total: l.total })),
    { libelle: 'Total encaissements', type: 'total', valeurs: plan.totalEncaissements, total: somme(plan.totalEncaissements) },
    { libelle: 'Décaissements', type: 'section', valeurs: [], total: 0 },
    ...plan.decaissements.map(l => ({ libelle: l.categorie, type: 'categorie' as const, valeurs: l.montants, total: l.total })),
    { libelle: 'Total décaissements', type: 'total', valeurs: plan.totalDecaissements, total: somme(plan.totalDecaissements) },
    { libelle: 'Solde de fin', type: 'solde', valeurs: plan.soldesFin, total: plan.soldeFinal },
    { libelle: 'Facilité de caisse', type: 'categorie', valeurs: Array(n).fill(plan.plafondFC), total: plan.plafondFC },
    { libelle: 'Disponibilité', type: 'solde', valeurs: plan.disponibilites, total: plan.disponibiliteFinale },
  ];
}
