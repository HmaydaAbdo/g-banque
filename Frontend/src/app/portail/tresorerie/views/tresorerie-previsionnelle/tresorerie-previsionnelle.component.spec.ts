import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { HttpTestingController } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { testProviders } from '../../../../../testing/test-providers';
import { TresoreriePrevisionnelleComponent } from './tresorerie-previsionnelle.component';
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
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(TresoreriePrevisionnelleComponent);
    fixture.detectChanges();
    return fixture;
  }

  const planReq = () => http.expectOne(r => r.url.endsWith('tresorerie-previsionnelle'));

  afterEach(() => http.verify());

  it("charge le plan à l'horizon de l'adresse et affiche console, total groupe en tête, cartes par banque et grille, détail fermé", () => {
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
    expect(el.querySelector('app-detail-banque')).toBeNull();
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

  it("l'ancienne adresse d'une banque (?banque=1&col=P1) redirige vers sa page dédiée, sans charger le plan global", () => {
    const params = convertToParamMap({ banque: '1', col: 'P1', horizon: 'FIN_DE_TRIMESTRE' });
    TestBed.configureTestingModule({
      imports: [TresoreriePrevisionnelleComponent],
      providers: [...testProviders, { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: params }, queryParamMap: of(params) } }],
    });
    http = TestBed.inject(HttpTestingController);
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    TestBed.createComponent(TresoreriePrevisionnelleComponent).detectChanges();

    expect(navigate).toHaveBeenCalledWith(['banque', 1], jasmine.objectContaining({
      queryParams: { horizon: 'FIN_DE_TRIMESTRE', col: 'P1' }, replaceUrl: true,
    }));
    http.expectNone(r => r.url.endsWith('tresorerie-previsionnelle'));
  });

  it('un clic sur une banque (carte ou grille) ouvre sa page avec la période choisie', () => {
    const fixture = creer();
    planReq().flush(planTresorerieFixture());
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);

    fixture.componentInstance.ouvrir({ banqueId: 2, colonne: 'P1', devise: 'MAD' });

    expect(navigate).toHaveBeenCalledWith(['banque', 2], jasmine.objectContaining({ queryParams: { horizon: null, col: 'P1' } }));
    expect(fixture.componentInstance.planDuPanneau).toBeNull();
  });

  it("le total d'une devise se déplie sous les cartes ; un second clic le referme", () => {
    const fixture = creer();
    planReq().flush(planTresorerieFixture());
    const c = fixture.componentInstance;

    c.ouvrir({ banqueId: null, colonne: '', devise: 'MAD' });
    fixture.detectChanges();
    expect(c.planDuPanneau!.banqueNom).toBe('Toutes les banques');
    expect(fixture.nativeElement.querySelector('app-detail-banque')).not.toBeNull();
    http.expectNone(r => r.url.includes('/recapitulatif/banque/'));

    c.ouvrir({ banqueId: null, colonne: '', devise: 'MAD' });
    expect(c.planDuPanneau).toBeNull();
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

  it("l'export porte sur le total de la devise (une banque s'exporte depuis sa page)", () => {
    const fixture = creer();
    planReq().flush(planTresorerieFixture());
    const c = fixture.componentInstance;

    expect(c.perimetreExport).toEqual({ devise: 'MAD', libelle: 'Total groupe' });
    c.exporterPdf();
    const total = http.expectOne(r => r.url.endsWith('/pdf'));
    expect(total.request.params.has('banqueId')).toBeFalse();
    expect(total.request.params.get('devise')).toBe('MAD');
  });

  it("rappelle en pastille l'horizon qui s'écarte de la vue par défaut, et le retire", () => {
    const fixture = creer({ horizon: 'FIN_DE_TRIMESTRE' });
    planReq().flush(planTresorerieFixture());
    fixture.detectChanges();
    spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);

    const c = fixture.componentInstance;
    expect(c.filtres.map(f => f.cle)).toEqual(['horizon']);
    expect((fixture.nativeElement as HTMLElement).querySelector('.console__filtres')!.textContent).toContain('Fin de trimestre');
    c.filtres[0].retirer();
    planReq().flush(planTresorerieFixture());
    expect(c.filtres).toEqual([]);
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
    expect(c.perimetreExport).toEqual({ devise: 'EUR', libelle: 'Total EUR' });
  });
});
