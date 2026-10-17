import { TestBed } from '@angular/core/testing';
import { testProviders } from '../../../../../testing/test-providers';
import { DetailBanqueComponent } from './detail-banque.component';
import { planBanqueFixture, planTresorerieFixture } from '../../testing/plan-tresorerie.fixture';

describe('DetailBanqueComponent', () => {

  function creer(besoin: number) {
    TestBed.configureTestingModule({ imports: [DetailBanqueComponent], providers: testProviders });
    const fixture = TestBed.createComponent(DetailBanqueComponent);
    fixture.componentRef.setInput('plan', planBanqueFixture(null, 'Toutes les banques', besoin));
    fixture.componentRef.setInput('colonnes', planTresorerieFixture().colonnes);
    fixture.detectChanges();
    return fixture;
  }

  it('affiche le total, son statut et sa date d\'horizon', () => {
    const fixture = creer(1000);
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Toutes les banques');
    expect(el.textContent).toContain('Besoin');
    expect(fixture.componentInstance.dateHorizon).toBe('2026-11-30');
    expect(fixture.componentInstance.statut).toBe('danger');
  });

  it('sans besoin mais avec des jours négatifs : statut « facilité de caisse utilisée »', () => {
    expect(creer(0).componentInstance.statut).toBe('warn');
  });

  it('la croix du détail le ferme', () => {
    const fixture = creer(0);
    let fermetures = 0;
    fixture.componentInstance.fermer.subscribe(() => fermetures++);
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.entete__fermer')!.click();
    expect(fermetures).toBe(1);
  });
});
