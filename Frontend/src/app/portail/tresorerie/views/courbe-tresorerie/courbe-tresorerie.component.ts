import { Component, Input, OnChanges } from '@angular/core';
import { ChartModule } from 'primeng/chart';
import { PlanBanque } from '../../dtos/PlanTresorerie';

/**
 * Courbe du solde prévu jour par jour jusqu'à l'horizon, avec la limite de la facilité de caisse
 * (−plafond) et le point bas mis en évidence. Les couleurs viennent des design tokens.
 */
@Component({
  selector: 'app-courbe-tresorerie',
  standalone: true,
  imports: [ChartModule],
  template: `<p-chart type="line" [data]="data" [options]="options" height="260px"
                      ariaLabel="Évolution du solde prévu jusqu'à l'horizon"></p-chart>`,
})
export class CourbeTresorerieComponent implements OnChanges {

  @Input({ required: true }) plan!: PlanBanque;

  data: unknown;
  options: unknown;

  ngOnChanges(): void {
    if (!this.plan) {
      return;
    }
    const couleur = (token: string) => getComputedStyle(document.documentElement).getPropertyValue(token).trim();
    const primaire = couleur('--color-primary');
    const danger = couleur('--color-danger');
    const texte = couleur('--color-text-secondary');
    const grille = couleur('--color-border');

    const courbe = this.plan.courbe;
    const indexPointBas = courbe.findIndex(p => p.date === this.plan.pointBas.date);
    const libelles = courbe.map(p => p.date.slice(8, 10) + '/' + p.date.slice(5, 7));

    const series: object[] = [{
      label: 'Solde prévu',
      data: courbe.map(p => p.solde),
      borderColor: primaire,
      backgroundColor: primaire,
      borderWidth: 2,
      tension: 0.15,
      pointRadius: courbe.map((_, i) => (i === indexPointBas ? 5 : 0)),
      pointBackgroundColor: danger,
    }];
    if (this.plan.plafondFC > 0) {
      series.push({
        label: 'Limite de la facilité de caisse',
        data: courbe.map(() => -this.plan.plafondFC),
        borderColor: danger,
        borderDash: [6, 4],
        borderWidth: 1.5,
        pointRadius: 0,
      });
    }
    this.data = { labels: libelles, datasets: series };

    const montant = (v: number) => v.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    this.options = {
      maintainAspectRatio: false,
      interaction: { mode: 'index', intersect: false },
      plugins: {
        legend: { position: 'bottom', labels: { color: texte, boxWidth: 12 } },
        tooltip: { callbacks: { label: (ctx: { dataset: { label: string }; parsed: { y: number } }) =>
              `${ctx.dataset.label} : ${montant(ctx.parsed.y)} MAD` } },
      },
      scales: {
        x: { ticks: { color: texte, maxRotation: 0, autoSkip: true, maxTicksLimit: 12 }, grid: { display: false } },
        y: {
          ticks: { color: texte, callback: (v: number) => (v / 1000).toLocaleString('fr-FR') + ' k' },
          // Ligne du zéro plus marquée : sous elle, le compte est à découvert
          grid: { color: (ctx: { tick: { value: number } }) => (ctx.tick.value === 0 ? texte : grille) },
        },
      },
    };
  }
}
