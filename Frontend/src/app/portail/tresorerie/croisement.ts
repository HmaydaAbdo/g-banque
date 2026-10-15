import { PlanBanque } from './dtos/PlanTresorerie';

/**
 * Ligne du détail banque › source. Les montants sont **signés** (encaissement positif,
 * décaissement négatif) : la ligne d'une banque (niveau 1) est le net de ses sources (niveau 2).
 */
export interface LigneCroisement {
  /** Identifiant stable ; le niveau 2 reprend celui de sa banque dans `parent`. */
  cle: string;
  niveau: 1 | 2;
  libelle: string;
  /** Renseigné pour une ligne de source (niveau 2) : sens du flux et famille. */
  sens?: 'ENCAISSEMENT' | 'DECAISSEMENT';
  famille?: string;
  /** Renseigné pour une ligne de banque (niveau 1) : permet d'ouvrir son détail. */
  banqueId?: number;
  valeurs: number[];
  total: number;
  parent?: string;
  /** Nombre de sources sous cette banque. */
  enfants: number;
}

const FAMILLES = ['Règlements', 'Financements', 'Charges bancaires', 'Autres'];

interface Flux {
  sens: 'ENCAISSEMENT' | 'DECAISSEMENT';
  categorie: string;
  famille: string;
  valeurs: number[];
}

const somme = (valeurs: number[]) => valeurs.reduce((a, b) => a + b, 0);

/** Flux signés d'une banque, classés par famille, puis encaissements avant décaissements. */
function fluxDe(banque: PlanBanque): Flux[] {
  const flux: Flux[] = [
    ...banque.encaissements.map(l => ({ sens: 'ENCAISSEMENT' as const, categorie: l.categorie, famille: l.famille, valeurs: l.montants })),
    ...banque.decaissements.map(l => ({ sens: 'DECAISSEMENT' as const, categorie: l.categorie, famille: l.famille, valeurs: l.montants.map(m => -m) })),
  ];
  const rang = (f: Flux) => (FAMILLES.indexOf(f.famille) + 1 || FAMILLES.length + 1);
  return flux.sort((a, b) => rang(a) - rang(b) || (a.sens === b.sens ? 0 : a.sens === 'ENCAISSEMENT' ? -1 : 1));
}

/**
 * Détail des banques d'une devise : chaque banque suivie de ses sources, avec le net de chaque période.
 * On n'additionne jamais des devises différentes : seules les banques de `devise` sont retenues.
 */
export function construireCroisement(banques: PlanBanque[], devise: string): LigneCroisement[] {
  const lignes: LigneCroisement[] = [];
  for (const b of banques.filter(x => x.banqueId !== null && x.devise === devise)) {
    const flux = fluxDe(b);
    const cle = `b:${b.banqueId}`;
    const n = b.soldesFin.length;
    const valeurs = Array.from({ length: n }, (_, i) => somme(flux.map(f => f.valeurs[i])));
    lignes.push({ cle, niveau: 1, libelle: b.banqueNom, banqueId: b.banqueId!, valeurs, total: somme(valeurs), enfants: flux.length });
    flux.forEach(f => lignes.push({
      cle: `${cle}:${f.sens}:${f.categorie}`, niveau: 2, libelle: f.categorie, sens: f.sens, famille: f.famille,
      valeurs: f.valeurs, total: somme(f.valeurs), parent: cle, enfants: 0,
    }));
  }
  return lignes;
}
