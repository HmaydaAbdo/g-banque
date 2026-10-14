import { PlanBanque, PlanTresorerie } from '../dtos/PlanTresorerie';

/** Plan d'une banque sur deux colonnes (« À ce jour » + novembre), point bas négatif non couvert. */
export function planBanqueFixture(banqueId: number | null, nom: string, besoin = 1000): PlanBanque {
  return {
    banqueId,
    banqueNom: nom,
    soldeActuel: 5000,
    plafondFC: 2000,
    encaissements: [{ categorie: 'REGLEMENT FRNS', montants: [1000, 0], total: 1000 }],
    decaissements: [{ categorie: 'Leasing', montants: [0, 9000], total: 9000 }],
    totalEncaissements: [1000, 0],
    totalDecaissements: [0, 9000],
    soldesFin: [6000, -3000],
    disponibilites: [8000, -1000],
    soldeFinal: -3000,
    disponibiliteFinale: -1000,
    pointBas: { date: '2026-11-20', solde: -3000, disponibilite: -1000 },
    besoinFinancement: besoin,
    joursSoldeNegatif: 11,
    courbe: [
      { date: '2026-10-14', solde: 6000 },
      { date: '2026-11-20', solde: -3000 },
    ],
  };
}

export function planTresorerieFixture(): PlanTresorerie {
  return {
    periode: 'FIN_DE_TRIMESTRE',
    dateCalcul: '2026-10-14',
    dateHorizon: '2026-12-31',
    colonnes: [
      { code: 'ECHU', libelle: 'À ce jour', du: null, au: '2026-10-14' },
      { code: 'P1', libelle: 'Novembre 2026', du: '2026-11-01', au: '2026-11-30' },
    ],
    consolide: planBanqueFixture(null, 'Toutes les banques', 0),
    banques: [planBanqueFixture(1, 'Banque A'), planBanqueFixture(2, 'Banque B', 0)],
  };
}
