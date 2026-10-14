import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { HttpTestingController } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { testProviders } from '../../../../../testing/test-providers';
import { TresoreriePrevisionnelleComponent } from './tresorerie-previsionnelle.component';
import { CourbeTresorerieComponent } from '../courbe-tresorerie/courbe-tresorerie.component';
import { planTresorerieFixture } from '../../testing/plan-tresorerie.fixture';

describe('TresoreriePrevisionnelleComponent', () => {
  let http: HttpTestingController;

  function creer(queryParams: Record<string, string> = {}) {
    TestBed.configureTestingModule({
      imports: [TresoreriePrevisionnelleComponent],
      providers: [
        ...testProviders,
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap(queryParams) }, queryParamMap: of(convertToParamMap(queryParams)) },
        },
      ],
    });
    // La courbe (Chart.js) n'est pas l'objet de ces tests
    TestBed.overrideComponent(CourbeTresorerieComponent, { set: { template: '', imports: [] } });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(TresoreriePrevisionnelleComponent);
    fixture.detectChanges();
    return fixture;
  }

  afterEach(() => http.verify());

  it("charge le plan à l'horizon porté par l'adresse et affiche la vue consolidée", () => {
    const fixture = creer({ horizon: 'FIN_DE_TRIMESTRE' });
    const req = http.expectOne(r => r.url.endsWith('tresorerie-previsionnelle'));
    expect(req.request.params.get('periode')).toBe('FIN_DE_TRIMESTRE');
    req.flush(planTresorerieFixture());
    fixture.detectChanges();

    const c = fixture.componentInstance;
    expect(c.vueConsolidee).toBeTrue();
    expect(c.perimetre!.banqueNom).toBe('Toutes les banques');
    expect(c.banqueOptions.map(o => o.label)).toEqual(['Toutes les banques', 'Banque A', 'Banque B']);
    expect(fixture.nativeElement.textContent).toContain('Par banque');
  });

  it("ouvre directement la banque demandée par l'adresse et signale le besoin de financement", () => {
    const fixture = creer({ banque: '1' });
    http.expectOne(r => r.url.endsWith('tresorerie-previsionnelle')).flush(planTresorerieFixture());
    fixture.detectChanges();

    const c = fixture.componentInstance;
    expect(c.perimetre!.banqueNom).toBe('Banque A');
    expect(c.alerte).toBe('besoin');
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('20/11/2026');
  });

  it("changer d'horizon recharge le plan et l'écrit dans l'adresse", () => {
    const fixture = creer();
    http.expectOne(r => r.url.endsWith('tresorerie-previsionnelle')).flush(planTresorerieFixture());
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);

    fixture.componentInstance.horizonControl.setValue('FIN_DE_TRIMESTRE');

    const req = http.expectOne(r => r.url.endsWith('tresorerie-previsionnelle'));
    expect(req.request.params.get('periode')).toBe('FIN_DE_TRIMESTRE');
    req.flush(planTresorerieFixture());
    expect(navigate.calls.mostRecent().args[1]!.queryParams).toEqual({ horizon: 'FIN_DE_TRIMESTRE', banque: null });
  });

  it("exporte le PDF de la banque affichée, ou consolidé en vue « Toutes les banques »", () => {
    const fixture = creer();
    http.expectOne(r => r.url.endsWith('tresorerie-previsionnelle')).flush(planTresorerieFixture());
    const c = fixture.componentInstance;

    c.exporterPdf();
    expect(http.expectOne(r => r.url.endsWith('/pdf')).request.params.has('banqueId')).toBeFalse();

    c.choisirBanque(2);
    c.exporterPdf();
    expect(http.expectOne(r => r.url.endsWith('/pdf')).request.params.get('banqueId')).toBe('2');
  });
});
