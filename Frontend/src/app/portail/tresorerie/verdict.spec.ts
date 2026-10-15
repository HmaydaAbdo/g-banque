import { verdictDuPlan } from './verdict';
import { planBanqueFixture, planTresorerieFixture } from './testing/plan-tresorerie.fixture';

describe('verdictDuPlan', () => {
  it('besoin de financement : nomme la pire banque, le jour et le montant (rouge)', () => {
    const v = verdictDuPlan(planTresorerieFixture());
    expect(v.niveau).toBe('danger');
    expect(v.titre).toContain('Banque A');
    expect(v.titre).toContain('20/11/2026');
    expect(v.detail).toContain('facilité de caisse');
  });

  it('plusieurs banques en besoin : la plus touchée est nommée, le nombre est rappelé', () => {
    const plan = planTresorerieFixture();
    plan.banques = [planBanqueFixture(1, 'Banque A', 1000), planBanqueFixture(2, 'Banque B', 5000)];
    const v = verdictDuPlan(plan);
    expect(v.titre).toContain('Banque B');
    expect(v.titre).toContain('et 1 autre banque');
    // Les deux banques en besoin sont nommées, avec leur montant
    const detail = v.detail.replace(/[  ]/g, ' ');
    expect(detail).toContain('Banque A : 1 000,00 MAD');
    expect(detail).toContain('Banque B : 5 000,00 MAD');
  });

  it('le total du groupe peut rester couvert : le verdict le dit, une facilité ne couvrant que sa banque', () => {
    const plan = planTresorerieFixture();
    plan.banques = [planBanqueFixture(1, 'Banque A', 1000)];
    plan.groupes = [{ ...planBanqueFixture(null, 'Toutes les banques', 0), pointBas: { date: '2026-11-20', solde: 9000, disponibilite: 11000 } }];
    const v = verdictDuPlan(plan);
    expect(v.niveau).toBe('danger');
    expect(v.detail).toContain('Le total du groupe reste couvert');
    expect(v.detail).toContain('virement');
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
    expect(v.detail).toContain('en MAD');
    expect(v.detail).toContain('en EUR');
  });

  it("aucun jour sous zéro : tout va bien jusqu'à l'horizon (vert)", () => {
    const plan = planTresorerieFixture();
    plan.banques = [{ ...planBanqueFixture(1, 'Banque A', 0), joursSoldeNegatif: 0 }];
    const v = verdictDuPlan(plan);
    expect(v.niveau).toBe('ok');
    expect(v.titre).toContain('31/12/2026');
  });
});
