import { TestBed } from '@angular/core/testing';
import { CartesBanquesComponent } from './cartes-banques.component';
import { planBanqueFixture } from '../../testing/plan-tresorerie.fixture';
import { PlanBanque } from '../../dtos/PlanTresorerie';

describe('CartesBanquesComponent', () => {

  const saine = (id: number, nom: string): PlanBanque =>
    ({ ...planBanqueFixture(id, nom, 0), joursSoldeNegatif: 0, pointBas: { date: '2026-11-20', solde: 9000, disponibilite: 11000 } });

  function creer(banques: PlanBanque[]) {
    TestBed.configureTestingModule({ imports: [CartesBanquesComponent] });
    const fixture = TestBed.createComponent(CartesBanquesComponent);
    fixture.componentRef.setInput('banques', banques);
    fixture.componentRef.setInput('dateCalcul', '2026-10-15');
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  it('une carte par banque, la banque en besoin en premier', () => {
    const { el } = creer([saine(2, 'Banque B'), planBanqueFixture(1, 'Banque A', 1000)]);
    expect(Array.from(el.querySelectorAll('.carte-banque__nom')).map(n => n.textContent!.trim())).toEqual(['Banque A', 'Banque B']);
    expect(el.querySelector('.carte-banque--danger')).not.toBeNull();
    expect(el.querySelector('.carte-banque--ok')).not.toBeNull();
  });

  it("une banque en besoin affiche son besoin, son jour et depuis combien de jours", () => {
    const { el } = creer([planBanqueFixture(1, 'Banque A', 1000)]);
    const carte = el.querySelector('.carte-banque--danger')!.textContent!;
    expect(carte).toContain('Besoin');
    expect(carte).toContain('20/11');
    expect(carte).toContain('depuis 3 j');
  });

  it("l'excédent des banques saines n'apparaît que si une banque est en besoin", () => {
    expect(creer([planBanqueFixture(1, 'Banque A', 1000), saine(2, 'Banque B')]).el.querySelector('.carte-banque__excedent')).not.toBeNull();
    TestBed.resetTestingModule();
    expect(creer([saine(2, 'Banque B'), saine(3, 'Banque C')]).el.querySelector('.carte-banque__excedent')).toBeNull();
  });

  it('un clic sur une carte ouvre le détail de la banque, sans période', () => {
    const { fixture, el } = creer([planBanqueFixture(1, 'Banque A', 1000)]);
    let choix: unknown;
    fixture.componentInstance.choisir.subscribe(c => choix = c);
    (el.querySelector('.carte-banque') as HTMLButtonElement).click();
    expect(choix).toEqual({ banqueId: 1, devise: 'MAD', colonne: '' });
  });
});
