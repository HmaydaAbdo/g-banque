import { RecaputilatifItem } from '../etatsDesEncours/views/lesEncours/recap/dtos/RecaputilatifItem';
import { SensOperation } from '../transactions/dtos/SensOperation';
import { TransactionSource, TransactionSourceLabels } from '../transactions/dtos/TransactionSource';
import { BlocSens, blocSens, OperationEncours, versOperation } from '../accueil/encours-timeline';
import { ColonnePlan, LignePlan, PlanBanque } from './dtos/PlanTresorerie';

/**
 * Plan de trésorerie d'une banque — logique de la page dédiée, sans Angular.
 *
 * Les soldes (ouverture, clôture, courbe, point bas) viennent toujours du plan calculé par le backend, sur toutes les
 * opérations : les filtres ne font que choisir les opérations montrées, jamais les soldes. Les flux par catégorie et par
 * source sont recalculés depuis les opérations du récapitulatif, rangées dans les colonnes du plan avec la même règle
 * que le backend (date de référence ; aujourd'hui et avant = « À ce jour »).
 */

export type SensFiltre = 'TOUS' | 'ENCAISSEMENT' | 'DECAISSEMENT';
export type Operateur = '>=' | '>' | '=' | '<=' | '<';

export interface FiltresPlan {
  sens: SensFiltre;
  sources: TransactionSource[];
  operateur: Operateur;
  /** Montant comparé (positif) ; `null` = pas de filtre de montant. */
  montant: number | null;
  /** Recherche sur le libellé, l'observation et le numéro. */
  texte: string;
}

export const FILTRES_VIDES: FiltresPlan = { sens: 'TOUS', sources: [], operateur: '>=', montant: null, texte: '' };

export const OPERATEURS: { label: string; value: Operateur }[] = [
  { label: '≥', value: '>=' }, { label: '>', value: '>' }, { label: '=', value: '=' },
  { label: '≤', value: '<=' }, { label: '<', value: '<' },
];

export function filtreActif(f: FiltresPlan): boolean {
  return f.sens !== 'TOUS' || f.sources.length > 0 || f.montant !== null || f.texte.trim() !== '';
}

export function montantDe(item: RecaputilatifItem): number {
  return Number(item.sens === SensOperation.ENCAISSEMENT ? item.credit : item.debit) || 0;
}

function compare(valeur: number, operateur: Operateur, seuil: number): boolean {
  switch (operateur) {
    case '>': return valeur > seuil;
    case '=': return Math.abs(valeur - seuil) < 0.005;
    case '<=': return valeur <= seuil;
    case '<': return valeur < seuil;
    default: return valeur >= seuil;
  }
}

/** L'opération correspond-elle aux filtres ? */
export function garder(item: RecaputilatifItem, f: FiltresPlan): boolean {
  if (f.sens !== 'TOUS' && item.sens !== f.sens) {
    return false;
  }
  if (f.sources.length > 0 && !f.sources.includes(item.source)) {
    return false;
  }
  if (f.montant !== null && !compare(montantDe(item), f.operateur, f.montant)) {
    return false;
  }
  const texte = f.texte.trim().toLowerCase();
  if (texte) {
    const cible = `${item.libelle ?? ''} ${item.observations ?? ''} ${item.numero ?? ''}`.toLowerCase();
    return cible.includes(texte);
  }
  return true;
}

/** Colonne d'une date, même règle que le backend : aujourd'hui ou avant → « À ce jour », après l'horizon → dernière. */
export function indexColonne(date: string, colonnes: ColonnePlan[]): number {
  if (colonnes.length === 0 || !date || date <= colonnes[0].au) {
    return 0;
  }
  const i = colonnes.findIndex((c, j) => j > 0 && date <= c.au);
  return i > 0 ? i : colonnes.length - 1;
}

/** Jour où l'opération pèse sur le solde : sa date, ou aujourd'hui si elle est passée (comme le backend). */
function jourEffectif(date: string, aujourdHui: string): string {
  return date && date > aujourdHui ? date : aujourdHui;
}

// ── Libellés de période ─────────────────────────────────────────────────────

const MOIS_COURT = ['janv.', 'févr.', 'mars', 'avr.', 'mai', 'juin', 'juil.', 'août', 'sept.', 'oct.', 'nov.', 'déc.'];

function jourMoisCourt(iso: string): string {
  return `${Number(iso.slice(8, 10))} ${MOIS_COURT[Number(iso.slice(5, 7)) - 1]}`;
}

/** Numéro de semaine ISO (lundi-dimanche), comme les colonnes hebdomadaires du plan. */
function semaineIso(iso: string): number {
  const d = new Date(Date.UTC(Number(iso.slice(0, 4)), Number(iso.slice(5, 7)) - 1, Number(iso.slice(8, 10))));
  const jour = d.getUTCDay() || 7;
  d.setUTCDate(d.getUTCDate() + 4 - jour);
  const debutAnnee = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
  return Math.ceil(((d.getTime() - debutAnnee.getTime()) / 86400000 + 1) / 7);
}

/**
 * Titre et sous-titre d'une période, pour le récit et le tableau : « À ce jour » (échues et du jour) ; une semaine
 * « 19 – 25 oct. » (semaine 43) ; un jour seul « 18 oct. » ; un mois « Novembre 2026 » (1 – 30 nov.).
 */
export function libellePeriode(colonne: ColonnePlan, index: number): { titre: string; sous: string } {
  if (index === 0 || !colonne.du) {
    return { titre: 'À ce jour', sous: 'échues et du jour' };
  }
  const du = colonne.du;
  const au = colonne.au;
  const memeMois = du.slice(0, 7) === au.slice(0, 7);
  const plage = du === au
    ? jourMoisCourt(du)
    : memeMois
      ? `${Number(du.slice(8, 10))} – ${jourMoisCourt(au)}`
      : `${jourMoisCourt(du)} – ${jourMoisCourt(au)}`;
  if (!colonne.libelle.startsWith('Du ')) {
    // Colonne mensuelle (fin de trimestre) : le backend la nomme déjà « Novembre 2026 »
    return { titre: colonne.libelle, sous: plage };
  }
  return { titre: plage, sous: du === au ? 'un seul jour' : `semaine ${semaineIso(du)}` };
}

// ── Jalons ──────────────────────────────────────────────────────────────────

export type NiveauJalon = 'info' | 'warn' | 'danger';

export interface Jalon {
  code: 'AUJOURDHUI' | 'SOUS_ZERO' | 'FC_DEPASSEE' | 'POINT_BAS';
  date: string;
  /** Colonne du plan qui contient le jalon (le jalon s'affiche après elle). */
  colonne: number;
  niveau: NiveauJalon;
  texte: string;
  /** Opérations qui provoquent l'événement (surlignées quand on clique sur le jalon). */
  causes: string[];
}

function montantTexte(v: number): string {
  return Math.abs(v).toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

/** La plus grosse sortie du jour : ce qui fait basculer le solde ce jour-là. */
function causeDuJour(jour: string, aujourdHui: string, items: RecaputilatifItem[]): RecaputilatifItem | null {
  const sorties = items.filter(i => i.sens === SensOperation.DECAISSEMENT && jourEffectif(i.dateReference, aujourdHui) === jour);
  return sorties.sort((a, b) => montantDe(b) - montantDe(a))[0] ?? null;
}

function descriptionCause(cause: RecaputilatifItem | null): string {
  if (!cause) {
    return '';
  }
  const libelle = cause.libelle || cause.observations || TransactionSourceLabels[cause.source];
  return ` après « ${libelle} » (${TransactionSourceLabels[cause.source] ?? cause.source}, ${montantTexte(montantDe(cause))})`;
}

/**
 * Événements de l'histoire, dans l'ordre chronologique : aujourd'hui, passage sous zéro, dépassement de la facilité
 * de caisse et point bas. Calculés sur la courbe du backend (toutes les opérations) ; les causes sont les opérations
 * du jour, toutes sources confondues.
 */
export function construireJalons(plan: PlanBanque, colonnes: ColonnePlan[], items: RecaputilatifItem[]): Jalon[] {
  if (colonnes.length === 0 || plan.courbe.length === 0) {
    return [];
  }
  const aujourdHui = colonnes[0].au;
  const jalons: Jalon[] = [{
    code: 'AUJOURDHUI', date: aujourdHui, colonne: 0, niveau: 'info',
    texte: `Aujourd'hui · solde ${montantTexte(plan.soldeActuel)}${plan.soldeActuel < 0 ? ' débiteur' : ''}`, causes: [],
  }];
  const plafond = plan.plafondFC > 0 ? plan.plafondFC : 0;

  if (plan.soldeActuel >= 0) {
    const jour = plan.courbe.find(p => p.solde < 0);
    if (jour) {
      const cause = causeDuJour(jour.date, aujourdHui, items);
      jalons.push({
        code: 'SOUS_ZERO', date: jour.date, colonne: indexColonne(jour.date, colonnes), niveau: plafond > 0 ? 'warn' : 'danger',
        texte: (plafond > 0 ? 'Le solde passe sous zéro : la facilité de caisse prend le relais' : 'Le solde passe sous zéro, sans facilité de caisse')
          + descriptionCause(cause),
        causes: cause ? [`${cause.source}:${cause.id}`] : [],
      });
    }
  }
  if (plafond > 0 && plan.soldeActuel + plafond >= 0) {
    const jour = plan.courbe.find(p => p.solde + plafond < 0);
    if (jour) {
      const cause = causeDuJour(jour.date, aujourdHui, items);
      jalons.push({
        code: 'FC_DEPASSEE', date: jour.date, colonne: indexColonne(jour.date, colonnes), niveau: 'danger',
        texte: 'Facilité de caisse dépassée' + descriptionCause(cause),
        causes: cause ? [`${cause.source}:${cause.id}`] : [],
      });
    }
  }
  const bas = plan.pointBas;
  if (bas && bas.solde < plan.soldeActuel) {
    const besoin = bas.disponibilite < 0 ? ` · besoin non couvert ${montantTexte(bas.disponibilite)}` : '';
    jalons.push({
      code: 'POINT_BAS', date: bas.date, colonne: indexColonne(bas.date, colonnes), niveau: bas.solde + plafond < 0 ? 'danger' : 'warn',
      texte: `Point bas : ${bas.solde < 0 ? '−' : ''}${montantTexte(bas.solde)}${besoin}`,
      causes: [],
    });
  }
  return jalons.sort((a, b) => a.date.localeCompare(b.date));
}

// ── Récit : une étape par colonne du plan ───────────────────────────────────

export type NiveauSolde = 'positif' | 'couvert' | 'depasse';

export function niveauSolde(solde: number, plafond: number): NiveauSolde {
  return solde >= 0 ? 'positif' : solde + Math.max(plafond, 0) >= 0 ? 'couvert' : 'depasse';
}

export interface PastilleSource {
  sens: 'ENCAISSEMENT' | 'DECAISSEMENT';
  source: TransactionSource;
  libelle: string;
  total: number;
  nombre: number;
  /** Faux quand des filtres sont actifs et qu'aucune opération de la source n'y correspond (pastille atténuée). */
  retenue: boolean;
}

export interface EtapeRecit {
  colonne: ColonnePlan;
  index: number;
  ouverture: number;
  cloture: number;
  entrees: number;
  sorties: number;
  niveau: NiveauSolde;
  /** Toutes les sources de la période (filtres ou non), entrées puis sorties, plus gros total d'abord. */
  pastilles: PastilleSource[];
  /** Opérations retenues par les filtres, groupées par sens puis par source. */
  entreesFiltrees: BlocSens | null;
  sortiesFiltrees: BlocSens | null;
  nombreFiltrees: number;
}

function pastilles(items: RecaputilatifItem[], filtres: FiltresPlan): PastilleSource[] {
  const groupes = new Map<string, PastilleSource>();
  for (const item of items) {
    const cle = `${item.sens}|${item.source}`;
    const p = groupes.get(cle) ?? {
      sens: item.sens as PastilleSource['sens'], source: item.source,
      libelle: TransactionSourceLabels[item.source] ?? item.source, total: 0, nombre: 0, retenue: false,
    };
    p.total += montantDe(item);
    p.nombre++;
    p.retenue = p.retenue || garder(item, filtres);
    groupes.set(cle, p);
  }
  return [...groupes.values()].sort((a, b) =>
    (a.sens === b.sens ? 0 : a.sens === 'ENCAISSEMENT' ? -1 : 1) || b.total - a.total);
}

/** Une étape par colonne du plan : soldes du backend, pastilles par source, opérations filtrées groupées par sens › source. */
export function construireEtapes(plan: PlanBanque, colonnes: ColonnePlan[], items: RecaputilatifItem[], filtres: FiltresPlan): EtapeRecit[] {
  return colonnes.map((colonne, index) => {
    const ouverture = index === 0 ? plan.soldeActuel : plan.soldesFin[index - 1];
    const dansColonne = items.filter(i => indexColonne(i.dateReference, colonnes) === index);
    const retenues = dansColonne.filter(i => garder(i, filtres));
    const ops = (sens: SensOperation): OperationEncours[] => retenues.filter(i => i.sens === sens).map(versOperation);
    return {
      colonne, index, ouverture,
      cloture: plan.soldesFin[index],
      entrees: plan.totalEncaissements[index],
      sorties: plan.totalDecaissements[index],
      niveau: niveauSolde(plan.soldesFin[index], plan.plafondFC),
      pastilles: pastilles(dansColonne, filtres),
      entreesFiltrees: blocSens(ops(SensOperation.ENCAISSEMENT)),
      sortiesFiltrees: blocSens(ops(SensOperation.DECAISSEMENT)),
      nombreFiltrees: retenues.length,
    };
  });
}

/** Colonne à ouvrir par défaut : celle du point bas (là où l'histoire se joue). */
export function colonneDuPointBas(plan: PlanBanque, colonnes: ColonnePlan[]): string {
  return colonnes[indexColonne(plan.pointBas?.date ?? '', colonnes)]?.code ?? '';
}

// ── Tableau : catégorie › source × colonnes ─────────────────────────────────

export interface LigneTableau {
  cle: string;
  niveau: 1 | 2;
  sens: 'ENCAISSEMENT' | 'DECAISSEMENT';
  libelle: string;
  categorie: string;
  source?: TransactionSource;
  montants: number[];
  total: number;
}

export interface TableauPlan {
  entrees: LigneTableau[];
  sorties: LigneTableau[];
  totalEntrees: number[];
  totalSorties: number[];
}

/**
 * Lignes du tableau : chaque catégorie du plan (dans l'ordre du backend) puis ses sources, montants recalculés depuis
 * les opérations retenues par les filtres. Sans filtre, les totaux sont ceux du plan (mêmes opérations, même règle).
 * Une catégorie ou une source sans opération retenue est omise.
 */
export function construireTableau(plan: PlanBanque, colonnes: ColonnePlan[], items: RecaputilatifItem[], filtres: FiltresPlan): TableauPlan {
  const n = colonnes.length;
  const retenues = items.filter(i => garder(i, filtres));
  const lignesSens = (sens: 'ENCAISSEMENT' | 'DECAISSEMENT', lignesPlan: LignePlan[]): LigneTableau[] => {
    const resultat: LigneTableau[] = [];
    for (const lp of lignesPlan) {
      const sources = lp.sources ?? [];
      const sousLignes: LigneTableau[] = sources.map(source => {
        const montants = new Array<number>(n).fill(0);
        retenues.filter(i => i.sens === sens && i.source === source)
          .forEach(i => montants[indexColonne(i.dateReference, colonnes)] += montantDe(i));
        return {
          cle: `${sens}|${lp.categorie}|${source}`, niveau: 2 as const, sens, categorie: lp.categorie, source,
          libelle: TransactionSourceLabels[source] ?? source, montants, total: montants.reduce((s, v) => s + v, 0),
        };
      }).filter(l => l.total !== 0);
      if (sousLignes.length === 0) {
        continue;
      }
      const montants = Array.from({ length: n }, (_, k) => sousLignes.reduce((s, l) => s + l.montants[k], 0));
      const categorie: LigneTableau = {
        cle: `${sens}|${lp.categorie}`, niveau: 1, sens, categorie: lp.categorie, libelle: lp.categorie,
        montants, total: montants.reduce((s, v) => s + v, 0),
      };
      if (sources.length === 1) {
        // Catégorie d'une seule source (ex. Leasing) : une seule ligne, détaillable directement
        resultat.push({ ...categorie, source: sources[0] });
      } else {
        resultat.push(categorie, ...sousLignes);
      }
    }
    return resultat;
  };
  const entrees = lignesSens('ENCAISSEMENT', plan.encaissements);
  const sorties = lignesSens('DECAISSEMENT', plan.decaissements);
  const totaux = (lignes: LigneTableau[]) =>
    Array.from({ length: n }, (_, k) => lignes.filter(l => l.niveau === 1).reduce((s, l) => s + l.montants[k], 0));
  return { entrees, sorties, totalEntrees: totaux(entrees), totalSorties: totaux(sorties) };
}

/** Opérations d'une case du tableau (source × colonne), retenues par les filtres, par date. */
export function operationsDeCase(items: RecaputilatifItem[], colonnes: ColonnePlan[], filtres: FiltresPlan,
                                 sens: string, source: TransactionSource, index: number): RecaputilatifItem[] {
  return items
    .filter(i => i.sens === sens && i.source === source && indexColonne(i.dateReference, colonnes) === index && garder(i, filtres))
    .sort((a, b) => (a.dateReference ?? '').localeCompare(b.dateReference ?? ''));
}
