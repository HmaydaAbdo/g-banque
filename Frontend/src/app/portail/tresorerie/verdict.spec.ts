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
    expect(v.detail).toContain('2 banques');
  });

  it('solde négatif couvert par la facilité : orange', () => {
    const plan = planTresorerieFixture();
    plan.banques = [planBanqueFixture(1, 'Banque A', 0)];
    expect(verdictDuPlan(plan).niveau).toBe('warn');
  });

  it("aucun jour sous zéro : tout va bien jusqu'à l'horizon (vert)", () => {
    const plan = planTresorerieFixture();
    plan.banques = [{ ...planBanqueFixture(1, 'Banque A', 0), joursSoldeNegatif: 0 }];
    const v = verdictDuPlan(plan);
    expect(v.niveau).toBe('ok');
    expect(v.titre).toContain('31/12/2026');
  });
});
