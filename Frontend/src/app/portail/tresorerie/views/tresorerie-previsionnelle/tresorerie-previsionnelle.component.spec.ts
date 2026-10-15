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

  it("charge le plan à l'horizon de l'adresse et affiche console, total groupe en tête, cartes par banque et grille, panneau fermé", () => {
    const fixture = creer({ horizon: 'FIN_DE_TRIMESTRE' });
    const req = planReq();
    expect(req.request.params.get('periode')).toBe('FIN_DE_TRIMESTRE');
    req.flush(planTresorerieFixture());
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelectorAll('app-console-filtres').length).toBe(1);
    expect(el.querySelector('app-total-groupe .etat__titre')!.textContent).toContain('Total groupe');
    // Le total est avant les cartes, la banque en besoin est la première carte
    expect(el.querySelector('app-total-groupe')!.compareDocumentPosition(el.querySelector('app-cartes-banques')!) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(Array.from(el.querySelectorAll('.carte-banque__nom')).map(n => n.textContent!.trim())).toEqual(['Banque A', 'Banque B']);
    expect(el.querySelectorAll('app-position-grid').length).toBe(1);
    expect(el.querySelectorAll('.grille tbody .grille__ligne--n1').length).toBe(2);
    expect(el.querySelector('app-banque-panel')).toBeNull();
  });

  it("tous les filtres et l'export sont dans la console, et nulle part ailleurs", () => {
    const fixture = creer();
    planReq().flush(planTresorerieFixture());
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    const console = el.querySelector('app-console-filtres')!;
    expect(console.textContent).toContain('Exporter PDF');
    expect(console.textContent).toContain('Fin de trimestre');
    // Aucun autre bouton d'export ni sélecteur d'horizon hors de la console
    expect(Array.from(el.querySelectorAll('p-button')).filter(b => b.textContent!.includes('Exporter')).length).toBe(1);
    expect(Array.from(el.querySelectorAll('p-selectbutton')).filter(s => !console.contains(s)).length).toBe(0);
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
    expect(navigate.calls.mostRecent().args[1]!.queryParams).toEqual({ horizon: null, devise: null, banque: 2, col: 'P1' });
    fixture.detectChanges();
    http.expectOne(r => r.url.includes('/recapitulatif/banque/2')).flush(aucunePage);
    fixture.detectChanges();

    c.fermer();
    expect(navigate.calls.mostRecent().args[1]!.queryParams).toEqual({ horizon: null, devise: null, banque: null, col: null });
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

  it("changer d'horizon dans la console recharge le plan côté serveur et l'écrit dans l'adresse", () => {
    const fixture = creer();
    planReq().flush(planTresorerieFixture());
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);

    fixture.componentInstance.horizonControl.setValue('FIN_DE_TRIMESTRE');
    const req = planReq();
    expect(req.request.params.get('periode')).toBe('FIN_DE_TRIMESTRE');
    req.flush(planTresorerieFixture());
    expect(navigate.calls.mostRecent().args[1]!.queryParams!['horizon']).toBe('FIN_DE_TRIMESTRE');
  });

  it("l'export suit ce que l'on regarde : le total de la devise, puis la banque dont le panneau est ouvert", () => {
    const fixture = creer();
    planReq().flush(planTresorerieFixture());
    spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    const c = fixture.componentInstance;

    expect(c.perimetreExport).toEqual({ banqueId: null, devise: 'MAD', libelle: 'Total groupe' });
    c.exporterPdf();
    const total = http.expectOne(r => r.url.endsWith('/pdf'));
    expect(total.request.params.has('banqueId')).toBeFalse();
    expect(total.request.params.get('devise')).toBe('MAD');

    c.ouvrir({ banqueId: 2, colonne: '', devise: 'MAD' });
    fixture.detectChanges();
    http.expectOne(r => r.url.includes('/recapitulatif/banque/2')).flush(aucunePage);
    expect(c.perimetreExport).toEqual({ banqueId: 2, devise: 'MAD', libelle: 'Banque B' });
    c.exporterPdf();
    const banque = http.expectOne(r => r.url.endsWith('/pdf'));
    expect(banque.request.params.get('banqueId')).toBe('2');
    expect(banque.request.params.has('devise')).toBeFalse();
  });

  it('rappelle en pastilles les filtres qui s\'écartent de la vue par défaut, et les retire un à un', () => {
    const fixture = creer({ horizon: 'FIN_DE_TRIMESTRE', banque: '1', col: 'P1' });
    planReq().flush(planTresorerieFixture());
    fixture.detectChanges();
    http.expectOne(r => r.url.includes('/recapitulatif/banque/1')).flush(aucunePage);
    fixture.detectChanges();

    const c = fixture.componentInstance;
    expect(c.filtres.map(f => f.cle)).toEqual(['horizon', 'banque', 'periode']);
    const texte = (fixture.nativeElement as HTMLElement).querySelector('.console__filtres')!.textContent!;
    expect(texte).toContain('Fin de trimestre');
    expect(texte).toContain('Banque A');
    expect(texte).toContain('Novembre 2026');

    spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    c.filtres.find(f => f.cle === 'periode')!.retirer();
    expect(c.colonne).toBe('');
    c.filtres.find(f => f.cle === 'banque')!.retirer();
    expect(c.planDuPanneau).toBeNull();
  });

  it('« Effacer les filtres » remet tout par défaut et recharge le plan', () => {
    const fixture = creer({ horizon: 'FIN_DE_TRIMESTRE' });
    planReq().flush(planTresorerieFixture());
    fixture.detectChanges();
    spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);

    fixture.componentInstance.reinitialiser();
    expect(planReq().request.params.get('periode')).toBe('FIN_DE_MOIS');
    expect(fixture.componentInstance.filtres).toEqual([]);
  });

  it('plusieurs devises : le sélecteur de devise est dans la console et choisit le total affiché', () => {
    const fixture = creer();
    const plan = planTresorerieFixture();
    plan.groupes = [planBanqueFixture(null, 'Banques en MAD', 0), { ...planBanqueFixture(null, 'Banques en EUR', 0), devise: 'EUR' }];
    planReq().flush(plan);
    fixture.detectChanges();
    spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    const c = fixture.componentInstance;

    expect(c.devises).toEqual(['MAD', 'EUR']);
    expect((fixture.nativeElement as HTMLElement).querySelector('app-console-filtres')!.textContent).toContain('Devise');
    expect(c.groupeActif!.devise).toBe('MAD');
    c.deviseControl.setValue('EUR');
    expect(c.groupeActif!.devise).toBe('EUR');
    expect(c.perimetreExport).toEqual({ banqueId: null, devise: 'EUR', libelle: 'Total EUR' });
  });
});
