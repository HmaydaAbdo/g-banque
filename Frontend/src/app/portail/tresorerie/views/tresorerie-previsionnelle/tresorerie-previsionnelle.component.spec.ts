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

  const planReq = () => http.expectOne(r => r.url.endsWith('tresorerie-previsionnelle'));

  afterEach(() => http.verify());

  it("charge le plan à l'horizon de l'adresse et affiche verdict et grille, panneau fermé", () => {
    const fixture = creer({ horizon: 'FIN_DE_TRIMESTRE' });
    const req = planReq();
    expect(req.request.params.get('periode')).toBe('FIN_DE_TRIMESTRE');
    req.flush(planTresorerieFixture());
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.verdict--danger')!.textContent).toContain('Banque A');
    expect(el.querySelectorAll('.grille tbody tr').length).toBe(2);
    expect(el.querySelector('app-banque-panel')).toBeNull();
  });

  it("une banque dans l'adresse ouvre directement son panneau", () => {
    const fixture = creer({ banque: '1', col: 'P1' });
    http.expectOne(r => r.url.endsWith('tresorerie-previsionnelle')).flush(planTresorerieFixture());
    fixture.detectChanges();
    http.expectOne(r => r.url.includes('/recapitulatif/banque/1')).flush({
      totalDebit: 0, totalCredit: 0, page: { content: [], page: { totalElements: 0 } },
    });
    fixture.detectChanges();

    expect(fixture.componentInstance.planDuPanneau!.banqueNom).toBe('Banque A');
    expect(fixture.componentInstance.colonne).toBe('P1');
    expect(fixture.nativeElement.querySelector('app-banque-panel')).not.toBeNull();
  });

  it("ouvrir une case écrit banque et période dans l'adresse ; fermer les retire", () => {
    const fixture = creer();
    planReq().flush(planTresorerieFixture());
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    const c = fixture.componentInstance;

    c.ouvrir({ banqueId: 2, colonne: 'P1' });
    expect(navigate.calls.mostRecent().args[1]!.queryParams).toEqual({ horizon: null, banque: 2, col: 'P1' });
    fixture.detectChanges();
    http.expectOne(r => r.url.includes('/recapitulatif/banque/2')).flush({
      totalDebit: 0, totalCredit: 0, page: { content: [], page: { totalElements: 0 } },
    });
    fixture.detectChanges();

    c.fermer();
    expect(navigate.calls.mostRecent().args[1]!.queryParams).toEqual({ horizon: null, banque: null, col: null });
    expect(c.planDuPanneau).toBeNull();
  });

  it("le total du groupe ouvre le panneau consolidé, sans liste d'opérations ni écriture dans l'adresse", () => {
    const fixture = creer();
    planReq().flush(planTresorerieFixture());
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);

    fixture.componentInstance.ouvrir({ banqueId: null, colonne: '' });
    fixture.detectChanges();

    expect(fixture.componentInstance.planDuPanneau!.banqueNom).toBe('Toutes les banques');
    expect(navigate.calls.mostRecent().args[1]!.queryParams!['banque']).toBeNull();
    http.expectNone(r => r.url.includes('/recapitulatif/banque/'));
  });

  it("changer d'horizon recharge le plan et l'écrit dans l'adresse", () => {
    const fixture = creer();
    planReq().flush(planTresorerieFixture());
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);

    fixture.componentInstance.horizonControl.setValue('FIN_DE_TRIMESTRE');

    const req = planReq();
    expect(req.request.params.get('periode')).toBe('FIN_DE_TRIMESTRE');
    req.flush(planTresorerieFixture());
    expect(navigate.calls.mostRecent().args[1]!.queryParams!['horizon']).toBe('FIN_DE_TRIMESTRE');
  });

  it('exporte le PDF consolidé, ou celui de la banque demandée', () => {
    const fixture = creer();
    planReq().flush(planTresorerieFixture());
    const c = fixture.componentInstance;

    c.exporterPdf(null);
    expect(http.expectOne(r => r.url.endsWith('/pdf')).request.params.has('banqueId')).toBeFalse();

    c.exporterPdf(2);
    expect(http.expectOne(r => r.url.endsWith('/pdf')).request.params.get('banqueId')).toBe('2');
  });
});
