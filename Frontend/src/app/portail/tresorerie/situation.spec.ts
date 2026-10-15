import { compterParSituation, excedent, joursEntre, niveauSituation, ordonnerParUrgence, partFacilite } from './situation';
import { planBanqueFixture } from './testing/plan-tresorerie.fixture';

describe('situation', () => {

  const enBesoin = (id: number, nom: string, besoin: number) => planBanqueFixture(id, nom, besoin);
  const facilite = (id: number, nom: string, solde: number) =>
    ({ ...planBanqueFixture(id, nom, 0), pointBas: { date: '2026-11-20', solde, disponibilite: 500 } });
  const saine = (id: number, nom: string, solde: number) =>
    ({ ...planBanqueFixture(id, nom, 0), joursSoldeNegatif: 0, pointBas: { date: '2026-11-20', solde, disponibilite: solde + 2000 } });

  it('besoin (rouge), facilité de caisse utilisée (orange), sinon sain (vert)', () => {
    expect(niveauSituation(enBesoin(1, 'A', 1000))).toBe('danger');
    expect(niveauSituation(facilite(2, 'B', -500))).toBe('warn');
    expect(niveauSituation(saine(3, 'C', 9000))).toBe('ok');
  });

  it('les banques sont classées par urgence : le plus gros besoin, puis la facilité utilisée, puis les saines par nom', () => {
    const ordre = ordonnerParUrgence([
      saine(4, 'Zeta', 100), enBesoin(1, 'A', 1000), saine(5, 'Alpha', 100), facilite(2, 'B', -500), enBesoin(3, 'C', 5000),
    ]).map(b => b.banqueNom);
    expect(ordre).toEqual(['C', 'A', 'B', 'Alpha', 'Zeta']);
  });

  it('la synthèse compte les banques par situation', () => {
    expect(compterParSituation([enBesoin(1, 'A', 1), enBesoin(2, 'B', 1), facilite(3, 'C', -1), saine(4, 'D', 1)]))
      .toEqual({ danger: 2, warn: 1, ok: 1 });
  });

  it("la part de facilité de caisse utilisée est bornée à 100 % et nulle sans facilité", () => {
    expect(partFacilite(facilite(1, 'A', -1000))).toBe(0.5);
    expect(partFacilite(facilite(1, 'A', -9000))).toBe(1);
    expect(partFacilite({ ...saine(1, 'A', 100), plafondFC: 0 })).toBe(0);
  });

  it("l'excédent n'existe que pour une banque saine : son solde le plus bas", () => {
    expect(excedent(saine(1, 'A', 9000))).toBe(9000);
    expect(excedent(enBesoin(2, 'B', 1000))).toBe(0);
  });

  it('les jours entre deux dates ne dépendent pas du fuseau horaire', () => {
    expect(joursEntre('2026-10-12', '2026-10-15')).toBe(3);
    expect(joursEntre('2026-10-15', '2026-10-15')).toBe(0);
  });
});
