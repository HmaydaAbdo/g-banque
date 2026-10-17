import { HorizonPrevision } from '../etatsDesEncours/views/lesEncours/recap/dtos/PeriodeRecap';
import { RecaputilatifItem } from '../etatsDesEncours/views/lesEncours/recap/dtos/RecaputilatifItem';
import { SensOperation } from '../transactions/dtos/SensOperation';
import { TransactionSource, TransactionSourceLabels } from '../transactions/dtos/TransactionSource';

/** Une opération d'encours telle que le popup l'affiche : montant toujours positif, le sens porte le signe. */
export interface OperationEncours {
  source: TransactionSource;
  date: string;
  libelle: string;
  numero: string | null;
  montant: number;
  /** Identité stable `${source}:${id}` (l'identifiant seul se répète d'une source à l'autre). */
  cle: string;
}

/** Les opérations d'une même source, au sein d'un sens et d'une période. */
export interface GroupeSource {
  source: TransactionSource;
  libelle: string;
  total: number;
  operations: OperationEncours[];
}

/** Toutes les opérations d'un même sens au sein d'une période, groupées par source (plus gros total d'abord). */
export interface BlocSens {
  total: number;
  nombre: number;
  sources: GroupeSource[];
}

export interface PeriodeTimeline {
  /** Clé stable : « echues » ou la date de début (yyyy-MM-dd). */
  code: string;
  titre: string;
  precision: string;
  echue: boolean;
  entrees: BlocSens | null;
  sorties: BlocSens | null;
  /** Entrées − sorties de la période. */
  net: number;
}

interface Borne {
  debut: Date;
  fin: Date;
}

const MOIS_COURT = (d: Date): string => d.toLocaleDateString('fr-FR', { month: 'short' });

function jour(iso: string): Date {
  return new Date(Number(iso.slice(0, 4)), Number(iso.slice(5, 7)) - 1, Number(iso.slice(8, 10)));
}

function iso(d: Date): string {
  const m = `${d.getMonth() + 1}`.padStart(2, '0');
  const j = `${d.getDate()}`.padStart(2, '0');
  return `${d.getFullYear()}-${m}-${j}`;
}

function aucuneHeure(d: Date): Date {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate());
}

/**
 * Découpe l'horizon : par semaine (lundi à dimanche, la première commençant aujourd'hui) pour la fin de mois,
 * par mois civil (le premier commençant aujourd'hui) pour la fin de trimestre — comme les colonnes du plan de trésorerie.
 */
export function bornesDesPeriodes(aujourdhui: Date, horizon: HorizonPrevision): Borne[] {
  const debutJour = aucuneHeure(aujourdhui);
  const dernierMois = horizon === 'FIN_DE_TRIMESTRE' ? Math.floor(debutJour.getMonth() / 3) * 3 + 2 : debutJour.getMonth();
  const fin = new Date(debutJour.getFullYear(), dernierMois + 1, 0);
  const bornes: Borne[] = [];
  let debut = debutJour;
  while (debut <= fin) {
    let finPeriode: Date;
    if (horizon === 'FIN_DE_TRIMESTRE') {
      finPeriode = new Date(debut.getFullYear(), debut.getMonth() + 1, 0);
    } else {
      const joursJusquauDimanche = (7 - debut.getDay()) % 7;
      finPeriode = new Date(debut.getFullYear(), debut.getMonth(), debut.getDate() + joursJusquauDimanche);
    }
    if (finPeriode > fin) {
      finPeriode = fin;
    }
    bornes.push({ debut, fin: finPeriode });
    debut = new Date(finPeriode.getFullYear(), finPeriode.getMonth(), finPeriode.getDate() + 1);
  }
  return bornes;
}

function libelleBorne(b: Borne, horizon: HorizonPrevision): { titre: string; precision: string } {
  const memeMois = b.debut.getMonth() === b.fin.getMonth();
  const plage = b.debut.getTime() === b.fin.getTime()
    ? `${b.debut.getDate()} ${MOIS_COURT(b.debut)}`
    : memeMois
      ? `${b.debut.getDate()} – ${b.fin.getDate()} ${MOIS_COURT(b.fin)}`
      : `${b.debut.getDate()} ${MOIS_COURT(b.debut)} – ${b.fin.getDate()} ${MOIS_COURT(b.fin)}`;
  if (horizon === 'FIN_DE_TRIMESTRE') {
    const mois = b.debut.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });
    return { titre: mois.charAt(0).toUpperCase() + mois.slice(1), precision: plage };
  }
  return { titre: plage, precision: '' };
}

/** Groupe les opérations d'un même sens par source (plus gros total d'abord) ; `null` sans opération. */
export function blocSens(operations: OperationEncours[]): BlocSens | null {
  if (operations.length === 0) {
    return null;
  }
  const parSource = new Map<TransactionSource, OperationEncours[]>();
  for (const op of operations) {
    parSource.set(op.source, [...(parSource.get(op.source) ?? []), op]);
  }
  const sources: GroupeSource[] = [...parSource.entries()].map(([source, ops]) => ({
    source,
    libelle: TransactionSourceLabels[source] ?? source,
    total: ops.reduce((s, o) => s + o.montant, 0),
    operations: [...ops].sort((a, b) => (a.date ?? '').localeCompare(b.date ?? '') || a.libelle.localeCompare(b.libelle)),
  }));
  sources.sort((a, b) => b.total - a.total || a.libelle.localeCompare(b.libelle));
  return { total: sources.reduce((s, g) => s + g.total, 0), nombre: operations.length, sources };
}

/** Opération d'encours à partir d'une ligne du récapitulatif : date de référence, libellé (ou observation), montant positif. */
export function versOperation(item: RecaputilatifItem): OperationEncours {
  const entree = item.sens === SensOperation.ENCAISSEMENT;
  const libelle = item.libelle || item.observations || '';
  return {
    source: item.source,
    date: item.dateReference ?? item.dateEcheance ?? item.dateEmission,
    libelle,
    // Un numéro déjà écrit dans le libellé (« contrat N° 0557150 ») n'est pas répété à côté
    numero: item.numero && !libelle.includes(item.numero) ? item.numero : null,
    montant: Number(entree ? item.credit : item.debit),
    cle: `${item.source}:${item.id}`,
  };
}

/**
 * Range les opérations d'encours en timeline, d'après leur date de référence (celle qui pilote l'horizon) :
 * « Échues » (date antérieure à aujourd'hui) puis une période par semaine
 * ou par mois jusqu'à l'horizon ; dans chaque période, un bloc par sens, puis un groupe par source. Les périodes sans
 * opération sont omises ; une opération datée après l'horizon rejoint la dernière période.
 */
export function construireTimeline(items: RecaputilatifItem[], aujourdhui: Date, horizon: HorizonPrevision): PeriodeTimeline[] {
  const debutJour = aucuneHeure(aujourdhui);
  const bornes = bornesDesPeriodes(aujourdhui, horizon);
  const seaux = new Map<string, { entrees: OperationEncours[]; sorties: OperationEncours[] }>();
  const seau = (code: string) => {
    if (!seaux.has(code)) {
      seaux.set(code, { entrees: [], sorties: [] });
    }
    return seaux.get(code)!;
  };

  for (const item of items) {
    const entree = item.sens === SensOperation.ENCAISSEMENT;
    const operation = versOperation(item);
    const date = operation.date;
    let code = 'echues';
    if (date && jour(date) >= debutJour) {
      const d = jour(date);
      const borne = bornes.find(b => d <= b.fin) ?? bornes[bornes.length - 1];
      code = iso(borne.debut);
    }
    seau(code)[entree ? 'entrees' : 'sorties'].push(operation);
  }

  const periodes: PeriodeTimeline[] = [];
  const ajouter = (code: string, titre: string, precision: string, echue: boolean) => {
    const contenu = seaux.get(code);
    if (!contenu) {
      return;
    }
    const entrees = blocSens(contenu.entrees);
    const sorties = blocSens(contenu.sorties);
    periodes.push({ code, titre, precision, echue, entrees, sorties, net: (entrees?.total ?? 0) - (sorties?.total ?? 0) });
  };
  ajouter('echues', 'Échues', `avant le ${debutJour.getDate()} ${MOIS_COURT(debutJour)}`, true);
  for (const borne of bornes) {
    const { titre, precision } = libelleBorne(borne, horizon);
    ajouter(iso(borne.debut), titre, precision, false);
  }
  return periodes;
}
