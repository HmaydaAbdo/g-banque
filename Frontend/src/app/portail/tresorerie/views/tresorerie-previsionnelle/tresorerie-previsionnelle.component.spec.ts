import { TestBed } from '@angular/core/testing';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { HttpTestingController } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { testProviders } from '../../../../../testing/test-providers';
import { TresoreriePrevisionnelleComponent } from './tresorerie-previsionnelle.component';
import { planBanqueFixture, planTresorerieFixture } from '../../testing/plan-tresorerie.fixture';

registerLocaleData(localeFr, 'fr-FR');

describe('TresoreriePrevisionnelleComponent (toutes les banques)', () => {
  let http: HttpTestingController;

  const operations = [
    { id: 7, banqueId: 1, dateEmission: '2026-11-01', dateEcheance: '2026-11-20', dateReference: '2026-11-20', libelle: 'Loyer engins', numero: null,
      sens: 'DECAISSEMENT', debit: 6000, credit: null, observations: '', source: 'LEASING' },
    { id: 8, banqueId: 2, dateEmission: '2026-10-14', dateEcheance: null, dateReference: '2026-10-14', libelle: 'Client Atlas', numero: 'CHQ 12',
      sens: 'ENCAISSEMENT', debit: null, credit: 1000, observations: '', source: 'CHEQUE' },
  ];

  function creer(queryParams: Record<string, string> = {}) {
    const params = convertToParamMap(queryParams);
    TestBed.configureTestingModule({
      imports: [TresoreriePrevisionnelleComponent],
      providers: [...testProviders, { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: params }, queryParamMap: of(params) } }],
    });
    http = TestBed.inject(HttpTestingController);
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    const fixture = TestBed.createComponent(TresoreriePrevisionnelleComponent);
    fixture.detectChanges();
    return { fixture, navigate };
  }

  const planReq = () => http.expectOne(r => r.url.endsWith('tresorerie-previsionnelle'));
  const encoursReq = () => http.expectOne(r => r.url.endsWith('/recapitulatif/toutes-banques'));

  function repondre(fixture: ReturnType<typeof creer>['fixture'], plan = planTresorerieFixture()) {
    planReq().flush(plan);
    encoursReq().flush(operations);
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  it("charge le plan et les encours de toutes les banques à l'horizon de l'adresse", () => {
    const { fixture } = creer({ horizon: 'FIN_DE_TRIMESTRE' });
    const plan = planReq();
    const encours = encoursReq();
    expect(plan.request.params.get('periode')).toBe('FIN_DE_TRIMESTRE');
    expect(encours.request.params.get('periode')).toBe('FIN_DE_TRIMESTRE');
    plan.flush(planTresorerieFixture());
    encours.flush(operations);
    fixture.detectChanges();
    expect(fixture.componentInstance.banques.length).toBe(2);
  });

  it('affiche la barre unique, la phrase du groupe, le bandeau « À surveiller » et la frise', () => {
    const { fixture } = creer();
    repondre(fixture);
    const el: HTMLElement = fixture.nativeElement;

    expect(el.querySelectorAll('app-barre-filtres').length).toBe(1);
    expect(el.querySelector('app-recit-global')).not.toBeNull();
    expect(el.querySelector('app-tableau-global')).toBeNull();
    expect(el.querySelector('.resume')!.textContent).toContain('Une facilité de caisse ne couvre que sa propre banque');
    expect(Array.from(el.querySelectorAll('.wb strong')).map(n => n.textContent!.trim())).toEqual(['Banque A', 'Banque B']);
    expect(el.querySelectorAll('.etape').length).toBe(2);
    // Un seul bouton d'export, dans la barre
    expect(Array.from(el.querySelectorAll('p-button')).filter(b => b.textContent!.includes('Exporter')).length).toBe(1);
  });

  it('un clic sur une banque du bandeau ouvre sa page ; un jalon ouvre la banque sur sa période', () => {
    const { fixture, navigate } = creer();
    repondre(fixture);
    const el: HTMLElement = fixture.nativeElement;

    el.querySelector<HTMLButtonElement>('.wb')!.click();
    expect(navigate).toHaveBeenCalledWith(['banque', 1], jasmine.objectContaining({ queryParams: { horizon: null, col: null } }));

    el.querySelector<HTMLButtonElement>('.jalon__btn')?.click();
    expect(navigate.calls.mostRecent().args[0]).toEqual(['banque', jasmine.any(Number)]);
  });

  it('bascule vers le tableau, filtre, et écrit tout dans l\'adresse', () => {
    const { fixture, navigate } = creer();
    repondre(fixture);
    const c = fixture.componentInstance;

    c.choisirVue('tableau');
    c.appliquer({ banques: [2], sens: 'ENCAISSEMENT', operateur: '>', montant: 500 });
    fixture.detectChanges();

    expect(navigate.calls.mostRecent().args[1]!.queryParams).toEqual(jasmine.objectContaining({
      vue: 'tableau', banques: '2', sens: 'ENCAISSEMENT', op: '>', montant: 500,
    }));
    expect(c.banques.map(b => b.banqueNom)).toEqual(['Banque B']);
    expect(fixture.nativeElement.querySelector('app-tableau-global')).not.toBeNull();
    expect(c.resumeFiltre).toEqual({ nombre: 1, entrees: 1000, sorties: 0 });
  });

  it("relit l'état de l'adresse : vue, banques, sources, étapes ouvertes", () => {
    const { fixture } = creer({ vue: 'tableau', banques: '1,2', sources: 'LEASING', ouvert: 'ECHU', q: 'loyer' });
    repondre(fixture);
    const c = fixture.componentInstance;

    expect(c.vue).toBe('tableau');
    expect(c.filtres.banques).toEqual([1, 2]);
    expect(c.filtres.sources).toEqual(['LEASING' as never]);
    expect(c.filtres.texte).toBe('loyer');
    expect([...c.ouvertes]).toEqual(['ECHU']);
  });

  it("l'ancienne adresse d'une banque (?banque=1&col=P1) redirige vers sa page, sans charger le plan", () => {
    const { navigate } = creer({ banque: '1', col: 'P1', horizon: 'FIN_DE_TRIMESTRE' });
    expect(navigate).toHaveBeenCalledWith(['banque', 1], jasmine.objectContaining({
      queryParams: { horizon: 'FIN_DE_TRIMESTRE', col: 'P1' }, replaceUrl: true,
    }));
    http.expectNone(r => r.url.endsWith('tresorerie-previsionnelle'));
  });

  it("changer d'horizon recharge le plan côté serveur et l'écrit dans l'adresse", () => {
    const { fixture, navigate } = creer();
    repondre(fixture);

    fixture.componentInstance.choisirHorizon('FIN_DE_TRIMESTRE');
    expect(planReq().request.params.get('periode')).toBe('FIN_DE_TRIMESTRE');
    encoursReq().flush([]);
    expect(navigate.calls.mostRecent().args[1]!.queryParams!['horizon']).toBe('FIN_DE_TRIMESTRE');
  });

  it("l'export porte sur le total de la devise (une banque s'exporte depuis sa page)", () => {
    const { fixture } = creer();
    repondre(fixture);
    fixture.componentInstance.exporterPdf();
    const req = http.expectOne(r => r.url.endsWith('/pdf'));
    expect(req.request.params.has('banqueId')).toBeFalse();
    expect(req.request.params.get('devise')).toBe('MAD');
  });

  it('plusieurs devises : un sélecteur dans la barre ; changer de devise réinitialise banques et sources', () => {
    const { fixture } = creer();
    const plan = planTresorerieFixture();
    plan.groupes = [planBanqueFixture(null, 'Banques en MAD', 0), { ...planBanqueFixture(null, 'Banques en EUR', 0), devise: 'EUR' }];
    plan.banques = [...plan.banques, { ...planBanqueFixture(3, 'Banque E', 0), devise: 'EUR' }];
    repondre(fixture, plan);
    const c = fixture.componentInstance;

    expect(c.devises).toEqual(['MAD', 'EUR']);
    c.appliquer({ banques: [1] });
    c.choisirDevise('EUR');
    expect(c.deviseActive).toBe('EUR');
    expect(c.filtres.banques).toEqual([]);
    expect(c.banques.map(b => b.banqueNom)).toEqual(['Banque E']);
  });

  it('erreur de chargement : message au lieu de la page', () => {
    const { fixture } = creer();
    planReq().flush('erreur', { status: 500, statusText: 'KO' });
    http.match(() => true);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Impossible de charger');
  });
});
