import { finHorizon, horizonAuDemarrage, horizonDepuisParam } from './PeriodeRecap';

describe('PeriodeRecap', () => {
  const le14octobre = new Date(2026, 9, 14);

  it('fin de mois : dernier jour du mois en cours', () => {
    expect(finHorizon('FIN_DE_MOIS', le14octobre)).toEqual(new Date(2026, 9, 31));
    expect(finHorizon('FIN_DE_MOIS', new Date(2028, 1, 3))).toEqual(new Date(2028, 1, 29));
  });

  it('fin de trimestre : dernier jour du trimestre civil en cours', () => {
    expect(finHorizon('FIN_DE_TRIMESTRE', le14octobre)).toEqual(new Date(2026, 11, 31));
    expect(finHorizon('FIN_DE_TRIMESTRE', new Date(2026, 3, 1))).toEqual(new Date(2026, 5, 30));
    expect(finHorizon('FIN_DE_TRIMESTRE', new Date(2026, 2, 31))).toEqual(new Date(2026, 2, 31));
  });

  it('à ce jour : le jour même', () => {
    expect(finHorizon('A_CE_JOUR', le14octobre)).toEqual(le14octobre);
  });

  it("lit l'horizon d'une adresse, toute autre valeur retombant sur la fin de mois", () => {
    expect(horizonDepuisParam('FIN_DE_TRIMESTRE')).toBe('FIN_DE_TRIMESTRE');
    expect(horizonDepuisParam('A_CE_JOUR')).toBe('FIN_DE_MOIS');
    expect(horizonDepuisParam(null)).toBe('FIN_DE_MOIS');
  });

  it("un rechargement de la page revient à la fin de mois, sinon l'adresse fait foi", () => {
    expect(horizonAuDemarrage('FIN_DE_TRIMESTRE', true)).toBe('FIN_DE_MOIS');
    expect(horizonAuDemarrage('FIN_DE_TRIMESTRE', false)).toBe('FIN_DE_TRIMESTRE');
    expect(horizonAuDemarrage(null, false)).toBe('FIN_DE_MOIS');
  });
});
