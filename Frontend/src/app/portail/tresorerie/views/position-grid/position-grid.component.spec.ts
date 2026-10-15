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
    fixture.componentRef.setInput('groupes', plan.groupes);
    fixture.detectChanges();
    const choix: CaseChoisie[] = [];
    fixture.componentInstance.choisir.subscribe(c => choix.push(c));
    return { fixture, choix, el: fixture.nativeElement as HTMLElement, plan };
  }

  const ligne = (el: HTMLElement, n: 1 | 2) => el.querySelectorAll(`tbody .grille__ligne--n${n}`);

  it('une ligne par banque, une case de solde par période, et le total du groupe', () => {
    const { el } = creer();
    expect(ligne(el, 1).length).toBe(2);
    expect(el.querySelectorAll('tbody tr:first-child .cellule').length).toBe(2);
    expect(el.querySelectorAll('tfoot .cellule').length).toBe(2);
    expect(ligne(el, 2).length).toBe(0);
  });

  it('colore chaque case : vert si positif, orange si couvert par la facilité, rouge si besoin', () => {
    const { fixture } = creer();
    const g = fixture.componentInstance;
    const banque = planBanqueFixture(1, 'A');
    // soldes [6000, -3000], disponibilités [8000, -1000]
    expect(g.etat(banque, 0)).toBe('ok');
    expect(g.etat(banque, 1)).toBe('danger');
    expect(g.etat({ ...banque, disponibilites: [8000, 500] }, 1)).toBe('warn');
  });

  it('un clic sur une banque déplie ses sources (flux signés), un second les replie', () => {
    const { fixture, el } = creer();
    const toggle = el.querySelector<HTMLButtonElement>('tbody .grille__ligne--n1 .grille__toggle')!;
    toggle.click();
    fixture.detectChanges();

    expect(ligne(el, 2).length).toBe(3);
    expect(toggle.getAttribute('aria-expanded')).toBe('true');
    expect(el.textContent).toContain('Leasing');
    expect(ligne(el, 2)[0].querySelector('td.flux.entree')).not.toBeNull();
    expect(ligne(el, 2)[1].querySelector('td.flux.sortie')).not.toBeNull();

    toggle.click();
    fixture.detectChanges();
    expect(ligne(el, 2).length).toBe(0);
  });

  it('« Tout déplier » puis « Tout replier » agit sur toutes les banques', () => {
    const { fixture, el } = creer();
    el.querySelector<HTMLButtonElement>('.barre__tout')!.click();
    fixture.detectChanges();
    expect(ligne(el, 2).length).toBe(6);
    expect(el.querySelector('.barre__tout')!.textContent).toContain('Tout replier');

    el.querySelector<HTMLButtonElement>('.barre__tout')!.click();
    fixture.detectChanges();
    expect(ligne(el, 2).length).toBe(0);
  });

  it('un clic sur une case remonte la banque et la période ; sur le nom, la banque seule ; sur le total, la devise', () => {
    const { el, choix } = creer();
    el.querySelectorAll<HTMLElement>('tbody tr:first-child .cellule')[1].click();
    el.querySelector<HTMLElement>('tbody tr:first-child .grille__nom')!.click();
    el.querySelectorAll<HTMLElement>('tfoot .cellule')[0].click();
    expect(choix).toEqual([
      { banqueId: 1, colonne: 'P1', devise: 'MAD' },
      { banqueId: 1, colonne: '', devise: 'MAD' },
      { banqueId: null, colonne: 'ECHU', devise: 'MAD' },
    ]);
  });

  it('chaque case a un libellé accessible et la trajectoire est tracée', () => {
    const { el } = creer();
    expect(el.querySelector('.cellule')!.getAttribute('aria-label')).toContain('Banque A');
    expect(el.querySelector('.cellule--danger')!.getAttribute('aria-label')).toContain('besoin');
    expect(el.querySelector('svg polyline')!.getAttribute('points')).toBeTruthy();
  });

  it('plusieurs devises : une section par devise, un total par devise, le code de devise sur chaque banque', () => {
    const { fixture, el, plan } = creer();
    fixture.componentRef.setInput('banques', [plan.banques[0], { ...plan.banques[1], devise: 'EUR' }]);
    fixture.componentRef.setInput('groupes', [plan.groupes[0], { ...planBanqueFixture(null, 'Banques en EUR', 0), devise: 'EUR' }]);
    fixture.detectChanges();

    expect(el.querySelectorAll('tbody .grille__devise-ligne').length).toBe(2);
    expect(ligne(el, 1).length).toBe(2);
    expect(el.querySelectorAll('tfoot tr').length).toBe(2);
    expect(el.querySelector('tfoot')!.textContent).toContain('Total MAD');
    expect(el.querySelector('tfoot')!.textContent).toContain('Total EUR');
    expect(el.querySelectorAll('tbody .grille__ligne--n1 .puce').length).toBeGreaterThanOrEqual(2);
  });
});
