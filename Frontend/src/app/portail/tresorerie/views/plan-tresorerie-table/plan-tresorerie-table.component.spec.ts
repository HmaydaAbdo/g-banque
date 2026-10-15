import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { construireLignes, PlanTresorerieTableComponent } from './plan-tresorerie-table.component';
import { planBanqueFixture, planTresorerieFixture } from '../../testing/plan-tresorerie.fixture';

describe('PlanTresorerieTableComponent', () => {

  it("regroupe les catégories par famille, dans l'ordre de lecture du plan", () => {
    const lignes = construireLignes(planBanqueFixture(1, 'Banque A'));

    expect(lignes.map(l => l.libelle)).toEqual([
      'Solde de début',
      'Encaissements', 'Règlements', 'REGLEMENT FRNS', 'Total encaissements',
      'Décaissements', 'Financements', 'Leasing', 'Agios', 'Total décaissements',
      'Solde de fin', 'Facilité de caisse', 'Disponibilité',
    ]);
    // Le solde de début d'une colonne est le solde de fin de la précédente
    expect(lignes[0].valeurs).toEqual([5000, 6000]);
  });

  it('la ligne d\'une famille additionne ses catégories', () => {
    const financements = construireLignes(planBanqueFixture(1, 'Banque A')).find(l => l.libelle === 'Financements')!;
    expect(financements.type).toBe('famille');
    expect(financements.valeurs).toEqual([0, 9000]);
    expect(financements.total).toBe(9000);
  });

  function creer() {
    TestBed.configureTestingModule({ imports: [PlanTresorerieTableComponent], providers: [provideNoopAnimations()] });
    const fixture = TestBed.createComponent(PlanTresorerieTableComponent);
    const plan = planTresorerieFixture();
    fixture.componentRef.setInput('colonnes', plan.colonnes);
    fixture.componentRef.setInput('plan', plan.banques[0]);
    fixture.detectChanges();
    return fixture;
  }

  it('affiche les colonnes du plan, chaque source déjà visible, un tiret pour zéro et le négatif en rouge', () => {
    const el: HTMLElement = creer().nativeElement;
    expect(el.querySelector('thead')!.textContent).toContain('Novembre 2026');
    expect(el.querySelector('thead')!.textContent).toContain('échues');
    expect(el.textContent).toContain('Financements');
    expect(el.textContent).toContain('Leasing');
    expect(el.querySelectorAll('td.neg').length).toBeGreaterThan(0);
    expect(el.textContent).toContain('—');
  });

  it('un clic sur une famille replie ses sources, un second les redéplie', () => {
    const fixture = creer();
    const el: HTMLElement = fixture.nativeElement;
    const bouton = Array.from(el.querySelectorAll<HTMLButtonElement>('.plan__toggle')).find(b => b.textContent!.includes('Financements'))!;
    expect(bouton.getAttribute('aria-expanded')).toBe('true');

    bouton.click();
    fixture.detectChanges();
    expect(el.textContent).not.toContain('Leasing');
    expect(bouton.getAttribute('aria-expanded')).toBe('false');

    bouton.click();
    fixture.detectChanges();
    expect(el.textContent).toContain('Leasing');
  });
});
