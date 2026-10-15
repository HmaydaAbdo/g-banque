import { construireCroisement } from './croisement';
import { planBanqueFixture } from './testing/plan-tresorerie.fixture';

describe('construireCroisement', () => {
  // Banque A : +1000 règlements (période 1) ; −6000 leasing et −3000 agios (période 2)
  const banques = () => [
    planBanqueFixture(1, 'Banque A'),
    planBanqueFixture(2, 'Banque B'),
    { ...planBanqueFixture(3, 'Banque Euro'), devise: 'EUR' },
  ];

  it('chaque banque est suivie de ses sources ; le niveau 1 est le net, encaissements positifs et décaissements négatifs', () => {
    const lignes = construireCroisement(banques(), 'MAD');

    expect(lignes.filter(l => l.niveau === 1).map(l => l.libelle)).toEqual(['Banque A', 'Banque B']);
    const a = lignes[0];
    expect(a.valeurs).toEqual([1000, -9000]);
    expect(a.total).toBe(-8000);
    expect(a.enfants).toBe(3);
    // Sources triées par famille : Règlements avant Financements
    expect(lignes.filter(l => l.parent === 'b:1').map(l => `${l.libelle}:${l.total}`))
      .toEqual(['REGLEMENT FRNS:1000', 'Leasing:-6000', 'Agios:-3000']);
  });

  it('une source porte son sens et sa famille', () => {
    const leasing = construireCroisement(banques(), 'MAD').find(l => l.libelle === 'Leasing')!;
    expect(leasing.sens).toBe('DECAISSEMENT');
    expect(leasing.famille).toBe('Financements');
  });

  it('on ne retient que les banques de la devise demandée', () => {
    expect(construireCroisement(banques(), 'EUR').filter(l => l.niveau === 1).map(l => l.libelle)).toEqual(['Banque Euro']);
    expect(construireCroisement(banques(), 'USD')).toEqual([]);
  });
});
