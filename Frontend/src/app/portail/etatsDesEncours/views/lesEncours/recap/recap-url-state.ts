import { ParamMap, Params } from '@angular/router';
import { TransactionSource } from '../../../../transactions/dtos/TransactionSource';
import { SensOperation } from '../../../../transactions/dtos/SensOperation';
import { Motif } from '../../../../impotEtTaxes/dtos/Motif';
import { parseLocalDate, toLocalDateString } from '../../../../global/types/date.utils';
import { HORIZON_PAR_DEFAUT, HorizonPrevision, horizonDepuisParam } from './dtos/PeriodeRecap';

/**
 * État de l'écran Récapitulatif porté par l'adresse (queryParams) : source ouverte, filtres
 * globaux (horizon, sens, types), filtres propres à la source, page / tri du tableau et ligne à
 * surligner.
 *
 * Les formulaires d'ajout et d'édition se ferment par un retour arrière du navigateur : l'écran
 * est alors reconstruit, et c'est l'adresse (écrite en `replaceUrl`, donc sans entrée d'historique
 * supplémentaire) qui lui rend exactement l'état qu'il avait avant le départ.
 *
 * Fonctions pures uniquement — l'écran lit et écrit l'état, ce fichier ne fait que le (dé)sérialiser.
 */

export const RECAP_LIGNES_PAR_PAGE = [5, 10, 25, 50];
export const RECAP_LIGNES_PAR_DEFAUT = 10;
export const RECAP_TRI_PAR_DEFAUT = 'dateReference';

/** Colonnes triables du tableau — liste blanche : le tri part tel quel vers l'API. */
const CHAMPS_TRI = [
  RECAP_TRI_PAR_DEFAUT, 'sens', 'dateEmission', 'dateEcheanceAffichage', 'libelle', 'observation',
  'id.sourceType', 'montant', 'id.entityId',
];

/** Filtres globaux : communs à toutes les sources, ils alimentent aussi les cartes et la liste des sources. */
export interface RecapFiltresGlobaux {
  /** Horizon des encours affichés : fin de mois (défaut) ou fin de trimestre. */
  periode: HorizonPrevision;
  transactionSources: TransactionSource[];
  sens: SensOperation | null;
}

/** Filtres de la source ouverte : ils ne s'appliquent qu'au tableau de cette source. */
export interface RecapFiltresSource {
  libelleOuObservation: string;
  numero: string;
  dateReferenceDepuis: Date | null;
  dateReferenceJusqua: Date | null;
  montant: number | null;
  /** Motif de Commission (valeur de {@link TransactionSource}) ou d'Impôt/Taxe (valeur de {@link Motif}). */
  motif: string | null;
}

export interface RecapEtatTableau {
  first: number;
  rows: number;
  sortField: string;
  sortOrder: 1 | -1;
}

export interface RecapEtatUrl {
  source: TransactionSource | null;
  globaux: RecapFiltresGlobaux;
  locaux: RecapFiltresSource;
  tableau: RecapEtatTableau;
  /** Clé composite `${source}:${id}` de la ligne à surligner au retour d'une édition. */
  focus: string | null;
}

export function filtresGlobauxVides(): RecapFiltresGlobaux {
  return { periode: HORIZON_PAR_DEFAUT, transactionSources: [], sens: null };
}

export function filtresSourceVides(): RecapFiltresSource {
  return {
    libelleOuObservation: '',
    numero: '',
    dateReferenceDepuis: null,
    dateReferenceJusqua: null,
    montant: null,
    motif: null,
  };
}

export function tableauParDefaut(rows: number = RECAP_LIGNES_PAR_DEFAUT): RecapEtatTableau {
  return { first: 0, rows, sortField: RECAP_TRI_PAR_DEFAUT, sortOrder: -1 };
}

/** Motifs acceptés pour une source — {@code null} si la source n'a pas de filtre de motif. */
export function motifsValides(source: TransactionSource | null): string[] {
  if (source === TransactionSource.COMMISSION) {
    return Object.values(TransactionSource);
  }
  if (source === TransactionSource.IMPOT_TAXE) {
    return Object.values(Motif);
  }
  return [];
}

/** Paramètres d'adresse de l'état — les valeurs neutres ou par défaut sont omises. */
export function etatVersQueryParams(etat: RecapEtatUrl): Params {
  const params: Params = {};

  const g = etat.globaux;
  if (g.periode !== HORIZON_PAR_DEFAUT) params['horizon'] = g.periode;
  if (g.transactionSources.length > 0) params['types'] = g.transactionSources.join(',');
  if (g.sens) params['sens'] = g.sens;

  if (!etat.source) {
    return params;
  }
  params['source'] = etat.source;

  const s = etat.locaux;
  if (s.libelleOuObservation) params['q'] = s.libelleOuObservation;
  if (s.numero) params['numero'] = s.numero;
  if (s.montant !== null) params['montant'] = s.montant;
  const du = toLocalDateString(s.dateReferenceDepuis);
  if (du) params['du'] = du;
  const au = toLocalDateString(s.dateReferenceJusqua);
  if (au) params['au'] = au;
  if (s.motif) params['motif'] = s.motif;

  const t = etat.tableau;
  if (t.first > 0) params['page'] = Math.floor(t.first / t.rows) + 1;
  if (t.rows !== RECAP_LIGNES_PAR_DEFAUT) params['rows'] = t.rows;
  if (t.sortField !== RECAP_TRI_PAR_DEFAUT || t.sortOrder !== -1) {
    params['sort'] = `${t.sortField},${t.sortOrder === 1 ? 'asc' : 'desc'}`;
  }
  if (etat.focus) params['focus'] = etat.focus;

  return params;
}

/** État décrit par une adresse — toute valeur inconnue ou illisible retombe sur sa valeur neutre. */
export function queryParamsVersEtat(map: ParamMap): RecapEtatUrl {
  const source = enumValide<TransactionSource>(map.get('source'), Object.values(TransactionSource));

  const globaux: RecapFiltresGlobaux = {
    periode: horizonDepuisParam(map.get('horizon')),
    transactionSources: (map.get('types') ?? '')
      .split(',')
      .filter((v): v is TransactionSource => (Object.values(TransactionSource) as string[]).includes(v)),
    sens: enumValide<SensOperation>(map.get('sens'), Object.values(SensOperation)),
  };

  if (!source) {
    return { source: null, globaux, locaux: filtresSourceVides(), tableau: tableauParDefaut(), focus: null };
  }

  const locaux: RecapFiltresSource = {
    libelleOuObservation: map.get('q') ?? '',
    numero: map.get('numero') ?? '',
    dateReferenceDepuis: parseLocalDate(map.get('du')),
    dateReferenceJusqua: parseLocalDate(map.get('au')),
    montant: nombre(map.get('montant')),
    motif: enumValide(map.get('motif'), motifsValides(source)),
  };

  const rowsBrut = Number(map.get('rows'));
  const rows = RECAP_LIGNES_PAR_PAGE.includes(rowsBrut) ? rowsBrut : RECAP_LIGNES_PAR_DEFAUT;
  const page = Math.max(1, Math.floor(Number(map.get('page'))) || 1);
  const [champ, sens] = (map.get('sort') ?? '').split(',');
  const triValide = CHAMPS_TRI.includes(champ) && (sens === 'asc' || sens === 'desc');

  return {
    source,
    globaux,
    locaux,
    tableau: {
      first: (page - 1) * rows,
      rows,
      sortField: triValide ? champ : RECAP_TRI_PAR_DEFAUT,
      sortOrder: triValide ? (sens === 'asc' ? 1 : -1) : -1,
    },
    focus: map.get('focus'),
  };
}

/** Forme canonique d'un jeu de paramètres, pour reconnaître nos propres écritures d'adresse. */
export function serialiserParams(params: Params): string {
  return Object.keys(params)
    .sort()
    .map(cle => `${cle}=${params[cle]}`)
    .join('&');
}

export function serialiserParamMap(map: ParamMap): string {
  const params: Params = {};
  for (const cle of map.keys) {
    params[cle] = map.get(cle);
  }
  return serialiserParams(params);
}

function enumValide<T extends string>(valeur: string | null, autorises: readonly string[]): T | null {
  return valeur !== null && autorises.includes(valeur) ? (valeur as T) : null;
}

function nombre(valeur: string | null): number | null {
  if (valeur === null || valeur.trim() === '') {
    return null;
  }
  const n = Number(valeur);
  return Number.isFinite(n) ? n : null;
}
