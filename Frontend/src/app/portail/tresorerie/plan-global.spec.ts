import { RecaputilatifItem } from '../etatsDesEncours/views/lesEncours/recap/dtos/RecaputilatifItem';
import { SensOperation } from '../transactions/dtos/SensOperation';
import { TransactionSource } from '../transactions/dtos/TransactionSource';
import { ColonnePlan, PlanBanque } from './dtos/PlanTresorerie';
import { FILTRES_VIDES } from './plan-banque';
import {
  banquesDuPerimetre, construireEtapesGlobales, construireJalonsGlobaux, construireResume, construireTableauGlobal,
  FILTRES_GLOBAL_VIDES, filtreGlobalActif,
} from './plan-global';

/** Au 17/10 : A (plafond 500) dépasse le 28/10 ; B (plafond 200) passe sous zéro le 20/10, couvert ; C reste positive. */
const COLONNES: ColonnePlan[] = [
  { code: 'ECHU', libelle: 'À ce jour', du: null, au: '2026-10-17' },
  { code: 'P1', libelle: 'Du 18/10 au 25/10', du: '2026-10-18', au: '2026-10-25' },
  { code: 'P2', libelle: 'Du 26/10 au 31/10', du: '2026-10-26', au: '2026-10-31' },
];

function plan(id: number, nom: string, solde: number, plafond: number, fins: number[], enc: number[], dec: number[],
              courbe: { date: string; solde: number }[], devise = 'MAD'): PlanBanque {
  const bas = courbe.reduce((m, p) => (p.solde < m.solde ? p : m), courbe[0]);
  return {
    banqueId: id, banqueNom: nom, devise, soldeActuel: solde, plafondFC: plafond, encaissements: [], decaissements: [],
    totalEncaissements: enc, totalDecaissements: dec, soldesFin: fins, disponibilites: fins.map(f => f + plafond),
    soldeFinal: fins[fins.length - 1], disponibiliteFinale: fins[fins.length - 1] + plafond,
    pointBas: { date: bas.date, solde: bas.solde, disponibilite: bas.solde + plafond },
    besoinFinancement: Math.max(0, -(bas.solde + plafond)), joursSoldeNegatif: courbe.filter(p => p.solde < 0).length,
    courbe, plusGrosMouvements: [], enBesoinDepuis: null,
  };
}

const A = plan(1, 'A', 1000, 500, [700, 600, -750], [0, 0, 0], [300, 100, 1350],
  [{ date: '2026-10-17', solde: 700 }, { date: '2026-10-28', solde: -750 }]);
const B = plan(2, 'B', 300, 200, [300, -100, -150], [0, 0, 0], [0, 400, 50],
  [{ date: '2026-10-17', solde: 300 }, { date: '2026-10-20', solde: -100 }, { date: '2026-10-30', solde: -150 }]);
const C = plan(3, 'C', 50, 0, [50, 80, 90], [0, 30, 10], [0, 0, 0], [{ date: '2026-10-17', solde: 50 }]);
const EUR = plan(4, 'E', 10, 0, [10, 10, 10], [0, 0, 0], [0, 0, 0], [{ date: '2026-10-17', solde: 10 }], 'EUR');

let id = 0;
function op(banqueId: number, source: TransactionSource, sens: SensOperation, date: string, montant: number, libelle = 'op'): RecaputilatifItem {
  return {
    id: ++id, banqueId, dateEmission: date, dateEcheance: null, dateReference: date, libelle, numero: null, sens,
    debit: (sens === SensOperation.DECAISSEMENT ? montant : null) as never,
    credit: (sens === SensOperation.ENCAISSEMENT ? montant : null) as never,
    observations: '', source, compositeKey: `${source}:${id}`,
  };
}

const D = SensOperation.DECAISSEMENT;
const OPS = [
  op(1, TransactionSource.LCN_DECAISSEMENT, D, '2026-10-12', 300),
  op(1, TransactionSource.Virement, D, '2026-10-22', 100),
  op(1, TransactionSource.LCN_DECAISSEMENT, D, '2026-10-28', 1350, 'LCN Atlas'),
  op(2, TransactionSource.LEASING, D, '2026-10-20', 400, 'Loyer engins'),
  op(2, TransactionSource.LEASING, D, '2026-10-30', 50),
  op(3, TransactionSource.DECOMPTES, SensOperation.ENCAISSEMENT, '2026-10-24', 30),
  op(3, TransactionSource.DECOMPTES, SensOperation.ENCAISSEMENT, '2026-10-29', 10),
];

describe('plan-global', () => {
  const TOUTES = [A, B, C, EUR];

  it('périmètre : les banques de la devise, restreintes au filtre « Banques »', () => {
    expect(banquesDuPerimetre(TOUTES, 'MAD', []).map(b => b.banqueNom)).toEqual(['A', 'B', 'C']);
    expect(banquesDuPerimetre(TOUTES, 'MAD', [2]).map(b => b.banqueNom)).toEqual(['B']);
    expect(banquesDuPerimetre(TOUTES, 'EUR', []).map(b => b.banqueNom)).toEqual(['E']);
    expect(filtreGlobalActif(FILTRES_GLOBAL_VIDES)).toBeFalse();
    expect(filtreGlobalActif({ ...FILTRES_GLOBAL_VIDES, banques: [1] })).toBeTrue();
  });

  it('étapes : solde du groupe = somme des banques, compte par niveau, banques de la plus urgente à la plus sereine', () => {
    const etapes = construireEtapesGlobales([A, B, C], COLONNES, OPS, FILTRES_VIDES);

    expect(etapes.map(e => [e.ouverture, e.cloture])).toEqual([[1350, 1050], [1050, 580], [580, -810]]);
    expect(etapes[1].compte).toEqual({ depasse: 0, couvert: 1, positif: 2 });
    expect(etapes[2].compte).toEqual({ depasse: 1, couvert: 1, positif: 1 });
    expect(etapes[2].banques.map(l => [l.banque.banqueNom, l.niveau])).toEqual([['A', 'depasse'], ['B', 'couvert'], ['C', 'positif']]);
    expect(etapes[1].banques.find(l => l.banque.banqueNom === 'C')!.flux).toBe(30);
  });

  it('les filtres d\'opération comptent les opérations retenues par banque, sans toucher aux soldes', () => {
    const etapes = construireEtapesGlobales([A, B, C], COLONNES, OPS, { ...FILTRES_VIDES, sources: [TransactionSource.LEASING] });
    expect(etapes[1].banques.map(l => [l.banque.banqueNom, l.nombreFiltrees])).toEqual([['B', 1], ['C', 0], ['A', 0]]);
    expect(etapes[1].cloture).toBe(580);
  });

  it('jalons par banque, dans l\'ordre des dates, avec l\'opération qui les provoque', () => {
    const jalons = construireJalonsGlobaux([A, B, C], COLONNES, OPS);
    expect(jalons.map(j => [j.banqueNom, j.code, j.date])).toEqual([
      ['B', 'SOUS_ZERO', '2026-10-20'],
      ['A', 'FC_DEPASSEE', '2026-10-28'],
      ['A', 'SOUS_ZERO', '2026-10-28'],
    ]);
    expect(jalons[0].texte).toContain('Loyer engins');
    expect(jalons[1].texte).toContain('LCN Atlas');
  });

  it('résumé : départ, sorties et entrées retenues, banques en dépassement et couvertes avec leur date', () => {
    const jalons = construireJalonsGlobaux([A, B, C], COLONNES, OPS);
    const r = construireResume([A, B, C], OPS, FILTRES_VIDES, jalons);
    expect(r.soldeDepart).toBe(1350);
    expect(r.soldeFinal).toBe(-810);
    expect([r.nombreSorties, r.totalSorties, r.nombreEntrees, r.totalEntrees]).toEqual([5, 2200, 2, 40]);
    expect(r.depassements).toEqual([{ nom: 'A', date: '2026-10-28' }]);
    expect(r.couvertes).toEqual([{ nom: 'B', date: '2026-10-20' }]);
  });

  it('tableau : soldes et niveaux par période ; entrées/sorties du plan, ou des opérations retenues avec un filtre', () => {
    const complet = construireTableauGlobal([A, B, C], COLONNES, OPS, FILTRES_VIDES);
    expect(complet[0].niveaux).toEqual(['positif', 'positif', 'depasse']);
    expect(complet[0].sorties).toEqual([300, 100, 1350]);

    const filtre = construireTableauGlobal([A, B, C], COLONNES, OPS, { ...FILTRES_VIDES, operateur: '>', montant: 500 });
    expect(filtre[0].sorties).toEqual([0, 0, 1350]);
    expect(filtre[0].soldes).toEqual(A.soldesFin);
  });
});
