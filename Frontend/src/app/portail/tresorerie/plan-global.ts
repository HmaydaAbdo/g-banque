import { RecaputilatifItem } from '../etatsDesEncours/views/lesEncours/recap/dtos/RecaputilatifItem';
import { SensOperation } from '../transactions/dtos/SensOperation';
import { ColonnePlan, PlanBanque } from './dtos/PlanTresorerie';
import {
  construireJalons, FILTRES_VIDES, filtreActif, FiltresPlan, garder, indexColonne, Jalon, montantDe, niveauSolde, NiveauSolde,
} from './plan-banque';

/**
 * Trésorerie de toutes les banques — logique de la page globale, sur le modèle de la page d'une banque.
 *
 * Le filtre « Banques » fixe le périmètre (les soldes du groupe sont la somme des banques retenues, toujours calculés
 * sur toutes leurs opérations) ; les filtres d'opération (sens, sources, montant, recherche) ne choisissent que les
 * opérations comptées et montrées, jamais les soldes. Une facilité de caisse ne couvre que sa propre banque : le
 * statut d'une période se lit banque par banque.
 */

export interface FiltresGlobal extends FiltresPlan {
  /** Banques du périmètre ; vide = toutes les banques de la devise. */
  banques: number[];
}

export const FILTRES_GLOBAL_VIDES: FiltresGlobal = { ...FILTRES_VIDES, banques: [] };

export function filtreGlobalActif(f: FiltresGlobal): boolean {
  return f.banques.length > 0 || filtreActif(f);
}

/** Banques de la devise, restreintes au filtre « Banques » s'il est posé. */
export function banquesDuPerimetre(banques: PlanBanque[], devise: string, choix: number[]): PlanBanque[] {
  return banques.filter(b => b.banqueId !== null && b.devise === devise && (choix.length === 0 || choix.includes(b.banqueId)));
}

const RANG: Record<NiveauSolde, number> = { depasse: 0, couvert: 1, positif: 2 };

/** Marge d'une banque à un solde : solde + plafond de facilité de caisse (négative = besoin non couvert). */
function marge(solde: number, plafond: number): number {
  return solde + Math.max(plafond, 0);
}

export interface LigneBanqueEtape {
  banque: PlanBanque;
  ouverture: number;
  cloture: number;
  flux: number;
  niveau: NiveauSolde;
  /** Opérations de la banque, sur la période, retenues par les filtres d'opération. */
  nombreFiltrees: number;
}

export interface EtapeGlobale {
  colonne: ColonnePlan;
  index: number;
  ouverture: number;
  cloture: number;
  flux: number;
  /** Nombre de banques par niveau à la clôture de la période. */
  compte: Record<NiveauSolde, number>;
  /** Banques de la plus urgente (dépassement, plus grand besoin d'abord) à la plus sereine. */
  banques: LigneBanqueEtape[];
  nombreFiltrees: number;
}

/** Une étape par colonne du plan : solde du groupe (somme du périmètre), compte des banques par niveau, ligne par banque. */
export function construireEtapesGlobales(banques: PlanBanque[], colonnes: ColonnePlan[], items: RecaputilatifItem[],
                                        filtres: FiltresPlan): EtapeGlobale[] {
  return colonnes.map((colonne, index) => {
    const lignes: LigneBanqueEtape[] = banques.map(b => {
      const ouverture = index === 0 ? b.soldeActuel : b.soldesFin[index - 1];
      const cloture = b.soldesFin[index];
      const nombreFiltrees = items.filter(i => i.banqueId === b.banqueId && indexColonne(i.dateReference, colonnes) === index
        && garder(i, filtres)).length;
      return {
        banque: b, ouverture, cloture, flux: b.totalEncaissements[index] - b.totalDecaissements[index],
        niveau: niveauSolde(cloture, b.plafondFC), nombreFiltrees,
      };
    });
    lignes.sort((a, b) => RANG[a.niveau] - RANG[b.niveau]
      || marge(a.cloture, a.banque.plafondFC) - marge(b.cloture, b.banque.plafondFC));
    const compte: Record<NiveauSolde, number> = { depasse: 0, couvert: 0, positif: 0 };
    lignes.forEach(l => compte[l.niveau]++);
    const somme = (f: (l: LigneBanqueEtape) => number) => lignes.reduce((s, l) => s + f(l), 0);
    return {
      colonne, index, compte, banques: lignes,
      ouverture: somme(l => l.ouverture), cloture: somme(l => l.cloture), flux: somme(l => l.flux),
      nombreFiltrees: somme(l => l.nombreFiltrees),
    };
  });
}

export interface JalonBanque extends Jalon {
  banqueId: number;
  banqueNom: string;
}

/**
 * Jalons de chaque banque du périmètre — passage sous zéro et dépassement de la facilité de caisse, avec l'opération
 * qui les provoque —, du plus ancien au plus récent (à date égale, le dépassement d'abord).
 */
export function construireJalonsGlobaux(banques: PlanBanque[], colonnes: ColonnePlan[], items: RecaputilatifItem[]): JalonBanque[] {
  const jalons: JalonBanque[] = [];
  for (const b of banques) {
    const operations = items.filter(i => i.banqueId === b.banqueId);
    construireJalons(b, colonnes, operations)
      .filter(j => j.code === 'SOUS_ZERO' || j.code === 'FC_DEPASSEE')
      .forEach(j => jalons.push({ ...j, banqueId: b.banqueId!, banqueNom: b.banqueNom }));
  }
  const gravite = (j: JalonBanque) => (j.niveau === 'danger' ? 0 : 1);
  return jalons.sort((a, b) => a.date.localeCompare(b.date) || gravite(a) - gravite(b) || a.banqueNom.localeCompare(b.banqueNom, 'fr'));
}

export interface ResumeGlobal {
  soldeDepart: number;
  soldeFinal: number;
  nombreSorties: number;
  totalSorties: number;
  nombreEntrees: number;
  totalEntrees: number;
  /** Banques dont la facilité de caisse ne suffit pas sur l'horizon, avec le premier jour de dépassement. */
  depassements: { nom: string; date: string }[];
  /** Banques qui utilisent leur facilité de caisse sans la dépasser, avec le premier jour sous zéro. */
  couvertes: { nom: string; date: string }[];
}

/** Données de la phrase qui résume l'horizon pour le périmètre (opérations retenues par les filtres). */
export function construireResume(banques: PlanBanque[], items: RecaputilatifItem[], filtres: FiltresPlan,
                                 jalons: JalonBanque[]): ResumeGlobal {
  const ids = new Set(banques.map(b => b.banqueId));
  const retenues = items.filter(i => ids.has(i.banqueId ?? null) && garder(i, filtres));
  const somme = (sens: string) => retenues.filter(i => i.sens === sens).reduce((s, i) => s + montantDe(i), 0);
  const premier = (banqueId: number | null, code: Jalon['code']) => jalons.find(j => j.banqueId === banqueId && j.code === code)?.date ?? '';
  return {
    soldeDepart: banques.reduce((s, b) => s + b.soldeActuel, 0),
    soldeFinal: banques.reduce((s, b) => s + b.soldeFinal, 0),
    nombreSorties: retenues.filter(i => i.sens === SensOperation.DECAISSEMENT).length,
    totalSorties: somme(SensOperation.DECAISSEMENT),
    nombreEntrees: retenues.filter(i => i.sens === SensOperation.ENCAISSEMENT).length,
    totalEntrees: somme(SensOperation.ENCAISSEMENT),
    depassements: banques.filter(b => b.besoinFinancement > 0)
      .map(b => ({ nom: b.banqueNom, date: premier(b.banqueId, 'FC_DEPASSEE') || premier(b.banqueId, 'SOUS_ZERO') || b.pointBas.date })),
    couvertes: banques.filter(b => b.besoinFinancement === 0 && b.joursSoldeNegatif > 0)
      .map(b => ({ nom: b.banqueNom, date: premier(b.banqueId, 'SOUS_ZERO') || b.pointBas.date })),
  };
}

// ── Tableau : Banque › période ──────────────────────────────────────────────

export interface LigneTableauGlobal {
  banque: PlanBanque;
  soldes: number[];
  niveaux: NiveauSolde[];
  entrees: number[];
  sorties: number[];
}

/**
 * Une ligne par banque : solde de fin de chaque période et son niveau ; au dépli, entrées et sorties par période —
 * celles du plan sans filtre d'opération, sinon recalculées depuis les opérations retenues.
 */
export function construireTableauGlobal(banques: PlanBanque[], colonnes: ColonnePlan[], items: RecaputilatifItem[],
                                       filtres: FiltresPlan): LigneTableauGlobal[] {
  const avecFiltre = filtreActif(filtres);
  return banques.map(b => {
    let entrees = [...b.totalEncaissements];
    let sorties = [...b.totalDecaissements];
    if (avecFiltre) {
      entrees = colonnes.map(() => 0);
      sorties = colonnes.map(() => 0);
      items.filter(i => i.banqueId === b.banqueId && garder(i, filtres)).forEach(i => {
        const k = indexColonne(i.dateReference, colonnes);
        (i.sens === SensOperation.ENCAISSEMENT ? entrees : sorties)[k] += montantDe(i);
      });
    }
    return { banque: b, soldes: b.soldesFin, niveaux: b.soldesFin.map(s => niveauSolde(s, b.plafondFC)), entrees, sorties };
  });
}
