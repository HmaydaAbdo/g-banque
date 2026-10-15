import { verdictDuPlan } from './verdict';
import { planBanqueFixture, planTresorerieFixture } from './testing/plan-tresorerie.fixture';

describe('verdictDuPlan', () => {
  it('besoin de financement : nomme la pire banque, le jour et le montant (rouge)', () => {
    const v = verdictDuPlan(planTresorerieFixture());
    expect(v.niveau).toBe('danger');
    expect(v.titre).toContain('Banque A');
    expect(v.titre).toContain('20/11');
    expect(v.pastilles[0].niveau).toBe('danger');
  });

  it('plusieurs banques en besoin : la plus touchée est nommée, le nombre est rappelé', () => {
    const plan = planTresorerieFixture();
    plan.banques = [planBanqueFixture(1, 'Banque A', 1000), planBanqueFixture(2, 'Banque B', 5000)];
    const v = verdictDuPlan(plan);
    expect(v.titre).toContain('Banque B');
    expect(v.titre).toContain('+1 banque');
    // Les deux banques en besoin sont nommées, avec leur montant
    const pastilles = v.pastilles.map(p => p.libelle.replace(/[  ]/g, ' '));
    expect(pastilles.some(p => p.startsWith('Banque A · −1 k'))).toBeTrue();
    expect(pastilles.some(p => p.startsWith('Banque B · −5 k'))).toBeTrue();
  });

  it('le total du groupe peut rester couvert : une pastille « Groupe couvert » le signale', () => {
    const plan = planTresorerieFixture();
    plan.banques = [planBanqueFixture(1, 'Banque A', 1000)];
    plan.groupes = [{ ...planBanqueFixture(null, 'Toutes les banques', 0), pointBas: { date: '2026-11-20', solde: 9000, disponibilite: 11000 } }];
    const v = verdictDuPlan(plan);
    expect(v.niveau).toBe('danger');
    expect(v.pastilles.some(p => p.libelle.startsWith('Groupe couvert'))).toBeTrue();
  });

  it('solde négatif couvert par la facilité : orange', () => {
    const plan = planTresorerieFixture();
    plan.banques = [planBanqueFixture(1, 'Banque A', 0)];
    expect(verdictDuPlan(plan).niveau).toBe('warn');
  });

  it('la devise de la banque est reprise dans la phrase (jamais « MAD » en dur)', () => {
    const plan = planTresorerieFixture();
    plan.banques = [{ ...planBanqueFixture(1, 'Banque Euro', 1000), devise: 'EUR' }];
    expect(verdictDuPlan(plan).titre).toContain('EUR');
    expect(verdictDuPlan(plan).titre).not.toContain('MAD');
  });

  it('plusieurs devises : un point bas par devise, jamais additionnées', () => {
    const plan = planTresorerieFixture();
    plan.banques = [{ ...planBanqueFixture(1, 'A', 0), joursSoldeNegatif: 0 }];
    plan.groupes = [planBanqueFixture(null, 'Banques en MAD', 0), { ...planBanqueFixture(null, 'Banques en EUR', 0), devise: 'EUR' }];
    const v = verdictDuPlan(plan);
    expect(v.pastilles.length).toBe(2);
    expect(v.pastilles[0].libelle).toContain('MAD');
    expect(v.pastilles[1].libelle).toContain('EUR');
  });

  it("aucun jour sous zéro : tout va bien jusqu'à l'horizon (vert)", () => {
    const plan = planTresorerieFixture();
    plan.banques = [{ ...planBanqueFixture(1, 'Banque A', 0), joursSoldeNegatif: 0 }];
    const v = verdictDuPlan(plan);
    expect(v.niveau).toBe('ok');
    expect(v.titre).toContain('31/12/2026');
  });
});
