import { TestBed } from '@angular/core/testing';
import { testProviders } from '../../../../../testing/test-providers';
import { BarreFiltresComponent } from './barre-filtres.component';
import { FILTRES_GLOBAL_VIDES, FiltresGlobal } from '../../plan-global';
import { TransactionSource } from '../../../transactions/dtos/TransactionSource';

describe('BarreFiltresComponent', () => {

  function creer(filtres: FiltresGlobal = FILTRES_GLOBAL_VIDES, banques = true, devises: string[] = []) {
    TestBed.configureTestingModule({ imports: [BarreFiltresComponent], providers: testProviders });
    const fixture = TestBed.createComponent(BarreFiltresComponent);
    fixture.componentRef.setInput('filtres', filtres);
    fixture.componentRef.setInput('sources', [{ label: 'Leasing (2)', value: TransactionSource.LEASING }]);
    fixture.componentRef.setInput('banques', banques ? [{ label: 'BMCI', value: 3 }] : null);
    fixture.componentRef.setInput('horizon', 'FIN_DE_MOIS');
    fixture.componentRef.setInput('devises', devises);
    fixture.componentRef.setInput('devise', devises[0] ?? '');
    fixture.componentRef.setInput('resume', { nombre: 2, entrees: 0, sorties: 1500 });
    fixture.detectChanges();
    return fixture;
  }

  it('sans filtre : une seule barre, aucune pastille', () => {
    const fixture = creer();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelectorAll('.barre').length).toBe(1);
    expect(el.querySelector('.filtres')).toBeNull();
    expect(el.querySelectorAll('p-button').length).toBe(1);
    expect(fixture.componentInstance.actif).toBeFalse();
  });

  it('le filtre de banques n\'apparaît que sur la page globale ; la devise seulement à partir de deux', () => {
    expect(creer(FILTRES_GLOBAL_VIDES, false).nativeElement.querySelector('p-multiselect.barre__banques')).toBeNull();
    TestBed.resetTestingModule();
    expect(creer(FILTRES_GLOBAL_VIDES, true, ['MAD']).nativeElement.querySelector('.barre__devise')).toBeNull();
    TestBed.resetTestingModule();
    expect(creer(FILTRES_GLOBAL_VIDES, true, ['MAD', 'EUR']).nativeElement.querySelector('.barre__devise')).not.toBeNull();
  });

  it('filtres actifs : pastilles retirables, résumé des opérations retenues et « Effacer les filtres »', () => {
    const fixture = creer({ banques: [3], sens: 'DECAISSEMENT', sources: [TransactionSource.LEASING], operateur: '>', montant: 1000, texte: 'loyer' });
    const emis: Partial<FiltresGlobal>[] = [];
    let efface = 0;
    fixture.componentInstance.filtresChange.subscribe(f => emis.push(f));
    fixture.componentInstance.effacer.subscribe(() => efface++);

    const pastilles = fixture.componentInstance.pastilles;
    expect(pastilles.map(p => p.libelle)).toEqual(['BMCI', 'Sorties', 'Leasing', 'Montant > 1 000,00', '« loyer »']);
    expect((fixture.nativeElement as HTMLElement).querySelector('.filtres__resume')!.textContent).toContain('2 opérations');

    pastilles[0].retirer();
    pastilles[1].retirer();
    pastilles[3].retirer();
    expect(emis).toEqual([{ banques: [] }, { sens: 'TOUS' }, { montant: null }]);

    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.filtres__effacer')!.click();
    expect(efface).toBe(1);
  });

  it("n'émet que les vrais changements (PrimeNG émet aussi à l'initialisation)", () => {
    const fixture = creer();
    const emis: Partial<FiltresGlobal>[] = [];
    fixture.componentInstance.filtresChange.subscribe(f => emis.push(f));

    fixture.componentInstance.changer('sens', 'TOUS');
    fixture.componentInstance.changer('montant', null);
    fixture.componentInstance.changer('sens', 'ENCAISSEMENT');
    expect(emis).toEqual([{ sens: 'ENCAISSEMENT' }]);
  });
});
