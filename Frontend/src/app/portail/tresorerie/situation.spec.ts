import { joursEntre, niveauSituation, ordonnerParUrgence } from './situation';
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

  it('les jours entre deux dates ne dépendent pas du fuseau horaire', () => {
    expect(joursEntre('2026-10-12', '2026-10-15')).toBe(3);
    expect(joursEntre('2026-10-15', '2026-10-15')).toBe(0);
  });
});
