import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { construireLignes, PlanTresorerieTableComponent } from './plan-tresorerie-table.component';
import { planBanqueFixture, planTresorerieFixture } from '../../testing/plan-tresorerie.fixture';

describe('PlanTresorerieTableComponent', () => {

  it('construit les lignes dans l\'ordre de lecture : solde de début, encaissements, décaissements, soldes', () => {
    const lignes = construireLignes(planBanqueFixture(1, 'Banque A'));

    expect(lignes.map(l => l.libelle)).toEqual([
      'Solde de début', 'Encaissements', 'REGLEMENT FRNS', 'Total encaissements',
      'Décaissements', 'Leasing', 'Total décaissements', 'Solde de fin', 'Facilité de caisse', 'Disponibilité',
    ]);
    // Le solde de début d'une colonne est le solde de fin de la précédente
    expect(lignes[0].valeurs).toEqual([5000, 6000]);
    expect(lignes[6].total).toBe(9000);
    expect(lignes[8].valeurs).toEqual([2000, 2000]);
  });

  it('affiche les colonnes du plan, un tiret pour zéro et les montants négatifs en rouge', () => {
    TestBed.configureTestingModule({ imports: [PlanTresorerieTableComponent], providers: [provideNoopAnimations()] });
    const fixture = TestBed.createComponent(PlanTresorerieTableComponent);
    const plan = planTresorerieFixture();
    fixture.componentRef.setInput('colonnes', plan.colonnes);
    fixture.componentRef.setInput('plan', plan.banques[0]);
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('thead')!.textContent).toContain('Novembre 2026');
    expect(el.querySelector('thead')!.textContent).toContain('en retard');
    expect(el.querySelectorAll('td.neg').length).toBeGreaterThan(0);
    expect(el.textContent).toContain('—');
  });
});
