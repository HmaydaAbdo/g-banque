import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { TotalGroupeComponent } from './total-groupe.component';
import { planBanqueFixture } from '../../testing/plan-tresorerie.fixture';

describe('TotalGroupeComponent', () => {

  function creer(total = planBanqueFixture(null, 'Toutes les banques'), compte = { danger: 1, warn: 0, ok: 2 }, plusieursDevises = false) {
    TestBed.configureTestingModule({ imports: [TotalGroupeComponent], providers: [provideNoopAnimations()] });
    const fixture = TestBed.createComponent(TotalGroupeComponent);
    fixture.componentRef.setInput('total', total);
    fixture.componentRef.setInput('compte', compte);
    fixture.componentRef.setInput('dateCalcul', '2026-10-14');
    fixture.componentRef.setInput('dateHorizon', '2026-12-31');
    fixture.componentRef.setInput('plusieursDevises', plusieursDevises);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('titre « Total groupe » et période rappelée', () => {
    const el = creer();
    expect(el.querySelector('.etat__titre')!.textContent).toContain('Total groupe');
    expect(el.textContent).toContain('14/10');
    expect(el.textContent).toContain('31/12/2026');
  });

  it('le liseré suit la situation du total', () => {
    expect(creer().querySelector('.etat--danger')).not.toBeNull();
  });

  it('le nombre de banques par situation est affiché, sans les situations vides', () => {
    const puces = Array.from(creer().querySelectorAll('.etat__puce')).map(p => p.textContent!.trim());
    expect(puces).toEqual(['1 en besoin', '2 sans souci']);
  });

  it('donne les cinq indicateurs du total : actuel, prévu, solde le plus bas, besoin, jours à solde négatif', () => {
    const el = creer();
    expect(Array.from(el.querySelectorAll('dt')).map(dt => dt.textContent!.trim()))
      .toEqual(['Solde actuel', 'Solde prévu', 'Solde le plus bas', 'Besoin', 'Jours à solde négatif']);
    expect(el.querySelector('.indicateur--alerte')).not.toBeNull();
  });

  it("sans besoin, l'indicateur devient la disponibilité la plus basse", () => {
    const total = { ...planBanqueFixture(null, 'Toutes les banques', 0), pointBas: { date: '2026-11-20', solde: 100, disponibilite: 2100 } };
    const el = creer(total, { danger: 0, warn: 0, ok: 3 });
    expect(Array.from(el.querySelectorAll('dt')).map(dt => dt.textContent!.trim())).toContain('Disponibilité la plus basse');
    expect(el.querySelector('.indicateur--alerte')).toBeNull();
  });

  it('plusieurs devises : la devise est rappelée dans le titre et avec les montants', () => {
    const el = creer({ ...planBanqueFixture(null, 'Banques en EUR', 0), devise: 'EUR' }, { danger: 0, warn: 0, ok: 1 }, true);
    expect(el.querySelector('.etat__titre')!.textContent).toContain('EUR');
    expect(el.querySelector('dl')!.getAttribute('aria-label')).toContain('EUR');
    expect(el.querySelector('dd small')!.textContent).toContain('EUR');
  });
});
