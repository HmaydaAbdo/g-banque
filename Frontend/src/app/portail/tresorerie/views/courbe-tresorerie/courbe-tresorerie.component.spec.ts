import { CourbeTresorerieComponent } from './courbe-tresorerie.component';
import { planBanqueFixture } from '../../testing/plan-tresorerie.fixture';

describe('CourbeTresorerieComponent', () => {

  function donnees(plafond: number) {
    const c = new CourbeTresorerieComponent();
    c.plan = { ...planBanqueFixture(1, 'Banque A'), plafondFC: plafond };
    c.ngOnChanges();
    return c.data as { labels: string[]; datasets: { label: string; data: number[]; pointRadius?: number[] }[] };
  }

  it('trace le solde jour par jour et met le point bas en évidence', () => {
    const d = donnees(2000);
    expect(d.labels).toEqual(['14/10', '20/11']);
    expect(d.datasets[0].data).toEqual([6000, -3000]);
    expect(d.datasets[0].pointRadius).toEqual([0, 5]);
  });

  it('ajoute la limite de la facilité de caisse seulement si un plafond existe', () => {
    expect(donnees(2000).datasets[1].data).toEqual([-2000, -2000]);
    expect(donnees(0).datasets.length).toBe(1);
  });
});
