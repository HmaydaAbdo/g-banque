import { situationFc } from './fc-consommation';

describe('situationFc', () => {
  it('solde positif : disponibilité = solde + plafond, FC intacte', () => {
    const s = situationFc(2_500_000, 8_000_000);
    expect(s.statut).toBe('DISPONIBILITE');
    expect(s.montantTotal).toBe(10_500_000);
    expect(s.utilise).toBe(0);
    expect(s.reste).toBe(8_000_000);
    expect(s.pourcentage).toBe(0);
    expect(s.palier).toBe('aucun');
  });

  it('découvert sous 70 % du plafond : besoin couvert par la FC, palier faible', () => {
    const s = situationFc(-4_000_000, 10_000_000);
    expect(s.statut).toBe('BESOIN_COUVERT');
    expect(s.montantTotal).toBe(4_000_000);
    expect(s.pourcentage).toBe(40);
    expect(s.largeur).toBe(40);
    expect(s.reste).toBe(6_000_000);
    expect(s.palier).toBe('faible');
  });

  it('70 % pile : palier élevé ; 100 % pile : encore couvert', () => {
    expect(situationFc(-7_000_000, 10_000_000).palier).toBe('eleve');
    const plafond = situationFc(-10_000_000, 10_000_000);
    expect(plafond.statut).toBe('BESOIN_COUVERT');
    expect(plafond.palier).toBe('eleve');
    expect(plafond.reste).toBe(0);
  });

  it('au-delà du plafond : besoin en dépassement pour la part non couverte, barre pleine', () => {
    const s = situationFc(-6_350_000, 5_000_000);
    expect(s.statut).toBe('BESOIN_DEPASSEMENT');
    expect(s.montantTotal).toBe(1_350_000);
    expect(s.pourcentage).toBe(127);
    expect(s.largeur).toBe(100);
    expect(s.utilise).toBe(5_000_000);
    expect(s.reste).toBe(0);
    expect(s.palier).toBe('depasse');
  });

  it('sans plafond : un solde négatif est un dépassement, un solde positif une disponibilité', () => {
    expect(situationFc(-100, undefined).statut).toBe('BESOIN_DEPASSEMENT');
    expect(situationFc(-100, 0).montantTotal).toBe(100);
    expect(situationFc(100, null).statut).toBe('DISPONIBILITE');
  });
});
