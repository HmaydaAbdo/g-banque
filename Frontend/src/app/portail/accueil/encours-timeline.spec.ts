import { bornesDesPeriodes, construireTimeline } from './encours-timeline';
import { RecaputilatifItem } from '../etatsDesEncours/views/lesEncours/recap/dtos/RecaputilatifItem';
import { SensOperation } from '../transactions/dtos/SensOperation';
import { TransactionSource } from '../transactions/dtos/TransactionSource';

describe('encours-timeline', () => {
  // Samedi 17 octobre 2026
  const aujourdhui = new Date(2026, 9, 17);

  function item(source: TransactionSource, sens: SensOperation, date: string, montant: number, libelle = 'op'): RecaputilatifItem {
    return {
      id: 1, dateEmission: date, dateEcheance: null, dateReference: date, libelle, numero: null, sens,
      debit: (sens === SensOperation.DECAISSEMENT ? montant : 0) as never,
      credit: (sens === SensOperation.ENCAISSEMENT ? montant : 0) as never,
      observations: '', source, compositeKey: `${source}:1`,
    };
  }

  it('découpe la fin de mois par semaine, la première commençant aujourd\'hui et la dernière finissant le 31', () => {
    const bornes = bornesDesPeriodes(aujourdhui, 'FIN_DE_MOIS').map(b => [b.debut.getDate(), b.fin.getDate()]);
    expect(bornes).toEqual([[17, 18], [19, 25], [26, 31]]);
  });

  it('découpe la fin de trimestre par mois civil, le premier commençant aujourd\'hui', () => {
    const bornes = bornesDesPeriodes(aujourdhui, 'FIN_DE_TRIMESTRE').map(b => [b.debut.getMonth() + 1, b.debut.getDate(), b.fin.getMonth() + 1, b.fin.getDate()]);
    expect(bornes).toEqual([[10, 17, 10, 31], [11, 1, 11, 30], [12, 1, 12, 31]]);
  });

  it('range les opérations par période, puis par sens, puis par source (plus gros total d\'abord)', () => {
    const timeline = construireTimeline([
      item(TransactionSource.LCN_ENCAISSEMENT, SensOperation.ENCAISSEMENT, '2026-10-12', 1000),
      item(TransactionSource.Virement, SensOperation.DECAISSEMENT, '2026-10-16', 300),
      item(TransactionSource.DECOMPTES, SensOperation.ENCAISSEMENT, '2026-10-18', 5000),
      item(TransactionSource.LEASING, SensOperation.DECAISSEMENT, '2026-10-20', 100),
      item(TransactionSource.AGIOS, SensOperation.DECAISSEMENT, '2026-10-22', 400),
      item(TransactionSource.AGIOS, SensOperation.DECAISSEMENT, '2026-10-23', 50),
      item(TransactionSource.COMMISSION, SensOperation.DECAISSEMENT, '2026-10-31', 20),
    ], aujourdhui, 'FIN_DE_MOIS');

    expect(timeline.map(p => p.titre)).toEqual(['Échues', '17 – 18 oct.', '19 – 25 oct.', '26 – 31 oct.']);
    expect(timeline[0].echue).toBeTrue();
    expect(timeline[0].net).toBe(700);
    expect(timeline[1].entrees?.total).toBe(5000);
    expect(timeline[1].sorties).toBeNull();

    const semaine = timeline[2];
    expect(semaine.entrees).toBeNull();
    expect(semaine.sorties?.nombre).toBe(3);
    expect(semaine.sorties?.sources.map(s => [s.libelle, s.total])).toEqual([['Agios', 450], ['Leasing', 100]]);
    expect(semaine.net).toBe(-550);
  });

  it("range par date de référence (pas par échéance, vide pour la plupart des sources) et tolère un libellé vide", () => {
    const sansLibelle = { ...item(TransactionSource.CHEQUE, SensOperation.DECAISSEMENT, '2026-10-20', 10), libelle: null, observations: 'CHQ 12' };
    const avecLibelle = item(TransactionSource.CHEQUE, SensOperation.DECAISSEMENT, '2026-10-20', 20, 'B');
    const t = construireTimeline([avecLibelle, sansLibelle], aujourdhui, 'FIN_DE_MOIS');

    expect(t.map(p => p.titre)).toEqual(['19 – 25 oct.']);
    expect(t[0].sorties?.sources[0].operations.map(o => o.libelle)).toEqual(['B', 'CHQ 12']);
  });

  it('omet les périodes sans opération et reste vide sans opération', () => {
    expect(construireTimeline([], aujourdhui, 'FIN_DE_MOIS')).toEqual([]);
    const t = construireTimeline([item(TransactionSource.LEASING, SensOperation.DECAISSEMENT, '2026-10-30', 10)], aujourdhui, 'FIN_DE_MOIS');
    expect(t.map(p => p.titre)).toEqual(['26 – 31 oct.']);
  });

  it('une opération datée après l\'horizon rejoint la dernière période', () => {
    const t = construireTimeline([item(TransactionSource.LEASING, SensOperation.DECAISSEMENT, '2026-12-30', 10)], aujourdhui, 'FIN_DE_MOIS');
    expect(t.map(p => p.titre)).toEqual(['26 – 31 oct.']);
  });

  it('en fin de trimestre, une période porte le mois et la plage', () => {
    const t = construireTimeline([item(TransactionSource.LEASING, SensOperation.DECAISSEMENT, '2026-11-15', 10)], aujourdhui, 'FIN_DE_TRIMESTRE');
    expect(t[0].titre).toBe('Novembre 2026');
    expect(t[0].precision).toBe('1 – 30 nov.');
  });
});
