import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { PositionGridComponent, CaseChoisie } from './position-grid.component';
import { planBanqueFixture, planTresorerieFixture } from '../../testing/plan-tresorerie.fixture';

describe('PositionGridComponent', () => {

  function creer() {
    TestBed.configureTestingModule({ imports: [PositionGridComponent], providers: [provideNoopAnimations()] });
    const fixture = TestBed.createComponent(PositionGridComponent);
    const plan = planTresorerieFixture();
    fixture.componentRef.setInput('colonnes', plan.colonnes);
    fixture.componentRef.setInput('banques', plan.banques);
    fixture.componentRef.setInput('groupe', plan.consolide);
    fixture.detectChanges();
    const choix: CaseChoisie[] = [];
    fixture.componentInstance.choisir.subscribe(c => choix.push(c));
    return { fixture, choix, el: fixture.nativeElement as HTMLElement, plan };
  }

  it('une ligne par banque, une case par période, et le total du groupe', () => {
    const { el } = creer();
    expect(el.querySelectorAll('tbody tr').length).toBe(2);
    expect(el.querySelectorAll('tbody tr:first-child .cellule').length).toBe(2);
    expect(el.querySelectorAll('tfoot .cellule').length).toBe(2);
  });

  it('colore chaque case : vert si positif, orange si couvert par la facilité, rouge si besoin de financement', () => {
    const { fixture } = creer();
    const g = fixture.componentInstance;
    const banque = planBanqueFixture(1, 'A');
    // soldes [6000, -3000], disponibilités [8000, -1000]
    expect(g.etat(banque, 0)).toBe('ok');
    expect(g.etat(banque, 1)).toBe('danger');
    expect(g.etat({ ...banque, disponibilites: [8000, 500] }, 1)).toBe('warn');
  });

  it('un clic sur une case remonte la banque et la période ; sur le nom, la banque seule', () => {
    const { el, choix } = creer();
    el.querySelectorAll<HTMLElement>('tbody tr:first-child .cellule')[1].click();
    el.querySelector<HTMLElement>('tbody tr:first-child .grille__nom')!.click();
    el.querySelectorAll<HTMLElement>('tfoot .cellule')[0].click();
    expect(choix).toEqual([
      { banqueId: 1, colonne: 'P1' },
      { banqueId: 1, colonne: '' },
      { banqueId: null, colonne: 'ECHU' },
    ]);
  });

  it('chaque case a un libellé accessible et la trajectoire est tracée', () => {
    const { el } = creer();
    expect(el.querySelector('.cellule')!.getAttribute('aria-label')).toContain('Banque A');
    expect(el.querySelector('.cellule--danger')!.getAttribute('aria-label')).toContain('besoin de financement');
    expect(el.querySelector('svg polyline')!.getAttribute('points')).toBeTruthy();
  });
});
