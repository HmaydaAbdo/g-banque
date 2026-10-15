import { Component, Input, OnChanges } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { TooltipModule } from 'primeng/tooltip';
import { ColonnePlan, LignePlan, PlanBanque } from '../../dtos/PlanTresorerie';

/** Ordre d'affichage des familles de catégories (les noms viennent du backend). */
const FAMILLES = ['Règlements', 'Financements', 'Charges bancaires', 'Autres'];

/** Ligne affichée du plan : solde, en-tête de sens, famille (dépliable), catégorie ou total. */
export interface LigneAffichee {
  /** Identifiant stable ; pour une famille `sens:famille`, repris par ses catégories dans `parent`. */
  cle: string;
  libelle: string;
  type: 'solde' | 'section' | 'famille' | 'categorie' | 'total';
  valeurs: number[];
  total: number;
  parent?: string;
}

/**
 * Plan de trésorerie d'une banque : une colonne par période, des lignes regroupées par famille
 * (Règlements, Financements, Charges bancaires, Autres) que l'on déplie pour voir les catégories,
 * encadrées par le solde de début, les totaux, le solde de fin et la disponibilité.
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
  /** Familles dépliées ; tout est replié au départ pour ne montrer que l'essentiel. */
  private readonly ouvertes = new Set<string>();

  ngOnChanges(): void {
    this.lignes = this.plan ? construireLignes(this.plan) : [];
  }

  visible(l: LigneAffichee): boolean {
    return l.type !== 'categorie' || this.ouvertes.has(l.parent!);
  }

  ouverte(cle: string): boolean {
    return this.ouvertes.has(cle);
  }

  basculer(cle: string): void {
    if (!this.ouvertes.delete(cle)) {
      this.ouvertes.add(cle);
    }
  }
}

const somme = (valeurs: number[]) => valeurs.reduce((a, b) => a + b, 0);

/** Une section (encaissements ou décaissements) : en-tête, puis chaque famille suivie de ses catégories. */
function sectionParFamille(prefixe: string, titre: string, lignes: LignePlan[], n: number): LigneAffichee[] {
  const resultat: LigneAffichee[] = [{ cle: `${prefixe}:section`, libelle: titre, type: 'section', valeurs: [], total: 0 }];
  const familles = [...new Set([...FAMILLES, ...lignes.map(l => l.famille)])];
  for (const famille of familles) {
    const membres = lignes.filter(l => l.famille === famille);
    if (membres.length === 0) {
      continue;
    }
    const cle = `${prefixe}:${famille}`;
    const valeurs = Array.from({ length: n }, (_, i) => somme(membres.map(m => m.montants[i])));
    resultat.push({ cle, libelle: famille, type: 'famille', valeurs, total: somme(valeurs) });
    membres.forEach(m => resultat.push({
      cle: `${cle}:${m.categorie}`, libelle: m.categorie, type: 'categorie', valeurs: m.montants, total: m.total, parent: cle,
    }));
  }
  return resultat;
}

/** Lignes du tableau, dans l'ordre de lecture d'un plan de trésorerie. */
export function construireLignes(plan: PlanBanque): LigneAffichee[] {
  const n = plan.soldesFin.length;
  const soldesDebut = [plan.soldeActuel, ...plan.soldesFin.slice(0, n - 1)];

  return [
    { cle: 'debut', libelle: 'Solde de début', type: 'solde', valeurs: soldesDebut, total: plan.soldeActuel },
    ...sectionParFamille('enc', 'Encaissements', plan.encaissements, n),
    { cle: 'tot-enc', libelle: 'Total encaissements', type: 'total', valeurs: plan.totalEncaissements, total: somme(plan.totalEncaissements) },
    ...sectionParFamille('dec', 'Décaissements', plan.decaissements, n),
    { cle: 'tot-dec', libelle: 'Total décaissements', type: 'total', valeurs: plan.totalDecaissements, total: somme(plan.totalDecaissements) },
    { cle: 'fin', libelle: 'Solde de fin', type: 'solde', valeurs: plan.soldesFin, total: plan.soldeFinal },
    { cle: 'fc', libelle: 'Facilité de caisse', type: 'categorie', valeurs: Array(n).fill(plan.plafondFC), total: plan.plafondFC, parent: 'fc' },
    { cle: 'dispo', libelle: 'Disponibilité', type: 'solde', valeurs: plan.disponibilites, total: plan.disponibiliteFinale },
  ];
}
