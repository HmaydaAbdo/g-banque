import { construireCroisement, totalCroisement } from './croisement';
import { planBanqueFixture } from './testing/plan-tresorerie.fixture';

describe('construireCroisement', () => {
  // Banque A : +1000 règlements (période 1) ; −6000 leasing et −3000 agios (période 2)
  // Banque B : même fixture, devise EUR
  const banques = () => [
    planBanqueFixture(1, 'Banque A'),
    planBanqueFixture(2, 'Banque B'),
    { ...planBanqueFixture(3, 'Banque Euro'), devise: 'EUR' },
  ];

  it('banque puis source : le niveau 1 est le net de ses sources, encaissements positifs et décaissements négatifs', () => {
    const lignes = construireCroisement(banques(), 'MAD', 'BANQUE_SOURCE');

    expect(lignes.filter(l => l.niveau === 1).map(l => l.libelle)).toEqual(['Banque A', 'Banque B']);
    const a = lignes[0];
    expect(a.valeurs).toEqual([1000, -9000]);
    expect(a.total).toBe(-8000);
    expect(a.enfants).toBe(3);
    // Sources triées par famille : Règlements avant Financements
    expect(lignes.filter(l => l.parent === 'b:1').map(l => `${l.libelle}:${l.total}`))
      .toEqual(['REGLEMENT FRNS:1000', 'Leasing:-6000', 'Agios:-3000']);
  });

  it("source puis banque : le niveau 1 regroupe les banques, et chaque source donne ses banques", () => {
    const lignes = construireCroisement(banques(), 'MAD', 'SOURCE_BANQUE');
    const niveau1 = lignes.filter(l => l.niveau === 1);

    expect(niveau1.map(l => l.libelle)).toEqual(['REGLEMENT FRNS', 'Leasing', 'Agios']);
    const leasing = niveau1[1];
    expect(leasing.valeurs).toEqual([0, -12000]);
    expect(leasing.enfants).toBe(2);
    expect(lignes.filter(l => l.parent === leasing.cle).map(l => `${l.libelle}:${l.banqueId}:${l.total}`))
      .toEqual(['Banque A:1:-6000', 'Banque B:2:-6000']);
  });

  it('le total est le même quel que soit l\'ordre, et ne compte que la devise demandée', () => {
    const n = 2;
    const a = totalCroisement(construireCroisement(banques(), 'MAD', 'BANQUE_SOURCE'), n);
    const b = totalCroisement(construireCroisement(banques(), 'MAD', 'SOURCE_BANQUE'), n);
    expect(a).toEqual([2000, -18000]);
    expect(b).toEqual(a);
    expect(totalCroisement(construireCroisement(banques(), 'EUR', 'BANQUE_SOURCE'), n)).toEqual([1000, -9000]);
  });

  it('une devise sans banque ne produit aucune ligne', () => {
    expect(construireCroisement(banques(), 'USD', 'BANQUE_SOURCE')).toEqual([]);
  });
});
