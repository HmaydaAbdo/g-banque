import { PlanBanque } from './dtos/PlanTresorerie';

/** Ordre de lecture du croisement : chaque banque puis ses sources, ou chaque source puis ses banques. */
export type OrdreCroisement = 'BANQUE_SOURCE' | 'SOURCE_BANQUE';

/**
 * Ligne du croisement banque × source. Les montants sont **signés** (encaissement positif,
 * décaissement négatif) : la ligne d'un niveau 1 est le net de ses lignes de niveau 2.
 */
export interface LigneCroisement {
  /** Identifiant stable ; le niveau 2 reprend celui de son parent dans `parent`. */
  cle: string;
  niveau: 1 | 2;
  libelle: string;
  /** Renseigné pour une ligne de source (niveau 1 ou 2) : sens du flux et famille. */
  sens?: 'ENCAISSEMENT' | 'DECAISSEMENT';
  famille?: string;
  /** Renseigné pour une ligne de banque (niveau 1 ou 2) : permet d'ouvrir son détail. */
  banqueId?: number;
  valeurs: number[];
  total: number;
  parent?: string;
  /** Nombre de lignes de niveau 2 sous cette ligne. */
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

function additionner(listes: number[][], n: number): number[] {
  return Array.from({ length: n }, (_, i) => somme(listes.map(l => l[i])));
}

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
 * Croise les banques d'une devise avec leurs sources. On n'additionne jamais des devises
 * différentes : seules les banques de `devise` entrent dans le croisement.
 */
export function construireCroisement(banques: PlanBanque[], devise: string, ordre: OrdreCroisement): LigneCroisement[] {
  const membres = banques.filter(b => b.banqueId !== null && b.devise === devise);
  const n = membres[0]?.soldesFin.length ?? 0;
  const lignes: LigneCroisement[] = [];

  if (ordre === 'BANQUE_SOURCE') {
    for (const b of membres) {
      const flux = fluxDe(b);
      const cle = `b:${b.banqueId}`;
      const valeurs = additionner(flux.map(f => f.valeurs), n);
      lignes.push({ cle, niveau: 1, libelle: b.banqueNom, banqueId: b.banqueId!, valeurs, total: somme(valeurs), enfants: flux.length });
      flux.forEach(f => lignes.push({
        cle: `${cle}:${f.sens}:${f.categorie}`, niveau: 2, libelle: f.categorie, sens: f.sens, famille: f.famille,
        valeurs: f.valeurs, total: somme(f.valeurs), parent: cle, enfants: 0,
      }));
    }
    return lignes;
  }

  // Source puis banque : on regroupe les flux de toutes les banques par (sens, catégorie)
  const parSource = new Map<string, { flux: Flux; banques: { banque: PlanBanque; valeurs: number[] }[] }>();
  for (const b of membres) {
    for (const f of fluxDe(b)) {
      const cle = `${f.sens}:${f.categorie}`;
      const entree = parSource.get(cle) ?? { flux: f, banques: [] };
      entree.banques.push({ banque: b, valeurs: f.valeurs });
      parSource.set(cle, entree);
    }
  }
  const sources = [...parSource.entries()].sort(([, a], [, b]) => {
    const rang = (f: Flux) => (FAMILLES.indexOf(f.famille) + 1 || FAMILLES.length + 1);
    return rang(a.flux) - rang(b.flux) || (a.flux.sens === b.flux.sens ? 0 : a.flux.sens === 'ENCAISSEMENT' ? -1 : 1);
  });
  for (const [cle, { flux, banques: parBanque }] of sources) {
    const valeurs = additionner(parBanque.map(x => x.valeurs), n);
    lignes.push({
      cle: `s:${cle}`, niveau: 1, libelle: flux.categorie, sens: flux.sens, famille: flux.famille,
      valeurs, total: somme(valeurs), enfants: parBanque.length,
    });
    parBanque.forEach(x => lignes.push({
      cle: `s:${cle}:${x.banque.banqueId}`, niveau: 2, libelle: x.banque.banqueNom, banqueId: x.banque.banqueId!,
      valeurs: x.valeurs, total: somme(x.valeurs), parent: `s:${cle}`, enfants: 0,
    }));
  }
  return lignes;
}

/** Net de toutes les lignes de niveau 1 : la même chose quel que soit l'ordre choisi. */
export function totalCroisement(lignes: LigneCroisement[], n: number): number[] {
  return additionner(lignes.filter(l => l.niveau === 1).map(l => l.valeurs), n);
}
