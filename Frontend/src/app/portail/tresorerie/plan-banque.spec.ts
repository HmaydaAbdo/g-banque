import { RecaputilatifItem } from '../etatsDesEncours/views/lesEncours/recap/dtos/RecaputilatifItem';
import { SensOperation } from '../transactions/dtos/SensOperation';
import { TransactionSource } from '../transactions/dtos/TransactionSource';
import { ColonnePlan, PlanBanque } from './dtos/PlanTresorerie';
import {
  colonneDuPointBas, construireEtapes, construireJalons, construireTableau, FILTRES_VIDES, filtreActif, FiltresPlan,
  garder, indexColonne, libellePeriode, niveauSolde, operationsDeCase,
} from './plan-banque';

/**
 * Banque fictive au 17/10 : solde 1 000, plafond FC 500. Colonnes : À ce jour (≤ 17/10), 18-25/10, 26-31/10.
 * Échue : LCN −300 (12/10). 20/10 : virement −900 (solde −200, couvert). 22/10 : décompte +100.
 * 28/10 : LCN −600 (solde −700, FC dépassée) et chèque −50 (solde −750, point bas).
 */
const COLONNES: ColonnePlan[] = [
  { code: 'ECHU', libelle: 'À ce jour', du: null, au: '2026-10-17' },
  { code: 'P1', libelle: 'Du 18/10 au 25/10', du: '2026-10-18', au: '2026-10-25' },
  { code: 'P2', libelle: 'Du 26/10 au 31/10', du: '2026-10-26', au: '2026-10-31' },
];

let id = 0;
function op(source: TransactionSource, sens: SensOperation, date: string, montant: number, libelle = 'op', numero: string | null = null): RecaputilatifItem {
  return {
    id: ++id, dateEmission: date, dateEcheance: null, dateReference: date, libelle, numero, sens,
    debit: (sens === SensOperation.DECAISSEMENT ? montant : null) as never,
    credit: (sens === SensOperation.ENCAISSEMENT ? montant : null) as never,
    observations: '', source, compositeKey: `${source}:${id}`,
  };
}

const D = SensOperation.DECAISSEMENT;
const E = SensOperation.ENCAISSEMENT;
const OPS = [
  op(TransactionSource.LCN_DECAISSEMENT, D, '2026-10-12', 300, 'LCN 1', '8529246'),
  op(TransactionSource.Virement, D, '2026-10-20', 900, 'Fournisseur ciment'),
  op(TransactionSource.DECOMPTES, E, '2026-10-22', 100, 'DP 31'),
  op(TransactionSource.LCN_DECAISSEMENT, D, '2026-10-28', 600, 'LCN 2'),
  op(TransactionSource.CHEQUE, D, '2026-10-28', 50, 'Carburant'),
];

const PLAN: PlanBanque = {
  banqueId: 3, banqueNom: 'BMCI', devise: 'MAD', soldeActuel: 1000, plafondFC: 500,
  encaissements: [{ famille: 'Autres', categorie: 'Décompte', montants: [0, 100, 0], total: 100, sources: [TransactionSource.DECOMPTES] }],
  decaissements: [{
    famille: 'Règlements', categorie: 'REGLEMENT FRNS', montants: [300, 900, 650], total: 1850,
    sources: [TransactionSource.Virement, TransactionSource.LCN_DECAISSEMENT, TransactionSource.CHEQUE],
  }],
  totalEncaissements: [0, 100, 0], totalDecaissements: [300, 900, 650],
  soldesFin: [700, -100, -750], disponibilites: [1200, 400, -250], soldeFinal: -750, disponibiliteFinale: -250,
  pointBas: { date: '2026-10-28', solde: -750, disponibilite: -250 }, besoinFinancement: 250, joursSoldeNegatif: 12,
  courbe: [
    { date: '2026-10-17', solde: 700 }, { date: '2026-10-20', solde: -200 }, { date: '2026-10-22', solde: -100 },
    { date: '2026-10-28', solde: -750 }, { date: '2026-10-31', solde: -750 },
  ],
  plusGrosMouvements: [], enBesoinDepuis: null,
};

describe('plan-banque', () => {

  it('range une date comme le backend : aujourd\'hui et avant dans « À ce jour », au-delà de l\'horizon dans la dernière', () => {
    expect(indexColonne('2026-10-12', COLONNES)).toBe(0);
    expect(indexColonne('2026-10-17', COLONNES)).toBe(0);
    expect(indexColonne('2026-10-18', COLONNES)).toBe(1);
    expect(indexColonne('2026-10-31', COLONNES)).toBe(2);
    expect(indexColonne('2026-12-01', COLONNES)).toBe(2);
  });

  it('filtre par sens, source, montant avec opérateur et texte (libellé ou numéro)', () => {
    const f = (p: Partial<FiltresPlan>) => ({ ...FILTRES_VIDES, ...p });
    expect(filtreActif(FILTRES_VIDES)).toBeFalse();
    expect(OPS.filter(o => garder(o, f({ sens: 'ENCAISSEMENT' }))).length).toBe(1);
    expect(OPS.filter(o => garder(o, f({ sources: [TransactionSource.LCN_DECAISSEMENT] }))).length).toBe(2);
    expect(OPS.filter(o => garder(o, f({ operateur: '>=', montant: 600 }))).length).toBe(2);
    expect(OPS.filter(o => garder(o, f({ operateur: '<', montant: 100 }))).length).toBe(1);
    expect(OPS.filter(o => garder(o, f({ operateur: '=', montant: 900 }))).length).toBe(1);
    expect(OPS.filter(o => garder(o, f({ texte: '8529' }))).length).toBe(1);
    expect(OPS.filter(o => garder(o, f({ texte: 'ciment' }))).length).toBe(1);
  });

  it('jalons : aujourd\'hui, sous zéro (couvert), FC dépassée avec sa cause, point bas — dans l\'ordre des dates', () => {
    const jalons = construireJalons(PLAN, COLONNES, OPS);

    expect(jalons.map(j => j.code)).toEqual(['AUJOURDHUI', 'SOUS_ZERO', 'FC_DEPASSEE', 'POINT_BAS']);
    const sousZero = jalons[1];
    expect(sousZero.date).toBe('2026-10-20');
    expect(sousZero.colonne).toBe(1);
    expect(sousZero.niveau).toBe('warn');
    expect(sousZero.texte).toContain('Fournisseur ciment');
    const fc = jalons[2];
    expect(fc.date).toBe('2026-10-28');
    expect(fc.niveau).toBe('danger');
    expect(fc.causes).toEqual([`${TransactionSource.LCN_DECAISSEMENT}:${OPS[3].id}`]);
    expect(jalons[3].texte).toContain('besoin non couvert 250,00');
  });

  it('sans facilité de caisse : passer sous zéro est déjà un besoin, pas de jalon de dépassement', () => {
    const jalons = construireJalons({ ...PLAN, plafondFC: 0 }, COLONNES, OPS);
    expect(jalons.map(j => j.code)).toEqual(['AUJOURDHUI', 'SOUS_ZERO', 'POINT_BAS']);
    expect(jalons[1].niveau).toBe('danger');
  });

  it('étapes : soldes du backend, pastilles de toutes les sources, opérations filtrées groupées par sens puis source', () => {
    const etapes = construireEtapes(PLAN, COLONNES, OPS, { ...FILTRES_VIDES, sources: [TransactionSource.LCN_DECAISSEMENT] });

    expect(etapes.map(e => [e.ouverture, e.cloture, e.niveau])).toEqual([
      [1000, 700, 'positif'], [700, -100, 'couvert'], [-100, -750, 'depasse'],
    ]);
    const derniere = etapes[2];
    expect(derniere.pastilles.map(p => [p.libelle, p.total, p.retenue])).toEqual([
      ['LCN Décaissement', 600, true], ['Chèque', 50, false],
    ]);
    expect(derniere.nombreFiltrees).toBe(1);
    expect(derniere.sortiesFiltrees?.sources[0].operations[0].libelle).toBe('LCN 2');
    expect(etapes[1].nombreFiltrees).toBe(0);
    expect(colonneDuPointBas(PLAN, COLONNES)).toBe('P2');
  });

  it('niveau du solde : positif, couvert par la FC, au-delà du plafond', () => {
    expect(niveauSolde(10, 500)).toBe('positif');
    expect(niveauSolde(-500, 500)).toBe('couvert');
    expect(niveauSolde(-501, 500)).toBe('depasse');
    expect(niveauSolde(-1, 0)).toBe('depasse');
  });

  it('tableau : catégorie puis ses sources ; sans filtre les totaux sont ceux du plan, avec filtre ils suivent les opérations', () => {
    const complet = construireTableau(PLAN, COLONNES, OPS, FILTRES_VIDES);
    expect(complet.sorties.map(l => [l.niveau, l.libelle, l.montants])).toEqual([
      [1, 'REGLEMENT FRNS', [300, 900, 650]],
      [2, 'Virement', [0, 900, 0]],
      [2, 'LCN Décaissement', [300, 0, 600]],
      [2, 'Chèque', [0, 0, 50]],
    ]);
    expect(complet.totalSorties).toEqual(PLAN.totalDecaissements);
    expect(complet.totalEntrees).toEqual(PLAN.totalEncaissements);

    const filtre = construireTableau(PLAN, COLONNES, OPS, { ...FILTRES_VIDES, operateur: '>', montant: 500 });
    expect(filtre.entrees).toEqual([]);
    expect(filtre.sorties.map(l => l.libelle)).toEqual(['REGLEMENT FRNS', 'Virement', 'LCN Décaissement']);
    expect(filtre.totalSorties).toEqual([0, 900, 600]);

    expect(operationsDeCase(OPS, COLONNES, FILTRES_VIDES, D, TransactionSource.LCN_DECAISSEMENT, 0).map(o => o.libelle)).toEqual(['LCN 1']);
  });

  it('libellés de période : à ce jour, semaine, jour seul, mois', () => {
    expect(libellePeriode(COLONNES[0], 0)).toEqual({ titre: 'À ce jour', sous: 'échues et du jour' });
    expect(libellePeriode(COLONNES[1], 1)).toEqual({ titre: '18 – 25 oct.', sous: 'semaine 42' });
    expect(libellePeriode({ code: 'P1', libelle: 'Du 18/10 au 18/10', du: '2026-10-18', au: '2026-10-18' }, 1))
      .toEqual({ titre: '18 oct.', sous: 'un seul jour' });
    expect(libellePeriode({ code: 'P2', libelle: 'Novembre 2026', du: '2026-11-01', au: '2026-11-30' }, 2))
      .toEqual({ titre: 'Novembre 2026', sous: '1 – 30 nov.' });
  });

  it("catégorie d'une seule source : une seule ligne, détaillable directement", () => {
    const t = construireTableau(PLAN, COLONNES, OPS, FILTRES_VIDES);
    expect(t.entrees.map(l => [l.niveau, l.libelle, l.source])).toEqual([[1, 'Décompte', TransactionSource.DECOMPTES]]);
  });
});
