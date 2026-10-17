import { TestBed } from '@angular/core/testing';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { testProviders } from '../../../../../testing/test-providers';
import { TableauGlobalComponent } from './tableau-global.component';
import { planBanqueFixture, planTresorerieFixture } from '../../testing/plan-tresorerie.fixture';
import { FILTRES_VIDES } from '../../plan-banque';

registerLocaleData(localeFr, 'fr-FR');

describe('TableauGlobalComponent', () => {

  function creer() {
    TestBed.configureTestingModule({ imports: [TableauGlobalComponent], providers: testProviders });
    const fixture = TestBed.createComponent(TableauGlobalComponent);
    const plan = planTresorerieFixture();
    fixture.componentRef.setInput('banques', plan.banques);
    fixture.componentRef.setInput('colonnes', plan.colonnes);
    fixture.componentRef.setInput('operations', []);
    fixture.componentRef.setInput('filtres', FILTRES_VIDES);
    fixture.detectChanges();
    return fixture;
  }

  it('une ligne par banque, une barre de facilité de caisse par case et le total du groupe', () => {
    const fixture = creer();
    const el: HTMLElement = fixture.nativeElement;

    expect(el.querySelectorAll('tr.banque').length).toBe(2);
    expect(el.querySelectorAll('tr.banque app-fc-barre').length).toBe(4);
    expect(el.querySelector('tr.total')!.textContent).toContain('Total groupe');
    expect(fixture.componentInstance.totaux).toEqual([12000, -6000]);
  });

  it('une banque se déplie en entrées et sorties ; tout déplier / replier agit sur toutes', () => {
    const fixture = creer();
    const el: HTMLElement = fixture.nativeElement;

    el.querySelector<HTMLButtonElement>('tr.banque .deplier')!.click();
    fixture.detectChanges();
    expect(el.querySelectorAll('tr.flux').length).toBe(2);

    fixture.componentInstance.toutDeplier(true);
    fixture.detectChanges();
    expect(el.querySelectorAll('tr.flux').length).toBe(4);
    expect(fixture.componentInstance.toutesDepliees).toBeTrue();
    fixture.componentInstance.toutDeplier(false);
    fixture.detectChanges();
    expect(el.querySelectorAll('tr.flux').length).toBe(0);
  });

  it('un clic sur une case ouvre la banque sur cette période', () => {
    const fixture = creer();
    const choix: { banqueId: number; colonne: string }[] = [];
    fixture.componentInstance.ouvrirBanque.subscribe(c => choix.push(c));

    fixture.nativeElement.querySelectorAll('tr.banque')[1].querySelectorAll('.solde__btn')[1].click();
    expect(choix).toEqual([{ banqueId: 2, colonne: 'P1' }]);
  });

  it('sans banque : message', () => {
    const fixture = creer();
    fixture.componentRef.setInput('banques', [planBanqueFixture(null, 'x')].slice(1));
    fixture.componentInstance.ngOnChanges();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Aucune banque dans ce périmètre');
  });
});
