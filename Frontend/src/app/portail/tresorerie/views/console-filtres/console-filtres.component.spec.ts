import { TestBed } from '@angular/core/testing';
import { FormControl } from '@angular/forms';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ConsoleFiltresComponent } from './console-filtres.component';
import { HorizonPrevision } from '../../../etatsDesEncours/views/lesEncours/recap/dtos/PeriodeRecap';
import { FiltreActif } from '../../filtre-actif';

describe('ConsoleFiltresComponent', () => {

  function creer(filtres: FiltreActif[] = [], devises: string[] = ['MAD']) {
    TestBed.configureTestingModule({ imports: [ConsoleFiltresComponent], providers: [provideNoopAnimations()] });
    const fixture = TestBed.createComponent(ConsoleFiltresComponent);
    const horizon = new FormControl<HorizonPrevision>('FIN_DE_MOIS', { nonNullable: true });
    const devise = new FormControl<string>('', { nonNullable: true });
    fixture.componentRef.setInput('horizon', horizon);
    fixture.componentRef.setInput('devise', devise);
    fixture.componentRef.setInput('devises', devises);
    fixture.componentRef.setInput('filtres', filtres);
    fixture.componentRef.setInput('perimetreExport', 'Total groupe');
    fixture.componentRef.setInput('exportPossible', true);
    fixture.detectChanges();
    const evenements = { exports: 0, effacements: 0 };
    fixture.componentInstance.exporter.subscribe(() => evenements.exports++);
    fixture.componentInstance.effacer.subscribe(() => evenements.effacements++);
    return { fixture, el: fixture.nativeElement as HTMLElement, horizon, evenements };
  }

  it("réunit l'horizon et l'export au même endroit ; pas de sélecteur de devise s'il n'y en a qu'une", () => {
    const { el } = creer();
    expect(el.querySelector('[role="toolbar"]')).not.toBeNull();
    expect(el.textContent).toContain('Fin de mois');
    expect(el.textContent).toContain('Fin de trimestre');
    expect(el.textContent).toContain('Exporter PDF');
    expect(el.textContent).not.toContain('Devise');
    expect(el.textContent).toContain('Aucun filtre actif');
    expect(el.textContent).not.toContain('Effacer les filtres');
  });

  it('plusieurs devises : le sélecteur de devise apparaît dans la même console', () => {
    const { el } = creer([], ['MAD', 'EUR']);
    expect(el.textContent).toContain('Devise');
    expect(el.textContent).toContain('EUR');
  });

  it('affiche les filtres actifs en pastilles, chacun retirable, et « Effacer les filtres »', () => {
    let retire = 0;
    const { el, evenements } = creer([{ cle: 'banque', libelle: 'Banque', valeur: 'Banque A', retirer: () => retire++ }]);

    expect(el.querySelector('.pastille')!.textContent).toContain('Banque A');
    el.querySelector<HTMLButtonElement>('.pastille__retirer')!.click();
    expect(retire).toBe(1);

    el.querySelector<HTMLButtonElement>('.console__effacer')!.click();
    expect(evenements.effacements).toBe(1);
  });

  it("le bouton « Exporter PDF » demande l'export", () => {
    const { el, evenements } = creer();
    el.querySelector<HTMLButtonElement>('p-button button')!.click();
    expect(evenements.exports).toBe(1);
  });

  it("l'horizon choisi dans la console met à jour le contrôle de l'écran", () => {
    const { fixture, el, horizon } = creer();
    const boutons = Array.from(el.querySelectorAll<HTMLElement>('.p-button'));
    boutons.find(b => b.textContent!.includes('Fin de trimestre'))!.click();
    fixture.detectChanges();
    expect(horizon.value).toBe('FIN_DE_TRIMESTRE');
  });
});
