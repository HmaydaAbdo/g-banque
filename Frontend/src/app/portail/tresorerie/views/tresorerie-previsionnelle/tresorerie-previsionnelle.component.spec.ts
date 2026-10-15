import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { HttpTestingController } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { testProviders } from '../../../../../testing/test-providers';
import { TresoreriePrevisionnelleComponent } from './tresorerie-previsionnelle.component';
import { CourbeTresorerieComponent } from '../courbe-tresorerie/courbe-tresorerie.component';
import { planBanqueFixture, planTresorerieFixture } from '../../testing/plan-tresorerie.fixture';

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
  const aucunePage = { totalDebit: 0, totalCredit: 0, page: { content: [], page: { totalElements: 0 } } };

  afterEach(() => http.verify());

  it("charge le plan à l'horizon de l'adresse et affiche verdict et grille, panneau fermé", () => {
    const fixture = creer({ horizon: 'FIN_DE_TRIMESTRE' });
    const req = planReq();
    expect(req.request.params.get('periode')).toBe('FIN_DE_TRIMESTRE');
    expect(req.request.params.get('certainesSeulement')).toBe('false');
    req.flush(planTresorerieFixture());
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.verdict--danger')!.textContent).toContain('Banque A');
    expect(el.querySelectorAll('.grille tbody tr').length).toBe(2);
    expect(el.querySelector('app-banque-panel')).toBeNull();
  });

  it("une banque dans l'adresse ouvre directement son panneau", () => {
    const fixture = creer({ banque: '1', col: 'P1' });
    planReq().flush(planTresorerieFixture());
    fixture.detectChanges();
    http.expectOne(r => r.url.includes('/recapitulatif/banque/1')).flush(aucunePage);
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

    c.ouvrir({ banqueId: 2, colonne: 'P1', devise: 'MAD' });
    expect(navigate.calls.mostRecent().args[1]!.queryParams).toEqual(
      { horizon: null, flux: null, vue: null, devise: null, banque: 2, col: 'P1' });
    fixture.detectChanges();
    http.expectOne(r => r.url.includes('/recapitulatif/banque/2')).flush(aucunePage);
    fixture.detectChanges();

    c.fermer();
    expect(navigate.calls.mostRecent().args[1]!.queryParams).toEqual(
      { horizon: null, flux: null, vue: null, devise: null, banque: null, col: null });
    expect(c.planDuPanneau).toBeNull();
  });

  it("le total d'une devise ouvre le panneau consolidé, sans liste d'opérations ni écriture dans l'adresse", () => {
    const fixture = creer();
    planReq().flush(planTresorerieFixture());
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);

    fixture.componentInstance.ouvrir({ banqueId: null, colonne: '', devise: 'MAD' });
    fixture.detectChanges();

    expect(fixture.componentInstance.planDuPanneau!.banqueNom).toBe('Toutes les banques');
    expect(navigate.calls.mostRecent().args[1]!.queryParams!['banque']).toBeNull();
    http.expectNone(r => r.url.includes('/recapitulatif/banque/'));
  });

  it("changer d'horizon ou de flux recharge le plan côté serveur et l'écrit dans l'adresse", () => {
    const fixture = creer();
    planReq().flush(planTresorerieFixture());
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);

    fixture.componentInstance.horizonControl.setValue('FIN_DE_TRIMESTRE');
    const req = planReq();
    expect(req.request.params.get('periode')).toBe('FIN_DE_TRIMESTRE');
    req.flush(planTresorerieFixture());
    expect(navigate.calls.mostRecent().args[1]!.queryParams!['horizon']).toBe('FIN_DE_TRIMESTRE');

    fixture.componentInstance.fluxControl.setValue('CERTAINS');
    const req2 = planReq();
    expect(req2.request.params.get('certainesSeulement')).toBe('true');
    req2.flush({ ...planTresorerieFixture(), certainesSeulement: true });
    expect(navigate.calls.mostRecent().args[1]!.queryParams!['flux']).toBe('certains');
  });

  it("la vue « Par source » n'appelle pas le serveur et affiche les sources du total", () => {
    const fixture = creer({ vue: 'source' });
    planReq().flush(planTresorerieFixture());
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('app-position-grid')).toBeNull();
    expect(el.querySelector('app-plan-tresorerie-table')!.textContent).toContain('Leasing');

    spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture.componentInstance.vueControl.setValue('BANQUE');
    fixture.detectChanges();
    expect(el.querySelector('app-position-grid')).not.toBeNull();
    http.expectNone(r => r.url.endsWith('tresorerie-previsionnelle'));
  });

  it("rappelle en pastilles les réglages qui s'écartent de la vue par défaut, et les retire un à un", () => {
    const fixture = creer({ horizon: 'FIN_DE_TRIMESTRE', flux: 'certains', vue: 'source', banque: '1', col: 'P1' });
    planReq().flush({ ...planTresorerieFixture(), certainesSeulement: true });
    fixture.detectChanges();
    http.expectOne(r => r.url.includes('/recapitulatif/banque/1')).flush(aucunePage);
    fixture.detectChanges();

    const c = fixture.componentInstance;
    expect(c.filtres.map(f => f.cle)).toEqual(['horizon', 'flux', 'vue', 'banque', 'periode']);
    const texte = (fixture.nativeElement as HTMLElement).querySelector('.filtres')!.textContent!;
    expect(texte).toContain('Fin de trimestre');
    expect(texte).toContain('Certains seulement');
    expect(texte).toContain('Banque A');
    expect(texte).toContain('Novembre 2026');

    spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    c.filtres.find(f => f.cle === 'periode')!.retirer();
    expect(c.colonne).toBe('');
    c.filtres.find(f => f.cle === 'vue')!.retirer();
    expect(c.vueControl.value).toBe('BANQUE');
  });

  it('« Tout réinitialiser » remet les réglages par défaut et recharge le plan complet', () => {
    const fixture = creer({ flux: 'certains' });
    planReq().flush({ ...planTresorerieFixture(), certainesSeulement: true });
    fixture.detectChanges();
    spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);

    fixture.componentInstance.reinitialiser();
    expect(planReq().request.params.get('certainesSeulement')).toBe('false');
    expect(fixture.componentInstance.fluxControl.value).toBe('TOUS');
    expect(fixture.componentInstance.filtres).toEqual([]);
  });

  it('plusieurs devises : le sélecteur de devise choisit le total de la vue par source', () => {
    const fixture = creer({ vue: 'source' });
    const plan = planTresorerieFixture();
    plan.groupes = [planBanqueFixture(null, 'Banques en MAD', 0), { ...planBanqueFixture(null, 'Banques en EUR', 0), devise: 'EUR' }];
    planReq().flush(plan);
    fixture.detectChanges();
    spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);

    expect(fixture.componentInstance.devises).toEqual(['MAD', 'EUR']);
    expect(fixture.componentInstance.groupeActif!.devise).toBe('MAD');
    fixture.componentInstance.deviseControl.setValue('EUR');
    expect(fixture.componentInstance.groupeActif!.devise).toBe('EUR');
  });

  it('exporte le PDF de la devise affichée, ou celui de la banque demandée', () => {
    const fixture = creer();
    planReq().flush(planTresorerieFixture());
    const c = fixture.componentInstance;

    c.exporterPdf(null, 'MAD');
    const total = http.expectOne(r => r.url.endsWith('/pdf'));
    expect(total.request.params.has('banqueId')).toBeFalse();
    expect(total.request.params.get('devise')).toBe('MAD');

    c.exporterPdf(2, 'MAD');
    const banque = http.expectOne(r => r.url.endsWith('/pdf'));
    expect(banque.request.params.get('banqueId')).toBe('2');
    expect(banque.request.params.has('devise')).toBeFalse();
  });
});
