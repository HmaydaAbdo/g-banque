import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { BandeauEtatComponent } from './bandeau-etat.component';
import { Verdict } from '../../verdict';
import { planBanqueFixture } from '../../testing/plan-tresorerie.fixture';

describe('BandeauEtatComponent', () => {

  function creer(verdict: Verdict, total = planBanqueFixture(null, 'Toutes les banques'), plusieursDevises = false) {
    TestBed.configureTestingModule({ imports: [BandeauEtatComponent], providers: [provideNoopAnimations()] });
    const fixture = TestBed.createComponent(BandeauEtatComponent);
    fixture.componentRef.setInput('verdict', verdict);
    fixture.componentRef.setInput('total', total);
    fixture.componentRef.setInput('dateCalcul', '2026-10-14');
    fixture.componentRef.setInput('dateHorizon', '2026-12-31');
    fixture.componentRef.setInput('plusieursDevises', plusieursDevises);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  const rouge: Verdict = { niveau: 'danger', titre: 'Il manquera 1 000,00 MAD le 20/11/2026 sur Banque A', detail: 'Même en utilisant toute la facilité de caisse.' };

  it('affiche le verdict dans la teinte de la situation, avec une alerte accessible quand il y a un besoin', () => {
    const el = creer(rouge);
    expect(el.querySelector('.etat--danger')).not.toBeNull();
    expect(el.querySelector('[role="alert"]')!.textContent).toContain('Il manquera 1 000,00 MAD');
    expect(el.textContent).toContain('14/10/2026');
    expect(el.textContent).toContain('31/12/2026');
  });

  it('une situation saine est annoncée comme un statut, pas comme une alerte', () => {
    const el = creer({ niveau: 'ok', titre: "Tout va bien jusqu'au 31/12/2026", detail: '' });
    expect(el.querySelector('.etat--ok')).not.toBeNull();
    expect(el.querySelector('[role="status"]')).not.toBeNull();
    expect(el.querySelector('[role="alert"]')).toBeNull();
  });

  it('donne les cinq indicateurs du total : actuel, prévu, solde le plus bas, besoin, jours à solde négatif', () => {
    const el = creer(rouge);
    const libelles = Array.from(el.querySelectorAll('dt')).map(dt => dt.textContent!.trim());
    expect(libelles).toEqual(['Solde actuel', 'Solde prévu', 'Solde le plus bas', 'Besoin', 'Jours à solde négatif']);
    // fixture : solde actuel 5 000, prévu −3 000 → variation −8 000
    expect(el.textContent).toContain('sur la période');
    expect(el.querySelector('.indicateur--alerte')).not.toBeNull();
  });

  it("sans besoin, l'indicateur devient la disponibilité la plus basse", () => {
    const total = { ...planBanqueFixture(null, 'Toutes les banques', 0), pointBas: { date: '2026-11-20', solde: 100, disponibilite: 2100 } };
    const el = creer({ niveau: 'ok', titre: 'Tout va bien', detail: '' }, total);
    expect(Array.from(el.querySelectorAll('dt')).map(dt => dt.textContent!.trim())).toContain('Disponibilité la plus basse');
    expect(el.querySelector('.indicateur--alerte')).toBeNull();
  });

  it('plusieurs devises : la devise est rappelée avec les montants', () => {
    const el = creer(rouge, { ...planBanqueFixture(null, 'Banques en EUR', 0), devise: 'EUR' }, true);
    expect(el.querySelector('dl')!.getAttribute('aria-label')).toContain('EUR');
    expect(el.querySelector('dd small')!.textContent).toContain('EUR');
  });
});
