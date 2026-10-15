import { TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { testProviders } from '../../../../../testing/test-providers';
import { BanquePanelComponent } from './banque-panel.component';
import { CourbeTresorerieComponent } from '../courbe-tresorerie/courbe-tresorerie.component';
import { planBanqueFixture, planTresorerieFixture } from '../../testing/plan-tresorerie.fixture';

describe('BanquePanelComponent', () => {
  let http: HttpTestingController;

  function creer(banqueId: number | null, colonne = '') {
    TestBed.configureTestingModule({ imports: [BanquePanelComponent], providers: testProviders });
    TestBed.overrideComponent(CourbeTresorerieComponent, { set: { template: '', imports: [] } });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(BanquePanelComponent);
    const plan = planTresorerieFixture();
    fixture.componentRef.setInput('plan', planBanqueFixture(banqueId, 'Banque A'));
    fixture.componentRef.setInput('colonnes', plan.colonnes);
    fixture.componentRef.setInput('colonne', colonne);
    fixture.detectChanges();
    return fixture;
  }

  const operations = (r: { url: string }) => r.url.includes('/recapitulatif/banque/1');

  afterEach(() => http.verify());

  it('sans période choisie, ouvre celle du point bas (20/11 : colonne de novembre) et borne les dates', () => {
    const fixture = creer(1);
    const req = http.expectOne(operations);
    expect(fixture.componentInstance.periode.code).toBe('P1');
    expect(req.request.params.get('dateReferenceDepuis')).toBe('2026-11-01');
    expect(req.request.params.get('dateReferenceJusqua')).toBe('2026-11-30');
    expect(req.request.params.get('sort')).toBe('montant,desc');
    req.flush({ totalDebit: 0, totalCredit: 0, page: { content: [], page: { totalElements: 0 } } });
  });

  it("la colonne « À ce jour » n'a pas de borne basse", () => {
    creer(1, 'ECHU');
    const req = http.expectOne(operations);
    expect(req.request.params.has('dateReferenceDepuis')).toBeFalse();
    expect(req.request.params.get('dateReferenceJusqua')).toBe('2026-10-14');
    req.flush({ totalDebit: 0, totalCredit: 0, page: { content: [], page: { totalElements: 0 } } });
  });

  it("le groupe n'a pas de liste d'opérations : aucune requête", () => {
    const fixture = creer(null);
    http.expectNone(r => r.url.includes('/recapitulatif/banque/'));
    expect(fixture.componentInstance.listeDisponible).toBeFalse();
  });

  it('affiche les opérations reçues et le statut de la banque', () => {
    const fixture = creer(1);
    http.expectOne(operations).flush({
      totalDebit: 0, totalCredit: 0,
      page: { content: [{ id: 5, dateEmission: '2026-11-18', dateEcheance: '2026-11-20', libelle: 'Loyer leasing', sens: 'DECAISSEMENT', debit: 6000, credit: 0, source: 'LEASING' }], page: { totalElements: 12 } },
    });
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Loyer leasing');
    expect(el.textContent).toContain('Voir les 12 opérations');
    expect(el.textContent).toContain('Besoin de financement');
  });

  it('choisir une période le signale à l\'écran', () => {
    const fixture = creer(1);
    http.expectOne(operations).flush({ totalDebit: 0, totalCredit: 0, page: { content: [], page: { totalElements: 0 } } });
    const choix: string[] = [];
    fixture.componentInstance.colonneChange.subscribe(c => choix.push(c));
    fixture.componentInstance.choisirPeriode('ECHU');
    expect(choix).toEqual(['ECHU']);
  });
});
